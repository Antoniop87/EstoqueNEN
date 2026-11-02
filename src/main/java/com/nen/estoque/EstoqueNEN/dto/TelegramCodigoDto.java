package com.nen.estoque.EstoqueNEN.dto;

import java.time.Instant;

/**
 * Código temporário para ligar a conta ao Telegram.
 *
 * @param link link `t.me` que abre o bot já enviando o código, ou null se o bot não estiver configurado
 */
public record TelegramCodigoDto(

        String codigo,
        Instant expiraEm,
        String link

) {
}
