package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.dto.TelegramCodigoDto;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fluxo completo contra o banco: gerar código, vincular o chat, registrar por mensagem,
 * resumir o mês e desfazer. Roda em transação revertida ao final (não deixa dados).
 * Requer o PostgreSQL configurado no application.properties.
 */
@SpringBootTest
@Transactional
class TelegramIntegracaoTest {

    private static final long CHAT_ID = 987_654_321L;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 1);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TelegramVinculoService vinculoService;
    @Autowired
    private TelegramLancamentoService lancamentoService;
    @Autowired
    private MensagemParser parser;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void vinculaRegistraResumeEDesfaz() {
        User usuario = novoUsuario();
        autenticarComo(usuario);

        TelegramCodigoDto codigo = vinculoService.gerarCodigo();
        assertFalse(vinculoService.status().vinculado());

        // Código aceito sem diferenciar maiúsculas, e só uma vez.
        assertTrue(vinculoService.vincular(codigo.codigo().toLowerCase(), CHAT_ID).isPresent());
        assertTrue(vinculoService.vincular(codigo.codigo(), CHAT_ID).isEmpty());
        assertTrue(vinculoService.status().vinculado());

        User dono = vinculoService.usuarioDoChat(CHAT_ID).orElseThrow();
        assertEquals(usuario.getId(), dono.getId());

        TelegramLancamentoService.Registro salario =
                lancamentoService.registrar(dono, parser.interpretar("recebi 1.000 de salário", HOJE));
        lancamentoService.registrar(dono, parser.interpretar("gastei 250,50 no mercado", HOJE));
        lancamentoService.registrar(dono, parser.interpretar("cinema 40 05/10", HOJE)); // outro mês

        TelegramLancamentoService.Resumo novembro = lancamentoService.resumoDoMes(dono, YearMonth.of(2026, 11));
        assertEquals(0, new BigDecimal("1000").compareTo(novembro.entradas()));
        assertEquals(0, new BigDecimal("250.50").compareTo(novembro.saidas()));
        assertEquals(0, new BigDecimal("749.50").compareTo(novembro.saldo()));

        // Outro usuário não consegue desfazer o lançamento.
        assertFalse(lancamentoService.desfazer(novoUsuario(), salario.tipo(), salario.id()));
        assertTrue(lancamentoService.desfazer(dono, salario.tipo(), salario.id()));
        assertEquals(0, lancamentoService.resumoDoMes(dono, YearMonth.of(2026, 11)).entradas().signum());

        assertTrue(vinculoService.desvincularChat(CHAT_ID));
        assertTrue(vinculoService.usuarioDoChat(CHAT_ID).isEmpty());
    }

    @Test
    void codigoInexistenteNaoVincula() {
        assertTrue(vinculoService.vincular("ZZZZZZ", CHAT_ID).isEmpty());
    }

    private User novoUsuario() {
        return userRepository.save(User.builder()
                .email("telegram-it-" + UUID.randomUUID() + "@nickfinance.test")
                .password("x")
                .nome("Teste")
                .sobrenome("Integração")
                .build());
    }

    private static void autenticarComo(User usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of()));
    }
}
