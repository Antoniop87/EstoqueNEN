package com.nen.estoque.EstoqueNEN.dto;

import java.time.Instant;

/**
 * Situação da integração com o Telegram para o usuário logado.
 *
 * @param botAtivo      o bot está configurado e rodando no servidor
 * @param botUsername   usuário do bot (sem @), para montar o link
 * @param vinculado     a conta já está ligada a um chat
 * @param vinculadoEm   quando a ligação foi feita
 */
public record TelegramStatusDto(

        boolean botAtivo,
        String botUsername,
        boolean vinculado,
        Instant vinculadoEm

) {
}
