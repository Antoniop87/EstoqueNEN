package com.nen.estoque.EstoqueNEN.nino;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modelo servido pelo Groq, pela API compatível com o formato da OpenAI. */
@Slf4j
@Component
public class GroqModelo implements ModeloLinguagem {

    private static final int MAX_TOKENS = 1500;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient cliente;
    private final String chave;
    private final String modelo;

    @Autowired
    public GroqModelo(@Value("${groq.api-key:}") String chave,
                      @Value("${groq.model:openai/gpt-oss-120b}") String modelo,
                      @Value("${groq.url:https://api.groq.com/openai/v1}") String url) {
        this(RestClient.builder().baseUrl(url).requestFactory(fabrica()).build(), chave, modelo);
    }

    GroqModelo(RestClient cliente, String chave, String modelo) {
        this.cliente = cliente;
        this.chave = chave == null ? "" : chave.trim();
        this.modelo = modelo;
    }

    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(45));
        return fabrica;
    }

    @Override
    public boolean configurado() {
        return !chave.isEmpty();
    }

    @Override
    public Resposta conversar(List<Mensagem> mensagens, List<Ferramenta> ferramentas) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", modelo);
        corpo.put("temperature", 0.2);
        corpo.put("max_completion_tokens", MAX_TOKENS);
        corpo.put("messages", mensagens.stream().map(GroqModelo::paraJson).toList());
        if (!ferramentas.isEmpty()) {
            corpo.put("tools", ferramentas.stream().map(GroqModelo::paraJson).toList());
            corpo.put("tool_choice", "auto");
        }

        try {
            JsonNode json = cliente.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + chave)
                    .contentType(MediaType.APPLICATION_JSON)
                    // Em bytes para sair com Content-Length, em vez de transferência em partes.
                    .body(JSON.writeValueAsBytes(corpo))
                    .retrieve()
                    .body(JsonNode.class);
            return lerResposta(json);
        } catch (RestClientResponseException e) {
            // Só o status vai para o log: o corpo pode repetir dados financeiros do usuário.
            log.warn("Groq respondeu HTTP {}", e.getStatusCode().value());
            throw new ModeloIndisponivelException("O Groq respondeu HTTP " + e.getStatusCode().value());
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Falha ao chamar o Groq: {}", e.getClass().getSimpleName());
            throw new ModeloIndisponivelException("Falha ao chamar o Groq");
        }
    }

    private static Resposta lerResposta(JsonNode json) {
        JsonNode mensagem = json == null ? null : json.path("choices").path(0).path("message");
        if (mensagem == null || mensagem.isMissingNode()) {
            throw new ModeloIndisponivelException("Resposta do Groq sem mensagem");
        }

        List<Chamada> chamadas = new ArrayList<>();
        for (JsonNode chamada : mensagem.path("tool_calls")) {
            chamadas.add(new Chamada(
                    chamada.path("id").asText(),
                    chamada.path("function").path("name").asText(),
                    chamada.path("function").path("arguments").asText("{}")));
        }
        String texto = mensagem.path("content").isNull() ? "" : mensagem.path("content").asText("");
        return new Resposta(texto, chamadas);
    }

    private static Map<String, Object> paraJson(Mensagem mensagem) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("role", mensagem.papel());
        json.put("content", mensagem.conteudo() == null ? "" : mensagem.conteudo());
        if (mensagem.idChamada() != null) {
            json.put("tool_call_id", mensagem.idChamada());
        }
        if (!mensagem.chamadas().isEmpty()) {
            json.put("tool_calls", mensagem.chamadas().stream().map(c -> Map.of(
                    "id", c.id(),
                    "type", "function",
                    "function", Map.of("name", c.nome(), "arguments", c.argumentos()))).toList());
        }
        return json;
    }

    private static Map<String, Object> paraJson(Ferramenta ferramenta) {
        return Map.of("type", "function", "function", Map.of(
                "name", ferramenta.nome(),
                "description", ferramenta.descricao(),
                "parameters", ferramenta.parametros()));
    }
}
