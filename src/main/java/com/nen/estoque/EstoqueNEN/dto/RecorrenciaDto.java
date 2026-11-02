package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Recorrencia;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lançamentos recorrentes mensais. */
public final class RecorrenciaDto {

    private RecorrenciaDto() {
    }

    public record Request(

            @NotNull
            TipoLancamento tipo,

            @NotBlank @Size(max = 100)
            String descricao,

            @Size(max = 100)
            String detalhe,

            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
            BigDecimal valor,

            Long categoriaId,

            @Min(1) @Max(31)
            int diaDoMes,

            @NotNull
            Boolean ativa

    ) {
    }

    /**
     * @param proximaData quando o próximo lançamento será criado (null se pausada)
     */
    public record Response(Long id, TipoLancamento tipo, String descricao, String detalhe, BigDecimal valor,
                           Long categoriaId, int diaDoMes, boolean ativa, LocalDate proximaData) {

        public static Response from(Recorrencia r, LocalDate proximaData) {
            return new Response(r.getId(), r.getTipo(), r.getDescricao(), r.getDetalhe(), r.getValor(),
                    r.getCategoria() == null ? null : r.getCategoria().getId(),
                    r.getDiaDoMes(), r.isAtiva(), proximaData);
        }
    }
}
