package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.CategoriaDto;
import com.nen.estoque.EstoqueNEN.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categoria")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public ResponseEntity<List<CategoriaDto.Response>> listar() {
        return ResponseEntity.ok(service.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<CategoriaDto.Response> criar(@RequestBody @Valid CategoriaDto.Request dto) {
        return new ResponseEntity<>(service.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDto.Response> atualizar(@PathVariable Long id,
                                                           @RequestBody @Valid CategoriaDto.Request dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
