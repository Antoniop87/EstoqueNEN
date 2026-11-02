package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.Recorrencia;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecorrenciaRepository extends JpaRepository<Recorrencia, Long> {

    List<Recorrencia> findAllByUsuarioOrderByDiaDoMesAscIdAsc(User usuario);

    Optional<Recorrencia> findByIdAndUsuario(Long id, User usuario);

    List<Recorrencia> findAllByAtivaTrue();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Recorrencia r SET r.categoria = null WHERE r.categoria = :categoria")
    void limparCategoria(@Param("categoria") Categoria categoria);

}
