package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Receita;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Entradas: corpo de criação/edição e resposta. O usuário vem do token. */
public final class ReceitaDto {

    private ReceitaDto() {
    }

    public record Request(

            @NotBlank @Size(max = 100)
            String descricao,

            @Size(max = 100)
            String origem,

            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
            BigDecimal valor,

            @NotNull
            LocalDate data,

            Long categoriaId

    ) {
    }

    public record Response(Long id, String descricao, String origem, BigDecimal valor,
                           LocalDate data, Long categoriaId) {

        public static Response from(Receita receita) {
            return new Response(
                    receita.getId(),
                    receita.getDescricao(),
                    receita.getOrigem(),
                    receita.getValor(),
                    receita.getData(),
                    receita.getCategoria() == null ? null : receita.getCategoria().getId()
            );
        }
    }
}
