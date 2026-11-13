package com.nen.estoque.EstoqueNEN.banco;

import com.nen.estoque.EstoqueNEN.banco.PluggyCliente.PluggyException;
import com.nen.estoque.EstoqueNEN.model.*;
import com.nen.estoque.EstoqueNEN.repository.BancoConexaoRepository;
import com.nen.estoque.EstoqueNEN.repository.DespesaRepository;
import com.nen.estoque.EstoqueNEN.repository.ReceitaRepository;
import com.nen.estoque.EstoqueNEN.repository.TransacaoBancariaRepository;
import com.nen.estoque.EstoqueNEN.service.CategoriaService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Busca as transações de um banco conectado e as coloca na fila de revisão.
 * As chamadas à Pluggy ficam fora de transação, para não segurar conexão do banco.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BancoSincronizador {

    /** Na primeira vez, traz os últimos 90 dias. */
    static final int DIAS_PRIMEIRA_SINCRONIZACAO = 90;
    /** Depois, revisita alguns dias antes da última vez, para pegar o que o banco lançou com atraso. */
    static final int DIAS_DE_FOLGA = 10;

    private final PluggyCliente pluggy;
    private final BancoConexaoRepository conexoes;
    private final TransacaoBancariaRepository transacoes;
    private final DespesaRepository despesas;
    private final ReceitaRepository receitas;
    private final CategoriaService categorias;
    private final TransactionTemplate transacao;

    private record Recebida(String conta, PluggyCliente.Transacao transacao, MapeamentoPluggy.Lancamento lancamento) {
    }

    /** Chamado pelo webhook: responde rápido e sincroniza em segundo plano. */
    @Async
    public void sincronizarItemEmSegundoPlano(String itemId) {
        conexoes.findByItemId(itemId).ifPresent(conexao -> {
            try {
                sincronizar(conexao.getId());
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar a conexão {}: {}", conexao.getId(), e.getMessage());
            }
        });
    }

    /** @return quantas transações novas entraram na fila de revisão */
    public int sincronizar(Long conexaoId) {
        BancoConexao conexao = conexoes.findById(conexaoId).orElseThrow();
        try {
            PluggyCliente.Item item = pluggy.buscarItem(conexao.getItemId());
            LocalDate desde = conexao.getUltimaSincronizacao() == null
                    ? Datas.hoje().minusDays(DIAS_PRIMEIRA_SINCRONIZACAO)
                    : LocalDate.ofInstant(conexao.getUltimaSincronizacao(), Datas.FUSO).minusDays(DIAS_DE_FOLGA);

            List<Recebida> recebidas = new ArrayList<>();
            for (PluggyCliente.Conta conta : pluggy.contas(conexao.getItemId())) {
                for (PluggyCliente.Transacao t : pluggy.transacoes(conta.id(), desde)) {
                    MapeamentoPluggy.mapear(conta.tipo(), t)
                            .ifPresent(l -> recebidas.add(new Recebida(conta.nome(), t, l)));
                }
            }
            return Objects.requireNonNull(transacao.execute(s -> salvar(conexaoId, item, recebidas)));
        } catch (PluggyException e) {
            transacao.executeWithoutResult(s -> conexoes.findById(conexaoId).ifPresent(c -> {
                c.setErro("Não foi possível atualizar agora (" + e.getMessage() + ").");
                conexoes.save(c);
            }));
            throw e;
        }
    }

    private int salvar(Long conexaoId, PluggyCliente.Item item, List<Recebida> recebidas) {
        BancoConexao conexao = conexoes.findById(conexaoId).orElseThrow();
        User usuario = conexao.getUsuario();

        Set<String> existentes = recebidas.isEmpty() ? Set.of()
                : transacoes.idsExistentes(recebidas.stream().map(r -> r.transacao().id()).toList());
        List<Recebida> novas = recebidas.stream()
                .filter(r -> !existentes.contains(r.transacao().id()))
                .toList();

        Map<TipoLancamento, List<Lancado>> jaLancados = jaLancados(usuario, conexao, novas);
        for (Recebida r : novas) {
            MapeamentoPluggy.Lancamento l = r.lancamento();
            transacoes.save(TransacaoBancaria.builder()
                    .usuario(usuario)
                    .conexao(conexao)
                    .pluggyId(r.transacao().id())
                    .conta(r.conta())
                    .descricao(l.descricao())
                    .valor(l.valor())
                    .data(r.transacao().data())
                    .tipo(l.tipo())
                    .formaPagamento(l.formaPagamento())
                    .categoriaSugerida(categorias.sugerir(usuario, l.tipo(), l.descricao()).orElse(null))
                    .situacao(TransacaoBancaria.Situacao.PENDENTE)
                    .possivelDuplicado(jaLancados.get(l.tipo()).stream()
                            .anyMatch(j -> j.valor().compareTo(l.valor()) == 0
                                    && Math.abs(ChronoUnit.DAYS.between(j.data(), r.transacao().data())) <= 1))
                    .build());
        }

        conexao.setNomeBanco(item.nomeBanco());
        conexao.setStatus(item.status());
        conexao.setErro(item.erro());
        conexao.setUltimaSincronizacao(Instant.now());
        conexoes.save(conexao);
        return novas.size();
    }

    private record Lancado(BigDecimal valor, LocalDate data) {
    }

    /**
     * O que já existe para comparar e avisar de possível duplicado: lançamentos registrados
     * (à mão, Telegram, app) e transações recebidas por outra conexão do mesmo usuário.
     */
    private Map<TipoLancamento, List<Lancado>> jaLancados(User usuario, BancoConexao conexao, List<Recebida> novas) {
        Map<TipoLancamento, List<Lancado>> mapa = new EnumMap<>(TipoLancamento.class);
        mapa.put(TipoLancamento.DESPESA, new ArrayList<>(despesas.findAllByUsuarioOrderByDataDespesaDescIdDesc(usuario).stream()
                .map(d -> new Lancado(d.getValorDespesa(), d.getDataDespesa().toInstant().atZone(ZoneOffset.UTC).toLocalDate()))
                .toList()));
        mapa.put(TipoLancamento.RECEITA, new ArrayList<>(receitas.findAllByUsuarioOrderByDataDescIdDesc(usuario).stream()
                .map(r -> new Lancado(r.getValor(), r.getData()))
                .toList()));

        if (!novas.isEmpty() && conexao.getId() != null) {
            LocalDate de = novas.stream().map(r -> r.transacao().data()).min(LocalDate::compareTo).orElseThrow().minusDays(1);
            LocalDate ate = novas.stream().map(r -> r.transacao().data()).max(LocalDate::compareTo).orElseThrow().plusDays(1);
            // Importadas já viraram lançamento e foram contadas acima.
            transacoes.deOutrasConexoes(usuario, conexao, de, ate).stream()
                    .filter(t -> t.getSituacao() == TransacaoBancaria.Situacao.PENDENTE)
                    .forEach(t -> mapa.get(t.getTipo()).add(new Lancado(t.getValor(), t.getData())));
        }
        return mapa;
    }
}
