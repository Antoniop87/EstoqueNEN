package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.LoginUserDto;
import com.nen.estoque.EstoqueNEN.dto.RecoveryJwtTokenDto;
import com.nen.estoque.EstoqueNEN.model.Produto;
import com.nen.estoque.EstoqueNEN.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produto")
public class ProdutoController {

    @Autowired
    private ProdutoService service;

    @PostMapping
    public ResponseEntity<Void> cadastraProduto(@RequestBody Produto produto) {
        service.salvaProduto(produto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/atualiza")
    public ResponseEntity<Void> atualizaProduto(@RequestBody Produto produto){
        service.atualizaProduto(produto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteProduto(@PathVariable Long id){
        service.deleteProduto(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/pegaProduto/{id}")
    public ResponseEntity<Produto> pegaProdutoPorId(@PathVariable Long id){
        Produto produto = service.pegaProdutoPorId(id);
        System.out.println("bateu");
        return new ResponseEntity<>(produto, HttpStatus.OK);
    }

    @GetMapping("/lista")
    public ResponseEntity<List<Produto>> listaTodosProdutos(){
       List<Produto> produtos = service.listaTodosProdutos();
        return new ResponseEntity<>(produtos, HttpStatus.OK);
    }

}
