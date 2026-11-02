package com.nen.estoque.EstoqueNEN.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Verificação de saúde, pública. Usada pelo host (health check) e por monitores
 * que acessam a API periodicamente para ela não "dormir" no plano gratuito.
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok", "banco", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "erro", "banco", "indisponível"));
        }
    }
}
