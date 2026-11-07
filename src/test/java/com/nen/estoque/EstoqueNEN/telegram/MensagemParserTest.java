package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MensagemParserTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 1);

    private final MensagemParser parser = new MensagemParser();

    private LancamentoInterpretado ler(String mensagem) {
        return parser.interpretar(mensagem, HOJE);
    }

    @Test
    void despesaComDescricaoEEstabelecimento() {
        LancamentoInterpretado l = ler("gastei 45,90 com almoço no Restaurante Sabor");

        assertEquals(TipoLancamento.DESPESA, l.tipo());
        assertEquals(new BigDecimal("45.90"), l.valor());
        assertEquals("Almoço", l.descricao());
        assertEquals("Restaurante Sabor", l.detalhe());
        assertEquals(HOJE, l.data());
    }

    @Test
    void despesaSoComEstabelecimentoUsaEleComoDescricao() {
        LancamentoInterpretado l = ler("Paguei R$ 120 no mercado");

        assertEquals(TipoLancamento.DESPESA, l.tipo());
        assertEquals(new BigDecimal("120.00"), l.valor());
        assertEquals("Mercado", l.descricao());
        assertEquals("mercado", l.detalhe().toLowerCase());
    }

    @Test
    void receitaComOrigem() {
        LancamentoInterpretado l = ler("recebi 5.200 de salário da Empresa XPTO");

        assertEquals(TipoLancamento.RECEITA, l.tipo());
        assertEquals(new BigDecimal("5200.00"), l.valor());
        assertEquals("Salário", l.descricao());
        assertEquals("Empresa XPTO", l.detalhe());
    }

    @Test
    void semPalavraChaveEhDespesaEAceitaData() {
        LancamentoInterpretado l = ler("uber 32,40 ontem");

        assertEquals(TipoLancamento.DESPESA, l.tipo());
        assertEquals("Uber", l.descricao());
        assertNull(l.detalhe());
        assertEquals(HOJE.minusDays(1), l.data());
    }

    @Test
    void sinalDefineOTipo() {
        assertEquals(TipoLancamento.RECEITA, ler("+1350,50 freelance").tipo());
        assertEquals(TipoLancamento.DESPESA, ler("-15 café").tipo());
    }

    @Test
    void salarioSemPalavraChaveEhReceita() {
        assertEquals(TipoLancamento.RECEITA, ler("salario 5000").tipo());
    }

    @Test
    void conectorDoNaoSeparaDescricaoDeDespesa() {
        LancamentoInterpretado l = ler("compras do mês 300");

        assertEquals("Compras do mês", l.descricao());
        assertNull(l.detalhe());
    }

    @Test
    void dataSemAnoNoFuturoVaiParaOAnoAnterior() {
        assertEquals(LocalDate.of(2026, 10, 5), ler("mercado 120 em 05/10").data());
        assertEquals(LocalDate.of(2025, 12, 20), ler("presente 90 20/12").data());
        assertEquals(LocalDate.of(2025, 3, 2), ler("cinema 40 02/03/25").data());
    }

    @Test
    void palavraReaisEConectorNoMeioNaoEntramNaDescricao() {
        LancamentoInterpretado l = ler("gastei 20 reais hoje com lazer");

        assertEquals(TipoLancamento.DESPESA, l.tipo());
        assertEquals(new BigDecimal("20.00"), l.valor());
        assertEquals("Lazer", l.descricao());
        assertNull(l.detalhe());
        assertEquals(HOJE, l.data());
    }

    /** Frases do dia a dia: mensagem, descrição esperada, detalhe esperado (vazio = nenhum). */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "gastei 50 reais em gasolina no Posto Shell | Gasolina      | Posto Shell",
            "paguei 30 numa pizza                       | Pizza         | ",
            "paguei a conta de luz 150.                 | Conta de luz  | ",
            "gastei 15 contos com café na padaria       | Café          | Padaria",
            "comprei um tênis por 200 reais na Centauro | Tênis         | Centauro",
            "recebi 300 reais do João                   | João          | João",
            "recebi 2 mil de bônus                      | Bônus         | ",
            "almoço com amigos 80 no Outback            | Almoço com amigos | Outback",
            "ganhei 100 reais de presente da vó         | Presente      | Vó"
    })
    void frasesDoDiaADia(String mensagem, String descricao, String detalhe) {
        LancamentoInterpretado l = ler(mensagem.trim());

        assertEquals(descricao.trim(), l.descricao(), "descrição de: " + mensagem);
        assertEquals(detalhe == null ? null : detalhe.trim(), l.detalhe(), "detalhe de: " + mensagem);
    }

    @Test
    void entendeMilEPontuacaoNoValor() {
        assertEquals(new BigDecimal("2000.00"), ler("recebi 2 mil de bônus").valor());
        assertEquals(new BigDecimal("1500.00"), ler("recebi 1,5 mil de freela").valor());
        assertEquals(new BigDecimal("150.00"), ler("paguei a conta de luz 150.").valor());
    }

    @Test
    void dataComNoDiaNaoSobraNaDescricao() {
        LancamentoInterpretado l = ler("gastei 120 no mercado no dia 05/10");

        assertEquals("Mercado", l.descricao());
        assertEquals(LocalDate.of(2026, 10, 5), l.data());
    }

    @ParameterizedTest
    @CsvSource({
            "'1.234,56', 1234.56",
            "'1234,5', 1234.50",
            "'1234.56', 1234.56",
            "'1.350', 1350.00",
            "'45', 45.00"
    })
    void formatosDeValor(String texto, String esperado) {
        assertEquals(new BigDecimal(esperado), MensagemParser.paraBigDecimal(texto));
    }

    @Test
    void semValorDaErroAmigavel() {
        MensagemNaoEntendidaException erro =
                assertThrows(MensagemNaoEntendidaException.class, () -> ler("gastei no mercado"));
        assertTrue(erro.getMessage().contains("valor"));
    }

    @Test
    void dataInexistenteDaErro() {
        assertThrows(MensagemNaoEntendidaException.class, () -> ler("mercado 10 31/02"));
    }

    @Test
    void valorZeroDaErro() {
        assertThrows(MensagemNaoEntendidaException.class, () -> ler("gastei 0 no mercado"));
    }

    // ---------- Forma de pagamento ----------

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "gastei 50 no mercado no crédito          | CREDITO  | Mercado   | mercado",
            "gastei 120 no cartão de crédito com tênis | CREDITO  | Tênis     |",
            "uber 32,40 ontem no cartão               | CREDITO  | Uber      |",
            "almoço 45 no débito                       | DEBITO   | Almoço    |",
            "paguei 80 de luz no pix                   | PIX      | Luz       |",
            "gastei 20 em dinheiro com pipoca          | DINHEIRO | Pipoca    |",
            "farmácia 35,90 cartão débito              | DEBITO   | Farmácia  |",
            "gastei 20 reais hoje com lazer            |          | Lazer     |",
    })
    void reconheceFormaDePagamentoSemSujarADescricao(String mensagem, FormaPagamento forma, String descricao, String detalhe) {
        LancamentoInterpretado l = ler(mensagem);

        assertEquals(TipoLancamento.DESPESA, l.tipo());
        assertEquals(forma, l.formaPagamento());
        assertEquals(descricao, l.descricao());
        assertEquals(detalhe, l.detalhe() == null ? null : l.detalhe().toLowerCase());
    }

    @Test
    void creditoNoComecoContinuaSendoEntradaSemFormaDePagamento() {
        LancamentoInterpretado l = ler("crédito 50 reembolso");

        assertEquals(TipoLancamento.RECEITA, l.tipo());
        assertNull(l.formaPagamento());
        assertEquals("Reembolso", l.descricao());
    }

    @Test
    void entradaNaoGanhaFormaDePagamento() {
        LancamentoInterpretado l = ler("recebi 200 no pix do João");

        assertEquals(TipoLancamento.RECEITA, l.tipo());
        assertNull(l.formaPagamento());
    }
}
