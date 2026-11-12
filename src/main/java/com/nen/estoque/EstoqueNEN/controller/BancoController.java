package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.banco.BancoService;
import com.nen.estoque.EstoqueNEN.dto.BancoDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Integração bancária pela Pluggy. Tudo é do usuário do token. */
@RestController
@RequestMapping("/bancos")
@RequiredArgsConstructor
public class BancoController {

    private final BancoService service;

    @GetMapping
    public ResponseEntity<BancoDto.Status> status() {
        return ResponseEntity.ok(service.status());
    }

    @PostMapping("/connect-token")
    public ResponseEntity<BancoDto.ConnectToken> connectToken() {
        return ResponseEntity.ok(service.connectToken());
    }

    @PostMapping("/conexoes")
    public ResponseEntity<BancoDto.Conexao> conectar(@RequestBody @Valid BancoDto.NovaConexao dto) {
        return new ResponseEntity<>(service.conectar(dto.itemId()), HttpStatus.CREATED);
    }

    @PostMapping("/conexoes/{id}/sincronizar")
    public ResponseEntity<BancoDto.Resultado> sincronizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.sincronizar(id));
    }

    @DeleteMapping("/conexoes/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transacoes")
    public ResponseEntity<List<BancoDto.Transacao>> pendentes() {
        return ResponseEntity.ok(service.pendentes());
    }

    @PostMapping("/transacoes/{id}/importar")
    public ResponseEntity<Void> importar(@PathVariable Long id, @RequestBody(required = false) BancoDto.Importacao dto) {
        service.importar(id, dto == null ? null : dto.categoriaId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transacoes/{id}/ignorar")
    public ResponseEntity<Void> ignorar(@PathVariable Long id) {
        service.ignorar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transacoes/importar-todas")
    public ResponseEntity<BancoDto.Resultado> importarTodas() {
        return ResponseEntity.ok(service.importarTodas());
    }
}
