package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.service.OrcamentoService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Bot do Nick Finance. Recebe mensagens como "gastei 45,90 no mercado" e grava o lançamento
 * na conta vinculada ao chat. Registrado por {@link TelegramBotConfig} só quando há token.
 */
@Slf4j
public class NickFinanceBot extends TelegramLongPollingBot {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final ZoneId FUSO = Datas.FUSO;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DESFAZER = "desfazer";

    private final TelegramBotProperties properties;
    private final TelegramVinculoService vinculoService;
    private final TelegramLancamentoService lancamentoService;
    private final MensagemParser parser;
    private final TelegramLembretes lembretes;

    public NickFinanceBot(TelegramBotProperties properties,
                          TelegramVinculoService vinculoService,
                          TelegramLancamentoService lancamentoService,
                          MensagemParser parser,
                          TelegramLembretes lembretes) {
        super(properties.getToken());
        this.properties = properties;
        this.vinculoService = vinculoService;
        this.lancamentoService = lancamentoService;
        this.parser = parser;
        this.lembretes = lembretes;
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                tratarBotao(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                tratarMensagem(update.getMessage());
            }
        } catch (Exception e) {
            log.error("Erro ao tratar atualização do Telegram", e);
            Long chatId = update.hasMessage() ? update.getMessage().getChatId() : null;
            if (chatId != null) {
                enviar(chatId, "😕 Algo deu errado do nosso lado. Tente novamente em instantes.", null);
            }
        }
    }

    // ---------- Mensagens ----------

    private void tratarMensagem(Message message) {
        Long chatId = message.getChatId();
        String texto = message.getText().trim();
        String comando = texto.split("\\s+")[0].toLowerCase(Locale.ROOT).replaceFirst("@.*$", "");
        String argumento = texto.contains(" ") ? texto.substring(texto.indexOf(' ') + 1).trim() : "";

        switch (comando) {
            case "/start", "/vincular" -> {
                if (!argumento.isEmpty()) {
                    vincular(chatId, argumento);
                } else {
                    boasVindas(chatId);
                }
            }
            case "/ajuda", "/help" -> enviar(chatId, ajuda(), null);
            case "/saldo" -> comUsuario(chatId, usuario -> saldo(chatId, usuario));
            case "/contas" -> comUsuario(chatId, usuario -> enviar(chatId, lembretes.mensagemContas(usuario, true), null));
            case "/desvincular" -> desvincular(chatId);
            default -> {
                if (comando.startsWith("/")) {
                    enviar(chatId, "Não conheço esse comando. Envie /ajuda para ver o que eu sei fazer.", null);
                } else {
                    comUsuario(chatId, usuario -> registrar(chatId, usuario, texto));
                }
            }
        }
    }

    private void vincular(Long chatId, String codigo) {
        Optional<User> usuario = vinculoService.vincular(codigo, chatId);
        if (usuario.isEmpty()) {
            enviar(chatId, "❌ Código inválido ou expirado.\n\nGere um novo código no Nick Finance, em <b>Telegram</b>.", null);
            return;
        }
        enviar(chatId, "🎉 Pronto, <b>" + escapar(usuario.get().getNome()) + "</b>! Sua conta está conectada.\n\n" + ajuda(), null);
    }

    private void boasVindas(Long chatId) {
        if (vinculoService.usuarioDoChat(chatId).isPresent()) {
            enviar(chatId, "👋 Olá de novo! Sua conta já está conectada.\n\n" + ajuda(), null);
            return;
        }
        enviar(chatId, """
                👋 Olá! Eu sou o bot do <b>Nick Finance</b>.

                Para começar, conecte sua conta:
                1. Abra o Nick Finance e vá em <b>Telegram</b>
                2. Clique em <b>Gerar código</b>
                3. Envie aqui: <code>/vincular SEUCODIGO</code>""", null);
    }

    private void desvincular(Long chatId) {
        boolean removido = vinculoService.desvincularChat(chatId);
        enviar(chatId, removido
                ? "🔌 Conta desconectada. Para conectar de novo, gere um código no Nick Finance."
                : "Este chat não está conectado a nenhuma conta.", null);
    }

    private void registrar(Long chatId, User usuario, String texto) {
        LancamentoInterpretado lancamento;
        try {
            lancamento = parser.interpretar(texto, LocalDate.now(FUSO));
        } catch (MensagemNaoEntendidaException e) {
            enviar(chatId, "🤔 " + escapar(e.getMessage()) + "\n\nExemplos:\n" + exemplos(), null);
            return;
        }

        TelegramLancamentoService.Registro registro = lancamentoService.registrar(usuario, lancamento);
        boolean receita = lancamento.tipo() == TipoLancamento.RECEITA;

        StringBuilder resposta = new StringBuilder()
                .append(receita ? "💰 <b>Entrada registrada</b>\n\n" : "✅ <b>Despesa registrada</b>\n\n")
                .append("📝 ").append(escapar(lancamento.descricao())).append('\n');
        if (lancamento.detalhe() != null) {
            resposta.append(receita ? "🏢 " : "🏪 ").append(escapar(lancamento.detalhe())).append('\n');
        }
        if (registro.categoria() != null) {
            resposta.append("🏷️ ").append(escapar(registro.categoria())).append('\n');
        }
        resposta.append(receita ? "➕ " : "➖ ").append(moeda(lancamento.valor())).append('\n')
                .append("📅 ").append(lancamento.data().format(DATA));

        if (registro.orcamento() != null) {
            OrcamentoService.Situacao orcamento = registro.orcamento();
            boolean estourou = orcamento.gasto().compareTo(orcamento.limite()) > 0;
            resposta.append("\n\n").append(estourou ? "🚨 " : "⚠️ ")
                    .append("Você já usou <b>").append(orcamento.percentual()).append("%</b> do orçamento de ")
                    .append(escapar(registro.categoria())).append(" (")
                    .append(moeda(orcamento.gasto())).append(" de ").append(moeda(orcamento.limite())).append(").");
        }

        InlineKeyboardButton desfazer = InlineKeyboardButton.builder()
                .text("↩️ Desfazer")
                .callbackData(DESFAZER + ":" + registro.tipo() + ":" + registro.id())
                .build();
        enviar(chatId, resposta.toString(), InlineKeyboardMarkup.builder().keyboardRow(List.of(desfazer)).build());
    }

    private void saldo(Long chatId, User usuario) {
        YearMonth mes = YearMonth.now(FUSO);
        TelegramLancamentoService.Resumo resumo = lancamentoService.resumoDoMes(usuario, mes);
        String nomeMes = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);

        enviar(chatId, "📊 <b>Resumo de " + nomeMes + "</b>\n\n"
                + "💰 Entrou: " + moeda(resumo.entradas()) + "\n"
                + "💸 Saiu: " + moeda(resumo.saidas()) + "\n"
                + (resumo.saldo().signum() < 0 ? "🔴" : "🟢") + " Saldo: <b>" + moeda(resumo.saldo()) + "</b>", null);
    }

    // ---------- Botões ----------

    private void tratarBotao(CallbackQuery callback) {
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        String[] partes = callback.getData().split(":");

        String aviso = "Não foi possível desfazer.";
        if (partes.length == 3 && partes[0].equals(DESFAZER)) {
            Optional<User> usuario = vinculoService.usuarioDoChat(chatId);
            boolean desfeito = usuario.isPresent() && lancamentoService.desfazer(
                    usuario.get(), TipoLancamento.valueOf(partes[1]), Long.valueOf(partes[2]));
            if (desfeito) {
                aviso = "Lançamento removido.";
                editar(chatId, message.getMessageId(), "↩️ <s>" + escapar(message.getText()) + "</s>\n\n<i>Lançamento desfeito.</i>");
            }
        }

        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).text(aviso).build());
        } catch (TelegramApiException e) {
            log.warn("Falha ao responder botão do Telegram", e);
        }
    }

    // ---------- Apoio ----------

    private void comUsuario(Long chatId, java.util.function.Consumer<User> acao) {
        vinculoService.usuarioDoChat(chatId).ifPresentOrElse(acao, () -> boasVindas(chatId));
    }

    private static String ajuda() {
        return "É só me mandar uma mensagem com o lançamento:\n" + exemplos() + "\n\n"
                + "Comandos:\n"
                + "/saldo — resumo do mês\n"
                + "/contas — contas a pagar e a receber dos próximos dias\n"
                + "/desvincular — desconectar este chat\n"
                + "/ajuda — esta mensagem";
    }

    private static String exemplos() {
        return """
                • <code>gastei 45,90 com almoço no Restaurante Sabor</code>
                • <code>recebi 5200 de salário da Empresa X</code>
                • <code>uber 32,40 ontem</code>
                • <code>+150 venda 05/10</code>""";
    }

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Envia uma mensagem avulsa (HTML) para um chat. Usado pelos lembretes agendados. */
    public void enviarAviso(Long chatId, String textoHtml) {
        enviar(chatId, textoHtml, null);
    }

    private void enviar(Long chatId, String texto, InlineKeyboardMarkup teclado) {
        SendMessage mensagem = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(texto)
                .parseMode(ParseMode.HTML)
                .replyMarkup(teclado)
                .build();
        try {
            execute(mensagem);
        } catch (TelegramApiException e) {
            log.warn("Falha ao enviar mensagem para o chat {}", chatId, e);
        }
    }

    private void editar(Long chatId, Integer messageId, String texto) {
        try {
            execute(EditMessageText.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(messageId)
                    .text(texto)
                    .parseMode(ParseMode.HTML)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Falha ao editar mensagem do chat {}", chatId, e);
        }
    }
}
