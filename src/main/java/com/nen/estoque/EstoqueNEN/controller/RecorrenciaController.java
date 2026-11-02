package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.RecorrenciaDto;
import com.nen.estoque.EstoqueNEN.service.RecorrenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lançamentos recorrentes (gerados automaticamente todo mês).
 */
@RestController
@RequestMapping("/recorrencia")
@RequiredArgsConstructor
public class RecorrenciaController {

    private final RecorrenciaService service;

    @GetMapping
    public ResponseEntity<List<RecorrenciaDto.Response>> listar() {
        return ResponseEntity.ok(service.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<RecorrenciaDto.Response> criar(@RequestBody @Valid RecorrenciaDto.Request dto) {
        return new ResponseEntity<>(service.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecorrenciaDto.Response> atualizar(@PathVariable Long id, @RequestBody @Valid RecorrenciaDto.Request dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
