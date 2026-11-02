package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Meta;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetaRepository extends JpaRepository<Meta, Long> {

    List<Meta> findAllByUsuarioOrderByCriadaEmAscIdAsc(User usuario);

    Optional<Meta> findByIdAndUsuario(Long id, User usuario);

}
