package com.nen.estoque.EstoqueNEN.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Liga o bot (long polling) somente quando `telegram.bot.token` tem o formato de um token real
 * e `telegram.bot.username` está definido (texto de exemplo como "COLE_AQUI_O_TOKEN" é ignorado).
 * Se o Telegram recusar o token, a API continua no ar e o erro vai para o log.
 */
@Slf4j
@Configuration
@ConditionalOnExpression("'${telegram.bot.token:}'.matches('" + TelegramBotProperties.FORMATO_TOKEN + "')"
        + " and !'${telegram.bot.username:}'.isBlank()")
public class TelegramBotConfig {

    @Bean
    public NickFinanceBot nickFinanceBot(TelegramBotProperties properties,
                                         TelegramVinculoService vinculoService,
                                         TelegramLancamentoService lancamentoService,
                                         MensagemParser parser,
                                         TelegramLembretes lembretes) {
        return new NickFinanceBot(properties, vinculoService, lancamentoService, parser, lembretes);
    }

    @Bean(destroyMethod = "stop")
    public BotSession telegramBotSession(NickFinanceBot bot) throws TelegramApiException {
        try {
            BotSession session = new TelegramBotsApi(DefaultBotSession.class).registerBot(bot);
            log.info("Bot do Telegram @{} conectado.", bot.getBotUsername());
            return session;
        } catch (TelegramApiException e) {
            log.error("Não foi possível conectar o bot do Telegram. Verifique telegram.bot.token no local.properties.", e);
            return new DefaultBotSession();
        }
    }
}
