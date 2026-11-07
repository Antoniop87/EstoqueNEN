package com.nen.estoque.EstoqueNEN.model;

/** Como uma despesa foi paga. Nulo nas despesas antigas ou quando não foi informado. */
public enum FormaPagamento {

    PIX("Pix"),
    DEBITO("Débito"),
    DINHEIRO("Dinheiro"),
    CREDITO("Cartão de crédito");

    private final String rotulo;

    FormaPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
