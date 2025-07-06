package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.model.Produto;
import com.nen.estoque.EstoqueNEN.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository repository;

    public void salvaProduto(Produto produto){
        repository.save(produto);
    }

    public void deleteProduto(Long id){
        repository.deleteById(id);
    }

    public Produto pegaProdutoPorId(Long id){
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

    }

    public List<Produto> listaTodosProdutos(){
        return repository.findAll();
    }

    public void atualizaProduto(Produto produto){
        repository.save(produto);
    }

}
