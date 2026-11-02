package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.Receita;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    List<Receita> findAllByUsuarioOrderByDataDescIdDesc(User usuario);

    Optional<Receita> findByIdAndUsuario(Long id, User usuario);

    @Query("SELECT COALESCE(SUM(r.valor), 0) FROM Receita r " +
            "WHERE r.usuario = :usuario AND r.data >= :inicio AND r.data < :fim")
    BigDecimal somarNoPeriodo(@Param("usuario") User usuario, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Receita r SET r.categoria = null WHERE r.categoria = :categoria")
    void limparCategoria(@Param("categoria") Categoria categoria);

}
