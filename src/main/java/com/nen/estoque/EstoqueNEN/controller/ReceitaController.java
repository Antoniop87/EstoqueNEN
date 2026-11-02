package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.ReceitaDto;
import com.nen.estoque.EstoqueNEN.service.ReceitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Entradas de dinheiro. O usuário vem sempre do token, nunca da URL ou do body.
 */
@RestController
@RequestMapping("/receita")
@RequiredArgsConstructor
public class ReceitaController {

    private final ReceitaService receitaService;

    @GetMapping
    public ResponseEntity<List<ReceitaDto.Response>> listar() {
        return ResponseEntity.ok(receitaService.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<ReceitaDto.Response> criar(@RequestBody @Valid ReceitaDto.Request dto) {
        return new ResponseEntity<>(receitaService.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReceitaDto.Response> atualizar(@PathVariable Long id, @RequestBody @Valid ReceitaDto.Request dto) {
        return ResponseEntity.ok(receitaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        receitaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
