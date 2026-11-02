package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.CompromissoDto;
import com.nen.estoque.EstoqueNEN.service.CompromissoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contas a pagar e a receber.
 */
@RestController
@RequestMapping("/compromisso")
@RequiredArgsConstructor
public class CompromissoController {

    private final CompromissoService service;

    @GetMapping
    public ResponseEntity<List<CompromissoDto.Response>> listar() {
        return ResponseEntity.ok(service.listarDoUsuarioLogado());
    }

    @PostMapping
    public ResponseEntity<CompromissoDto.Response> criar(@RequestBody @Valid CompromissoDto.Request dto) {
        return new ResponseEntity<>(service.criar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompromissoDto.Response> atualizar(@PathVariable Long id, @RequestBody @Valid CompromissoDto.Request dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Marca como paga e cria o lançamento correspondente. */
    @PostMapping("/{id}/pagar")
    public ResponseEntity<CompromissoDto.Response> pagar(@PathVariable Long id,
                                                         @RequestBody(required = false) CompromissoDto.Pagamento pagamento) {
        return ResponseEntity.ok(service.pagar(id, pagamento == null ? null : pagamento.data()));
    }

    /** Desfaz o pagamento e remove o lançamento criado. */
    @PostMapping("/{id}/reabrir")
    public ResponseEntity<CompromissoDto.Response> reabrir(@PathVariable Long id) {
        return ResponseEntity.ok(service.reabrir(id));
    }
}
