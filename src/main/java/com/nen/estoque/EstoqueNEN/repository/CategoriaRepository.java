package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByUsuarioOrderByTipoAscNomeAsc(User usuario);

    List<Categoria> findAllByUsuarioAndTipo(User usuario, TipoLancamento tipo);

    Optional<Categoria> findByIdAndUsuario(Long id, User usuario);

    boolean existsByUsuario(User usuario);

}
