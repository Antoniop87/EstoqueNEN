package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Meta;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Metas de economia. */
public final class MetaDto {

    private MetaDto() {
    }

    public record Request(

            @NotBlank @Size(max = 80)
            String nome,

            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
            BigDecimal valorAlvo,

            LocalDate prazo,

            @NotBlank @Size(max = 40)
            String icone,

            @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}")
            String cor

    ) {
    }

    /** Guardar (valor positivo) ou retirar (negativo) dinheiro da meta. */
    public record Movimento(

            @NotNull @Digits(integer = 13, fraction = 2)
            BigDecimal valor

    ) {
    }

    public record Response(Long id, String nome, BigDecimal valorAlvo, BigDecimal valorGuardado,
                           LocalDate prazo, String icone, String cor, LocalDate criadaEm) {

        public static Response from(Meta m) {
            return new Response(m.getId(), m.getNome(), m.getValorAlvo(), m.getValorGuardado(),
                    m.getPrazo(), m.getIcone(), m.getCor(), m.getCriadaEm());
        }
    }
}
