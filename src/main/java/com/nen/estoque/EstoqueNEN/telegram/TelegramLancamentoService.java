package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.DespesaRepository;
import com.nen.estoque.EstoqueNEN.repository.ReceitaRepository;
import com.nen.estoque.EstoqueNEN.service.CategoriaService;
import com.nen.estoque.EstoqueNEN.service.LancamentoService;
import com.nen.estoque.EstoqueNEN.service.OrcamentoService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Grava, desfaz e resume lançamentos feitos pelo bot, sempre em nome de um usuário já vinculado.
 */
@Service
@RequiredArgsConstructor
public class TelegramLancamentoService {

    /** A partir de quanto do orçamento o bot avisa. */
    static final int ALERTA_ORCAMENTO_PERCENTUAL = 80;

    private final LancamentoService lancamentoService;
    private final CategoriaService categoriaService;
    private final OrcamentoService orcamentoService;
    private final DespesaRepository despesaRepository;
    private final ReceitaRepository receitaRepository;

    /**
     * @param categoria nome da categoria atribuída automaticamente (pode ser null)
     * @param orcamento situação do orçamento da categoria, quando passou do limite de alerta (pode ser null)
     */
    public record Registro(TipoLancamento tipo, Long id, String categoria, OrcamentoService.Situacao orcamento) {
    }

    public record Resumo(BigDecimal entradas, BigDecimal saidas) {
        public BigDecimal saldo() {
            return entradas.subtract(saidas);
        }
    }

    @Transactional
    public Registro registrar(User usuario, LancamentoInterpretado lancamento) {
        String texto = lancamento.descricao() + " " + (lancamento.detalhe() == null ? "" : lancamento.detalhe());
        Categoria categoria = categoriaService.sugerir(usuario, lancamento.tipo(), texto).orElse(null);

        Long id = lancamentoService.criar(usuario, new LancamentoService.Novo(
                lancamento.tipo(), lancamento.descricao(), lancamento.detalhe(), lancamento.valor(),
                lancamento.data(), categoria, lancamento.formaPagamento()));

        OrcamentoService.Situacao alerta = null;
        if (lancamento.tipo() == TipoLancamento.DESPESA && categoria != null) {
            alerta = orcamentoService.situacao(categoria, YearMonth.from(lancamento.data()))
                    .filter(s -> s.percentual() >= ALERTA_ORCAMENTO_PERCENTUAL)
                    .orElse(null);
        }
        return new Registro(lancamento.tipo(), id, categoria == null ? null : categoria.getNome(), alerta);
    }

    /** Remove um lançamento, somente se ele pertencer ao usuário. */
    @Transactional
    public boolean desfazer(User usuario, TipoLancamento tipo, Long id) {
        return lancamentoService.excluir(usuario, tipo, id);
    }

    @Transactional(readOnly = true)
    public Resumo resumoDoMes(User usuario, YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.plusMonths(1).atDay(1);
        return new Resumo(
                receitaRepository.somarNoPeriodo(usuario, inicio, fim),
                despesaRepository.somarNoPeriodo(usuario, Datas.paraDate(inicio), Datas.paraDate(fim))
        );
    }
}
