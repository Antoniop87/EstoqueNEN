package com.nen.estoque.EstoqueNEN.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nen.estoque.EstoqueNEN.banco.BancoSincronizador;
import com.nen.estoque.EstoqueNEN.banco.PluggyCliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Avisos da Pluggy (público, sem token). O aviso é só um sinal: os dados são buscados de
 * volta na API com a nossa chave, e só para conexões que já existem aqui. Por isso um
 * aviso falso, no máximo, provoca uma sincronização a mais.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PluggyWebhookController {

    private final PluggyCliente pluggy;
    private final BancoSincronizador sincronizador;

    @PostMapping("/pluggy/webhook")
    public ResponseEntity<Map<String, Boolean>> receber(@RequestBody JsonNode aviso) {
        String clientId = aviso.path("clientId").asText("");
        if (!clientId.isEmpty() && !clientId.equals(pluggy.clientId())) {
            return ResponseEntity.ok(Map.of("received", true));
        }

        String evento = aviso.path("event").asText("");
        String itemId = aviso.path("itemId").asText("");
        log.info("Webhook da Pluggy: {}", evento);
        if (!itemId.isEmpty() && (evento.startsWith("item/") || evento.startsWith("transactions/"))
                && !evento.equals("item/deleted")) {
            // A Pluggy exige resposta em até 10 segundos: a sincronização roda depois.
            sincronizador.sincronizarItemEmSegundoPlano(itemId);
        }
        return ResponseEntity.ok(Map.of("received", true));
    }
}
