package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Categorias: corpo de criação/edição e resposta. */
public final class CategoriaDto {

    private CategoriaDto() {
    }

    public record Request(

            @NotBlank @Size(max = 60)
            String nome,

            @NotBlank @Size(max = 40)
            String icone,

            @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}")
            String cor,

            @NotNull
            TipoLancamento tipo

    ) {
    }

    public record Response(Long id, String nome, String icone, String cor, TipoLancamento tipo) {

        public static Response from(Categoria categoria) {
            return new Response(categoria.getId(), categoria.getNome(), categoria.getIcone(),
                    categoria.getCor(), categoria.getTipo());
        }
    }
}
