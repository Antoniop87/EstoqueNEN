package com.nen.estoque.EstoqueNEN.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Conversa com o Nino. O histórico fica no navegador e é reenviado a cada mensagem. */
public final class NinoDto {

    private NinoDto() {
    }

    /** @param papel só user ou assistant: o cliente não consegue enviar instruções de sistema */
    public record Mensagem(

            @NotBlank @Pattern(regexp = "user|assistant")
            String papel,

            @NotBlank @Size(max = 4000)
            String texto

    ) {
    }

    public record Request(

            @NotEmpty @Size(max = 20) @Valid
            List<Mensagem> mensagens

    ) {
    }

    public record Response(String resposta, int restantesHoje) {
    }

    public record Status(boolean ativo, int restantesHoje, int limiteDiario) {
    }
}
