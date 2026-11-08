package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.NinoDto;
import com.nen.estoque.EstoqueNEN.nino.NinoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Assistente Nino. Responde sempre sobre os dados do usuário do token. */
@RestController
@RequestMapping("/nino")
@RequiredArgsConstructor
public class NinoController {

    private final NinoService service;

    @GetMapping
    public ResponseEntity<NinoDto.Status> status() {
        return ResponseEntity.ok(service.status());
    }

    @PostMapping("/mensagem")
    public ResponseEntity<NinoDto.Response> mensagem(@RequestBody @Valid NinoDto.Request dto) {
        return ResponseEntity.ok(service.responder(dto));
    }
}
