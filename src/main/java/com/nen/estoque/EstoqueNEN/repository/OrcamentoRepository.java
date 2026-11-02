package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.Orcamento;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findAllByUsuario(User usuario);

    Optional<Orcamento> findByIdAndUsuario(Long id, User usuario);

    Optional<Orcamento> findByCategoria(Categoria categoria);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Orcamento o WHERE o.categoria = :categoria")
    void deleteAllByCategoria(@Param("categoria") Categoria categoria);

}
