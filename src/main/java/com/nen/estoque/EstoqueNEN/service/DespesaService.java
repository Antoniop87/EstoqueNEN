package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.model.Despesa;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DespesaService {

    @Autowired
    private DespesaRepository repository;

    public void salvaProduto(Despesa despesa){
        repository.save(despesa);
    }

    public List<Despesa> listarPorUsuarioId(User usuarioId) {
        return repository.findAllByUsuario(usuarioId);
    }


}
