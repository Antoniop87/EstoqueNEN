package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Orcamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Orçamento mensal por categoria. O gasto do mês é calculado pelo front a partir das despesas. */
public final class OrcamentoDto {

    private OrcamentoDto() {
    }

    public record Request(

            @NotNull
            Long categoriaId,

            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
            BigDecimal valorLimite

    ) {
    }

    public record Response(Long id, Long categoriaId, BigDecimal valorLimite) {

        public static Response from(Orcamento orcamento) {
            return new Response(orcamento.getId(), orcamento.getCategoria().getId(), orcamento.getValorLimite());
        }
    }
}
