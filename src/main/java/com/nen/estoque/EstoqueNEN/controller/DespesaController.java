package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.DespesaDto;
import com.nen.estoque.EstoqueNEN.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Despesas. O usuário vem sempre do token, nunca da URL ou do body.
 */
@RestController
@RequestMapping("/despesa")
@RequiredArgsConstructor
public class DespesaController {

    private final DespesaService despesaService;

    @GetMapping
    public ResponseEntity<List<DespesaDto.Response>> listar() {
        return ResponseEntity.ok(despesaService.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<DespesaDto.Response> criar(@RequestBody @Valid DespesaDto.Request dto) {
        return new ResponseEntity<>(despesaService.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DespesaDto.Response> atualizar(@PathVariable Long id, @RequestBody @Valid DespesaDto.Request dto) {
        return ResponseEntity.ok(despesaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        despesaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
