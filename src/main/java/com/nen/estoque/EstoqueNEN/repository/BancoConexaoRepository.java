package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.BancoConexao;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BancoConexaoRepository extends JpaRepository<BancoConexao, Long> {

    List<BancoConexao> findAllByUsuarioOrderByCriadaEmAsc(User usuario);

    Optional<BancoConexao> findByIdAndUsuario(Long id, User usuario);

    Optional<BancoConexao> findByItemId(String itemId);
}
