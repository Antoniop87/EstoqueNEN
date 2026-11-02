package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.service.OrcamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testa o roteamento do bot sem falar com o Telegram: as chamadas à API são capturadas.
 */
class NickFinanceBotTest {

    private static final long CHAT_ID = 10L;

    private TelegramVinculoService vinculoService;
    private TelegramLancamentoService lancamentoService;
    private final List<BotApiMethod<?>> enviados = new ArrayList<>();
    private NickFinanceBot bot;

    @BeforeEach
    void setUp() {
        vinculoService = mock(TelegramVinculoService.class);
        lancamentoService = mock(TelegramLancamentoService.class);
        bot = new NickFinanceBot(new TelegramBotProperties("123:token", "nick_bot"),
                vinculoService, lancamentoService, new MensagemParser(), mock(TelegramLembretes.class)) {
            @Override
            public <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) {
                enviados.add(method);
                return null;
            }
        };
    }

    @Test
    void registraDespesaDeUsuarioVinculado() {
        User usuario = User.builder().id(7L).nome("Ana").build();
        when(vinculoService.usuarioDoChat(CHAT_ID)).thenReturn(Optional.of(usuario));
        when(lancamentoService.registrar(eq(usuario), any()))
                .thenReturn(new TelegramLancamentoService.Registro(TipoLancamento.DESPESA, 42L, "Alimentação",
                        new OrcamentoService.Situacao(new BigDecimal("100"), new BigDecimal("90"))));

        bot.onUpdateReceived(mensagem("gastei 45,90 no mercado"));

        ArgumentCaptor<LancamentoInterpretado> captor = ArgumentCaptor.forClass(LancamentoInterpretado.class);
        verify(lancamentoService).registrar(eq(usuario), captor.capture());
        assertEquals(TipoLancamento.DESPESA, captor.getValue().tipo());
        assertEquals(new BigDecimal("45.90"), captor.getValue().valor());

        SendMessage resposta = ultimaMensagem();
        assertTrue(resposta.getText().contains("Despesa registrada"));
        assertTrue(resposta.getText().contains("R$"));
        assertNotNull(resposta.getReplyMarkup(), "deve oferecer o botão Desfazer");
        assertTrue(resposta.getText().contains("Alimentação"), "deve mostrar a categoria");
        assertTrue(resposta.getText().contains("90%"), "deve avisar sobre o orçamento");
    }

    @Test
    void chatNaoVinculadoRecebeInstrucoes() {
        when(vinculoService.usuarioDoChat(CHAT_ID)).thenReturn(Optional.empty());

        bot.onUpdateReceived(mensagem("gastei 10 no mercado"));

        verifyNoInteractions(lancamentoService);
        assertTrue(ultimaMensagem().getText().contains("conecte sua conta"));
    }

    @Test
    void startComCodigoVinculaOChat() {
        when(vinculoService.vincular("ABC123", CHAT_ID))
                .thenReturn(Optional.of(User.builder().id(7L).nome("Ana").build()));

        bot.onUpdateReceived(mensagem("/start ABC123"));

        verify(vinculoService).vincular("ABC123", CHAT_ID);
        assertTrue(ultimaMensagem().getText().contains("Sua conta está conectada"));
    }

    @Test
    void codigoInvalidoAvisaOUsuario() {
        when(vinculoService.vincular(any(), any())).thenReturn(Optional.empty());

        bot.onUpdateReceived(mensagem("/vincular XXXXXX"));

        assertTrue(ultimaMensagem().getText().contains("inválido ou expirado"));
    }

    @Test
    void mensagemSemValorExplicaOFormato() {
        when(vinculoService.usuarioDoChat(CHAT_ID))
                .thenReturn(Optional.of(User.builder().id(7L).nome("Ana").build()));

        bot.onUpdateReceived(mensagem("oi tudo bem?"));

        verify(lancamentoService, never()).registrar(any(), any());
        assertTrue(ultimaMensagem().getText().contains("Não encontrei o valor"));
    }

    private static Update mensagem(String texto) {
        Message message = new Message();
        message.setChat(new Chat(CHAT_ID, "private"));
        message.setText(texto);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private SendMessage ultimaMensagem() {
        assertFalse(enviados.isEmpty(), "o bot deveria ter respondido");
        return (SendMessage) enviados.get(enviados.size() - 1);
    }
}
