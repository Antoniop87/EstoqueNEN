package com.nen.estoque.EstoqueNEN.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração do bot (`telegram.bot.*` no application.properties).
 * Configurado no local.properties (fora do git) ou por variáveis de ambiente.
 * Sem token válido, o bot fica desligado e o restante da API funciona normalmente.
 */
@Component
public class TelegramBotProperties {

    /** Formato dos tokens do BotFather: `123456789:AAH...`. */
    public static final String FORMATO_TOKEN = "\\d+:[\\w-]{20,}";

    private final String token;
    private final String username;

    public TelegramBotProperties(@Value("${telegram.bot.token:}") String token,
                                 @Value("${telegram.bot.username:}") String username) {
        this.token = token == null ? "" : token.trim();
        this.username = username == null ? "" : username.trim().replaceFirst("^@", "");
    }

    public boolean isConfigurado() {
        return token.matches(FORMATO_TOKEN) && !username.isEmpty();
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    /** Link que abre o bot já enviando `/start <codigo>`. */
    public String linkComCodigo(String codigo) {
        return isConfigurado() ? "https://t.me/" + username + "?start=" + codigo : null;
    }
}
