package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Compromisso;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contas a pagar e a receber. */
public final class CompromissoDto {

    private CompromissoDto() {
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

            @NotNull
            LocalDate vencimento,

            Long categoriaId

    ) {
    }

    /** Corpo opcional de "marcar como paga": data do pagamento (padrão: hoje). */
    public record Pagamento(LocalDate data) {
    }

    public record Response(Long id, TipoLancamento tipo, String descricao, String detalhe, BigDecimal valor,
                           LocalDate vencimento, Long categoriaId, LocalDate pagoEm, Long lancamentoId) {

        public static Response from(Compromisso c) {
            return new Response(c.getId(), c.getTipo(), c.getDescricao(), c.getDetalhe(), c.getValor(),
                    c.getVencimento(), c.getCategoria() == null ? null : c.getCategoria().getId(),
                    c.getPagoEm(), c.getLancamentoId());
        }
    }
}
