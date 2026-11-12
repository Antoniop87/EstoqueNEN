package com.nen.estoque.EstoqueNEN.banco;

import com.nen.estoque.EstoqueNEN.banco.PluggyCliente.PluggyException;
import com.nen.estoque.EstoqueNEN.dto.BancoDto;
import com.nen.estoque.EstoqueNEN.model.BancoConexao;
import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.TransacaoBancaria;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.BancoConexaoRepository;
import com.nen.estoque.EstoqueNEN.repository.TransacaoBancariaRepository;
import com.nen.estoque.EstoqueNEN.service.CategoriaService;
import com.nen.estoque.EstoqueNEN.service.LancamentoService;
import com.nen.estoque.EstoqueNEN.service.UsuarioLogadoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** Bancos conectados e a fila de transações para revisar, sempre do usuário logado. */
@Slf4j
@Service
public class BancoService {

    private final PluggyCliente pluggy;
    private final BancoSincronizador sincronizador;
    private final BancoConexaoRepository conexoes;
    private final TransacaoBancariaRepository transacoes;
    private final CategoriaService categorias;
    private final LancamentoService lancamentos;
    private final UsuarioLogadoService usuarioLogado;
    private final TransactionTemplate transacao;
    private final boolean incluirSandbox;

    public BancoService(PluggyCliente pluggy, BancoSincronizador sincronizador, BancoConexaoRepository conexoes,
                        TransacaoBancariaRepository transacoes, CategoriaService categorias,
                        LancamentoService lancamentos, UsuarioLogadoService usuarioLogado,
                        TransactionTemplate transacao,
                        @Value("${pluggy.incluir-sandbox:true}") boolean incluirSandbox) {
        this.pluggy = pluggy;
        this.sincronizador = sincronizador;
        this.conexoes = conexoes;
        this.transacoes = transacoes;
        this.categorias = categorias;
        this.lancamentos = lancamentos;
        this.usuarioLogado = usuarioLogado;
        this.transacao = transacao;
        this.incluirSandbox = incluirSandbox;
    }

    @Transactional(readOnly = true)
    public BancoDto.Status status() {
        User usuario = usuarioLogado.getUsuario();
        return new BancoDto.Status(
                pluggy.configurado(),
                incluirSandbox,
                conexoes.findAllByUsuarioOrderByCriadaEmAsc(usuario).stream().map(BancoDto.Conexao::from).toList(),
                transacoes.countByUsuarioAndSituacao(usuario, TransacaoBancaria.Situacao.PENDENTE));
    }

    public BancoDto.ConnectToken connectToken() {
        exigirConfigurado();
        User usuario = usuarioLogado.getUsuario();
        return new BancoDto.ConnectToken(chamar(() -> pluggy.criarConnectToken(String.valueOf(usuario.getId()))));
    }

    /**
     * Registra o banco que o usuário acabou de conectar no widget e traz as transações.
     * O item só é aceito se a Pluggy confirmar que foi criado para este usuário.
     */
    public BancoDto.Conexao conectar(String itemId) {
        exigirConfigurado();
        User usuario = usuarioLogado.getUsuario();
        PluggyCliente.Item item = chamar(() -> pluggy.buscarItem(itemId));
        if (!String.valueOf(usuario.getId()).equals(item.clientUserId())) {
            throw naoEncontrada();
        }

        Long id = transacao.execute(s -> {
            BancoConexao conexao = conexoes.findByItemId(itemId).orElseGet(() -> BancoConexao.builder()
                    .usuario(usuario).itemId(itemId).criadaEm(Instant.now()).build());
            if (!conexao.getUsuario().getId().equals(usuario.getId())) {
                throw naoEncontrada();
            }
            conexao.setNomeBanco(item.nomeBanco());
            conexao.setStatus(item.status());
            return conexoes.save(conexao).getId();
        });

        try {
            sincronizador.sincronizar(id);
        } catch (PluggyException e) {
            // A conexão fica salva com o erro; o usuário pode tentar de novo pela tela.
            log.warn("Primeira sincronização falhou: {}", e.getMessage());
        }
        return BancoDto.Conexao.from(conexoes.findById(id).orElseThrow());
    }

    public BancoDto.Resultado sincronizar(Long conexaoId) {
        exigirConfigurado();
        BancoConexao conexao = buscarConexao(conexaoId);
        return new BancoDto.Resultado(chamar(() -> sincronizador.sincronizar(conexao.getId())));
    }

    /** Desconecta o banco. Lançamentos já importados continuam no extrato. */
    public void remover(Long conexaoId) {
        BancoConexao conexao = buscarConexao(conexaoId);
        try {
            pluggy.excluirItem(conexao.getItemId());
        } catch (PluggyException e) {
            log.warn("Não foi possível excluir o item na Pluggy: {}", e.getMessage());
        }
        transacao.executeWithoutResult(s -> {
            transacoes.excluirDaConexao(conexao);
            conexoes.deleteById(conexao.getId());
        });
    }

    @Transactional(readOnly = true)
    public List<BancoDto.Transacao> pendentes() {
        return transacoes.findAllByUsuarioAndSituacaoOrderByDataDescIdDesc(
                        usuarioLogado.getUsuario(), TransacaoBancaria.Situacao.PENDENTE)
                .stream().map(BancoDto.Transacao::from).toList();
    }

    @Transactional
    public void importar(Long id, Long categoriaId) {
        User usuario = usuarioLogado.getUsuario();
        TransacaoBancaria t = buscarPendente(id, usuario);
        importar(t, usuario, categorias.buscarParaLancamento(categoriaId, usuario, t.getTipo()));
    }

    @Transactional
    public void ignorar(Long id) {
        TransacaoBancaria t = buscarPendente(id, usuarioLogado.getUsuario());
        t.setSituacao(TransacaoBancaria.Situacao.IGNORADA);
    }

    /** Importa todas as pendentes com a categoria sugerida. */
    @Transactional
    public BancoDto.Resultado importarTodas() {
        User usuario = usuarioLogado.getUsuario();
        List<TransacaoBancaria> pendentes = transacoes.findAllByUsuarioAndSituacaoOrderByDataDescIdDesc(
                usuario, TransacaoBancaria.Situacao.PENDENTE);
        pendentes.forEach(t -> importar(t, usuario, t.getCategoriaSugerida()));
        return new BancoDto.Resultado(pendentes.size());
    }

    private void importar(TransacaoBancaria t, User usuario, Categoria categoria) {
        Long lancamentoId = lancamentos.criar(usuario, new LancamentoService.Novo(
                t.getTipo(), t.getDescricao(), null, t.getValor(), t.getData(), categoria, t.getFormaPagamento()));
        t.setSituacao(TransacaoBancaria.Situacao.IMPORTADA);
        t.setLancamentoId(lancamentoId);
    }

    private TransacaoBancaria buscarPendente(Long id, User usuario) {
        TransacaoBancaria t = transacoes.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada."));
        if (t.getSituacao() != TransacaoBancaria.Situacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa transação já foi revisada.");
        }
        return t;
    }

    private BancoConexao buscarConexao(Long id) {
        return conexoes.findByIdAndUsuario(id, usuarioLogado.getUsuario()).orElseThrow(this::naoEncontrada);
    }

    private ResponseStatusException naoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Conexão não encontrada.");
    }

    private void exigirConfigurado() {
        if (!pluggy.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A integração bancária não está configurada.");
        }
    }

    private static <T> T chamar(java.util.function.Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (PluggyException e) {
            throw new ResponseStatusException(e.status() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY,
                    "Não foi possível falar com o banco agora.");
        }
    }
}
