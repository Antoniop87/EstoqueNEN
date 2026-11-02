package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.TelegramCodigoDto;
import com.nen.estoque.EstoqueNEN.dto.TelegramStatusDto;
import com.nen.estoque.EstoqueNEN.telegram.TelegramVinculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Integração com o Telegram da conta logada.
 */
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramVinculoService vinculoService;

    @GetMapping
    public ResponseEntity<TelegramStatusDto> status() {
        return new ResponseEntity<>(vinculoService.status(), HttpStatus.OK);
    }

    @PostMapping("/codigo")
    public ResponseEntity<TelegramCodigoDto> gerarCodigo() {
        return new ResponseEntity<>(vinculoService.gerarCodigo(), HttpStatus.CREATED);
    }

    @DeleteMapping
    public ResponseEntity<Void> desvincular() {
        vinculoService.desvincularUsuarioLogado();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
