package com.nen.estoque.EstoqueNEN.banco;

import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Converte uma transação da Pluggy em entrada ou despesa.
 *
 * Conta corrente (BANK): valor negativo é saída e positivo é entrada; Pix é reconhecido
 * pelo método de pagamento, o resto vira débito.
 * Cartão (CREDIT): valor positivo é compra (despesa no crédito) e negativo é estorno ou
 * pagamento da fatura (vira entrada, para o usuário decidir na revisão).
 */
public final class MapeamentoPluggy {

    private static final int TAMANHO_DESCRICAO = 100;

    private MapeamentoPluggy() {
    }

    public record Lancamento(TipoLancamento tipo, BigDecimal valor, FormaPagamento formaPagamento, String descricao) {
    }

    /** Vazio para o que não deve virar lançamento (valor zero ou transação ainda pendente no banco). */
    public static Optional<Lancamento> mapear(String tipoConta, PluggyCliente.Transacao t) {
        if (t.valor().signum() == 0 || "PENDING".equalsIgnoreCase(t.status())) {
            return Optional.empty();
        }
        boolean cartao = "CREDIT".equalsIgnoreCase(tipoConta);
        boolean saida = cartao ? t.valor().signum() > 0 : t.valor().signum() < 0;

        FormaPagamento forma = null;
        if (saida) {
            forma = cartao ? FormaPagamento.CREDITO
                    : "PIX".equalsIgnoreCase(t.metodoPagamento()) ? FormaPagamento.PIX : FormaPagamento.DEBITO;
        }
        return Optional.of(new Lancamento(
                saida ? TipoLancamento.DESPESA : TipoLancamento.RECEITA,
                t.valor().abs().setScale(2, RoundingMode.HALF_UP),
                forma,
                descricao(t.descricao())));
    }

    private static String descricao(String texto) {
        String limpo = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        if (limpo.isEmpty()) {
            return "Transação bancária";
        }
        return limpo.length() <= TAMANHO_DESCRICAO ? limpo : limpo.substring(0, TAMANHO_DESCRICAO);
    }
}
