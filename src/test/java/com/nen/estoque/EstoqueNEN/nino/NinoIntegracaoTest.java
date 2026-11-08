package com.nen.estoque.EstoqueNEN.nino;

import com.nen.estoque.EstoqueNEN.dto.DespesaDto;
import com.nen.estoque.EstoqueNEN.dto.NinoDto;
import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Chamada;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Mensagem;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.ModeloIndisponivelException;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Resposta;
import com.nen.estoque.EstoqueNEN.repository.UserRepository;
import com.nen.estoque.EstoqueNEN.service.DespesaService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Conversa com o modelo simulado, contra o banco real. Cada teste é revertido ao final. */
@SpringBootTest(properties = {"nino.limite-diario=2", "telegram.bot.token="})
@Transactional
class NinoIntegracaoTest {

    @MockitoBean private ModeloLinguagem modelo;
    @Autowired private NinoService nino;
    @Autowired private NinoFerramentas ferramentas;
    @Autowired private DespesaService despesas;
    @Autowired private UserRepository users;

    private static final NinoDto.Request PERGUNTA =
            new NinoDto.Request(List.of(new NinoDto.Mensagem("user", "quanto gastei?")));

    @BeforeEach
    void setUp() {
        autenticarComo(novoUsuario());
        when(modelo.configurado()).thenReturn(true);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executaFerramentaEDevolveATextoFinal() {
        despesas.criar(new DespesaDto.Request("Tênis", null, new BigDecimal("100.00"), Datas.hoje(), null, FormaPagamento.CREDITO));
        when(modelo.conversar(any(), any()))
                .thenReturn(new Resposta("", List.of(new Chamada("c1", "resumo_do_mes", "{}"))))
                .thenReturn(new Resposta("Você gastou R$ 100,00.", List.of()));

        NinoDto.Response resposta = nino.responder(PERGUNTA);

        assertEquals("Você gastou R$ 100,00.", resposta.resposta());
        assertEquals(1, resposta.restantesHoje());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mensagem>> enviadas = ArgumentCaptor.forClass(List.class);
        verify(modelo, times(2)).conversar(enviadas.capture(), any());
        Mensagem resultado = enviadas.getAllValues().get(1).stream()
                .filter(m -> m.papel().equals("tool")).findFirst().orElseThrow();
        assertEquals("c1", resultado.idChamada());
        assertTrue(resultado.conteudo().contains("\"saidasNoCredito\":100"));
    }

    @Test
    void naoEnxergaDadosDeOutroUsuario() {
        despesas.criar(new DespesaDto.Request("Segredo do vizinho", null, BigDecimal.TEN, Datas.hoje(), null, null));

        autenticarComo(novoUsuario());

        assertFalse(ferramentas.executar("listar_lancamentos", "{}").contains("Segredo"));
    }

    @Test
    void naoEnviaEmailNemNomeDoUsuarioAoModelo() {
        when(modelo.conversar(any(), any())).thenReturn(new Resposta("Oi", List.of()));

        nino.responder(PERGUNTA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mensagem>> enviadas = ArgumentCaptor.forClass(List.class);
        verify(modelo).conversar(enviadas.capture(), any());
        assertTrue(enviadas.getValue().stream().noneMatch(m -> m.conteudo().contains("nino-it-")));
    }

    @Test
    void bloqueiaAoAtingirOLimiteDiario() {
        when(modelo.conversar(any(), any())).thenReturn(new Resposta("Oi", List.of()));
        nino.responder(PERGUNTA);
        nino.responder(PERGUNTA);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> nino.responder(PERGUNTA));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatusCode());
        verify(modelo, times(2)).conversar(any(), any());
    }

    @Test
    void falhaDoProvedorNaoGastaACota() {
        when(modelo.conversar(any(), any())).thenThrow(new ModeloIndisponivelException("fora"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> nino.responder(PERGUNTA));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        assertEquals(2, nino.status().restantesHoje());
    }

    @Test
    void paraDeRodarSeOModeloSoPedirFerramentas() {
        when(modelo.conversar(any(), any()))
                .thenReturn(new Resposta("", List.of(new Chamada("c", "metas", "{}"))));

        assertThrows(ResponseStatusException.class, () -> nino.responder(PERGUNTA));

        verify(modelo, times(5)).conversar(any(), any());
    }

    @Test
    void ferramentaInvalidaVoltaComoErroParaOModelo() {
        assertTrue(ferramentas.executar("apagar_tudo", "{}").contains("erro"));
        assertTrue(ferramentas.executar("resumo_do_mes", "{\"mes\":\"abc\"}").contains("erro"));
        assertTrue(ferramentas.executar("listar_lancamentos", "não é json").contains("erro"));
    }

    @Test
    void semChaveInformaIndisponivel() {
        when(modelo.configurado()).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> nino.responder(PERGUNTA));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        assertFalse(nino.status().ativo());
    }

    private User novoUsuario() {
        return users.save(User.builder()
                .email("nino-it-" + UUID.randomUUID() + "@nickfinance.test")
                .password("x").nome("Teste").sobrenome("Nino")
                .build());
    }

    private static void autenticarComo(User usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of()));
    }
}
