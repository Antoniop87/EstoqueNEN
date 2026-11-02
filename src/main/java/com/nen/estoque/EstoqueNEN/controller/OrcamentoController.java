package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.OrcamentoDto;
import com.nen.estoque.EstoqueNEN.service.OrcamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Orçamentos mensais por categoria de despesa.
 */
@RestController
@RequestMapping("/orcamento")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;

    @GetMapping
    public ResponseEntity<List<OrcamentoDto.Response>> listar() {
        return ResponseEntity.ok(service.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<OrcamentoDto.Response> criar(@RequestBody @Valid OrcamentoDto.Request dto) {
        return new ResponseEntity<>(service.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoDto.Response> atualizar(@PathVariable Long id, @RequestBody @Valid OrcamentoDto.Request dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
