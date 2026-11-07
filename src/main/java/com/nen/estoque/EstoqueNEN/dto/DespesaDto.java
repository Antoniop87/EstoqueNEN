package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Despesa;
import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.util.Datas;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Despesas: corpo de criação/edição e resposta. O usuário vem do token. */
public final class DespesaDto {

    private DespesaDto() {
    }

    public record Request(

            @NotBlank @Size(max = 100)
            String descricao,

            @Size(max = 100)
            String estabelecimento,

            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
            BigDecimal valor,

            @NotNull
            LocalDate data,

            Long categoriaId,

            /** Opcional: clientes antigos (ex.: app Android) ainda não enviam. */
            FormaPagamento formaPagamento

    ) {
    }

    public record Response(Long id, String descricao, String estabelecimento, BigDecimal valor,
                           LocalDate data, Long categoriaId, FormaPagamento formaPagamento) {

        public static Response from(Despesa despesa) {
            return new Response(
                    despesa.getId(),
                    despesa.getNomeDespesa(),
                    despesa.getEstabelecimento(),
                    despesa.getValorDespesa(),
                    Datas.paraLocalDate(despesa.getDataDespesa()),
                    despesa.getCategoria() == null ? null : despesa.getCategoria().getId(),
                    despesa.getFormaPagamento()
            );
        }
    }
}
