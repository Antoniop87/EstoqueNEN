package com.nen.estoque.EstoqueNEN.banco;

import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MapeamentoPluggyTest {

    private static PluggyCliente.Transacao t(String valor, String status, String metodo) {
        return new PluggyCliente.Transacao("t1", "  Padaria  ", new BigDecimal(valor), LocalDate.of(2026, 11, 1),
                "DEBIT", status, metodo);
    }

    @Test
    void saidaDaContaViraDespesaNoDebitoOuPix() {
        var debito = MapeamentoPluggy.mapear("BANK", t("-12.5", "POSTED", null)).orElseThrow();
        assertEquals(TipoLancamento.DESPESA, debito.tipo());
        assertEquals(new BigDecimal("12.50"), debito.valor());
        assertEquals(FormaPagamento.DEBITO, debito.formaPagamento());
        assertEquals("Padaria", debito.descricao());

        assertEquals(FormaPagamento.PIX,
                MapeamentoPluggy.mapear("BANK", t("-5", "POSTED", "PIX")).orElseThrow().formaPagamento());
    }

    @Test
    void entradaNaContaViraReceita() {
        assertEquals(TipoLancamento.RECEITA, MapeamentoPluggy.mapear("BANK", t("100", "POSTED", null)).orElseThrow().tipo());
    }

    @Test
    void compraNoCartaoViraDespesaNoCredito() {
        var compra = MapeamentoPluggy.mapear("CREDIT", t("30", "POSTED", null)).orElseThrow();
        assertEquals(TipoLancamento.DESPESA, compra.tipo());
        assertEquals(FormaPagamento.CREDITO, compra.formaPagamento());
    }

    @Test
    void ignoraPendentesEValorZero() {
        assertTrue(MapeamentoPluggy.mapear("BANK", t("-10", "PENDING", null)).isEmpty());
        assertTrue(MapeamentoPluggy.mapear("BANK", t("0", "POSTED", null)).isEmpty());
    }

    @Test
    void lePproximoCursor() {
        assertEquals("abc", PluggyCliente.proximoCursor("?accountId=x&after=abc"));
        assertNull(PluggyCliente.proximoCursor(null));
        assertNull(PluggyCliente.proximoCursor(""));
    }
}
