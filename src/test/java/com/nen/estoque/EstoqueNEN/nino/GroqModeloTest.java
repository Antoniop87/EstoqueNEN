package com.nen.estoque.EstoqueNEN.nino;

import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Ferramenta;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Mensagem;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.ModeloIndisponivelException;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Resposta;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqModeloTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://groq.test/v1");
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final GroqModelo modelo = new GroqModelo(builder.build(), "chave-teste", "modelo-teste");

    private static final List<Ferramenta> FERRAMENTAS =
            List.of(new Ferramenta("metas", "Lista metas", Map.of("type", "object", "properties", Map.of())));

    @Test
    void enviaAutenticacaoEFerramentasELeTexto() {
        servidor.expect(requestTo("https://groq.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-teste"))
                .andExpect(pedido -> assertTrue(pedido.getHeaders().getContentLength() > 0, "corpo com tamanho definido"))
                .andExpect(jsonPath("$.model").value("modelo-teste"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.tools[0].function.name").value("metas"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"Olá"}}]}
                        """, MediaType.APPLICATION_JSON));

        Resposta resposta = modelo.conversar(List.of(Mensagem.de("user", "oi")), FERRAMENTAS);

        assertEquals("Olá", resposta.texto());
        assertTrue(resposta.chamadas().isEmpty());
        servidor.verify();
    }

    @Test
    void leChamadaDeFerramentaComConteudoNulo() {
        servidor.expect(requestTo("https://groq.test/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":null,
                          "tool_calls":[{"id":"c1","type":"function","function":{"name":"metas","arguments":"{}"}}]}}]}
                        """, MediaType.APPLICATION_JSON));

        Resposta resposta = modelo.conversar(List.of(Mensagem.de("user", "oi")), FERRAMENTAS);

        assertEquals("", resposta.texto());
        assertEquals("metas", resposta.chamadas().get(0).nome());
        assertEquals("c1", resposta.chamadas().get(0).id());
    }

    @Test
    void reenviaChamadaEResultadoNoFormatoDoProvedor() {
        servidor.expect(requestTo("https://groq.test/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[0].tool_calls[0].function.name").value("metas"))
                .andExpect(jsonPath("$.messages[1].role").value("tool"))
                .andExpect(jsonPath("$.messages[1].tool_call_id").value("c1"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"ok"}}]}
                        """, MediaType.APPLICATION_JSON));

        modelo.conversar(List.of(
                Mensagem.doModelo(new Resposta("", List.of(new ModeloLinguagem.Chamada("c1", "metas", "{}")))),
                Mensagem.daFerramenta("c1", "[]")), FERRAMENTAS);

        servidor.verify();
    }

    @Test
    void erroDoProvedorViraIndisponivelSemExporChave() {
        servidor.expect(requestTo("https://groq.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ModeloIndisponivelException erro = assertThrows(ModeloIndisponivelException.class,
                () -> modelo.conversar(List.of(Mensagem.de("user", "oi")), FERRAMENTAS));
        assertFalse(erro.getMessage().contains("chave-teste"));
    }

    @Test
    void semChaveNaoEstaConfigurado() {
        assertFalse(new GroqModelo(builder.build(), "  ", "m").configurado());
        assertTrue(modelo.configurado());
    }
}
