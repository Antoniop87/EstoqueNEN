package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.MetaDto;
import com.nen.estoque.EstoqueNEN.service.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Metas de economia.
 */
@RestController
@RequestMapping("/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService service;

    @GetMapping
    public ResponseEntity<List<MetaDto.Response>> listar() {
        return ResponseEntity.ok(service.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<MetaDto.Response> criar(@RequestBody @Valid MetaDto.Request dto) {
        return new ResponseEntity<>(service.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetaDto.Response> atualizar(@PathVariable Long id, @RequestBody @Valid MetaDto.Request dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Guarda (valor positivo) ou retira (negativo) dinheiro da meta. */
    @PostMapping("/{id}/movimento")
    public ResponseEntity<MetaDto.Response> movimentar(@PathVariable Long id,
                                                       @RequestBody @Valid MetaDto.Movimento movimento) {
        return ResponseEntity.ok(service.movimentar(id, movimento.valor()));
    }
}
