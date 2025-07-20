package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.model.Despesa;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.service.DespesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/despesa")
public class DespesaController {

    @Autowired
    private DespesaService despesaService;

    @PostMapping
    public ResponseEntity<Void> cadastraProduto(@RequestBody Despesa despesa) {
        despesaService.salvaProduto(despesa);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/lista/usuario/{id}")
    public ResponseEntity<List<Despesa>> listarPorUsuario(@PathVariable("id") User usuarioId) {
        List<Despesa> despesas = despesaService.listarPorUsuarioId(usuarioId);
        return new ResponseEntity<>(despesas, HttpStatus.OK);
    }

}
