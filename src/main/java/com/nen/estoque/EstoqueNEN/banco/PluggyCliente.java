package com.nen.estoque.EstoqueNEN.banco;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente da API da Pluggy. A API key é gerada com o client id e o secret e
 * renovada sozinha; nenhuma chave fica no navegador.
 */
@Slf4j
@Component
public class PluggyCliente {

    /** A API key da Pluggy vale por um tempo limitado; renova antes de vencer. */
    private static final Duration VALIDADE_API_KEY = Duration.ofMinutes(90);
    private static final ObjectMapper JSON = new ObjectMapper();

    public record Item(String id, String clientUserId, String nomeBanco, String status, String erro) {
    }

    /** @param tipo BANK ou CREDIT */
    public record Conta(String id, String tipo, String nome) {
    }

    /** @param valor como a Pluggy envia, com sinal */
    public record Transacao(String id, String descricao, BigDecimal valor, LocalDate data, String tipo,
                            String status, String metodoPagamento) {
    }

    public static class PluggyException extends RuntimeException {
        private final int status;

        public PluggyException(String mensagem, int status) {
            super(mensagem);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private final RestClient cliente;
    private final String clientId;
    private final String clientSecret;

    private String apiKey;
    private Instant apiKeyExpiraEm = Instant.EPOCH;

    @Autowired
    public PluggyCliente(@Value("${pluggy.client-id:}") String clientId,
                         @Value("${pluggy.client-secret:}") String clientSecret,
                         @Value("${pluggy.url:https://api.pluggy.ai}") String url) {
        this(RestClient.builder().baseUrl(url).requestFactory(fabrica()).build(), clientId, clientSecret);
    }

    PluggyCliente(RestClient cliente, String clientId, String clientSecret) {
        this.cliente = cliente;
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(40));
        return fabrica;
    }

    public boolean configurado() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    /** O `clientId` que vem nos webhooks, para conferir que são nossos. */
    public String clientId() {
        return clientId;
    }

    /** Os avisos chegam pelo webhook cadastrado no painel da Pluggy, não por token. */
    public String criarConnectToken(String clientUserId) {
        Map<String, Object> opcoes = Map.of("clientUserId", clientUserId, "avoidDuplicates", true);
        return post("/connect_token", Map.of("options", opcoes)).path("accessToken").asText();
    }

    public Item buscarItem(String itemId) {
        JsonNode item = get("/items/" + itemId, Map.of());
        JsonNode erro = item.path("error");
        return new Item(
                item.path("id").asText(),
                item.path("clientUserId").asText(null),
                item.path("connector").path("name").asText(null),
                item.path("status").asText(null),
                erro.isMissingNode() || erro.isNull() ? null : erro.path("message").asText(null));
    }

    public List<Conta> contas(String itemId) {
        List<Conta> contas = new ArrayList<>();
        for (JsonNode conta : get("/accounts", Map.of("itemId", itemId)).path("results")) {
            String nome = conta.path("marketingName").asText("");
            contas.add(new Conta(conta.path("id").asText(), conta.path("type").asText(),
                    nome.isBlank() ? conta.path("name").asText("Conta") : nome));
        }
        return contas;
    }

    /** Todas as transações da conta desde a data, seguindo o cursor do /v2/transactions. */
    public List<Transacao> transacoes(String contaId, LocalDate desde) {
        List<Transacao> transacoes = new ArrayList<>();
        String cursor = null;
        do {
            Map<String, String> parametros = new LinkedHashMap<>();
            parametros.put("accountId", contaId);
            parametros.put("dateFrom", desde.toString());
            if (cursor != null) {
                parametros.put("after", cursor);
            }
            JsonNode pagina = get("/v2/transactions", parametros);
            for (JsonNode t : pagina.path("results")) {
                transacoes.add(new Transacao(
                        t.path("id").asText(),
                        t.path("description").asText(""),
                        new BigDecimal(t.path("amount").asText("0")),
                        OffsetDateTime.parse(t.path("date").asText()).toLocalDate(),
                        t.path("type").asText(),
                        t.path("status").asText("POSTED"),
                        t.path("paymentData").path("paymentMethod").asText(null)));
            }
            cursor = proximoCursor(pagina.path("next").asText(null));
        } while (cursor != null);
        return transacoes;
    }

    /** O `next` vem como "?accountId=...&after=..."; só o `after` interessa. */
    static String proximoCursor(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        return UriComponentsBuilder.fromUriString(next).build().getQueryParams().getFirst("after");
    }

    public void excluirItem(String itemId) {
        executar(() -> cliente.delete().uri("/items/" + itemId).header("X-API-KEY", apiKey())
                .retrieve().toBodilessEntity());
    }

    // ---------- HTTP ----------

    private JsonNode get(String caminho, Map<String, String> parametros) {
        return executar(() -> cliente.get()
                .uri(builder -> {
                    builder.path(caminho);
                    parametros.forEach(builder::queryParam);
                    return builder.build();
                })
                .header("X-API-KEY", apiKey())
                .retrieve()
                .body(JsonNode.class));
    }

    private JsonNode post(String caminho, Object corpo) {
        return executar(() -> cliente.post().uri(caminho)
                .header("X-API-KEY", apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes(corpo))
                .retrieve()
                .body(JsonNode.class));
    }

    private synchronized String apiKey() {
        if (apiKey == null || Instant.now().isAfter(apiKeyExpiraEm)) {
            JsonNode resposta = cliente.post().uri("/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bytes(Map.of("clientId", clientId, "clientSecret", clientSecret)))
                    .retrieve()
                    .body(JsonNode.class);
            apiKey = resposta == null ? null : resposta.path("apiKey").asText(null);
            if (apiKey == null) {
                throw new PluggyException("A Pluggy não devolveu a API key", 502);
            }
            apiKeyExpiraEm = Instant.now().plus(VALIDADE_API_KEY);
        }
        return apiKey;
    }

    private <T> T executar(java.util.function.Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                // Chave vencida antes da hora: a próxima chamada gera outra.
                synchronized (this) {
                    apiKey = null;
                }
            }
            log.warn("Pluggy respondeu HTTP {}", e.getStatusCode().value());
            throw new PluggyException("A Pluggy respondeu HTTP " + e.getStatusCode().value(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("Falha ao chamar a Pluggy: {}", e.getClass().getSimpleName());
            throw new PluggyException("Falha ao chamar a Pluggy", HttpStatus.BAD_GATEWAY.value());
        }
    }

    private static byte[] bytes(Object corpo) {
        try {
            // Em bytes para sair com Content-Length, em vez de transferência em partes.
            return JSON.writeValueAsBytes(corpo);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
