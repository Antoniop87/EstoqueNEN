package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.Despesa;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findAllByUsuarioOrderByDataDespesaDescIdDesc(User usuario);

    Optional<Despesa> findByIdAndUsuario(Long id, User usuario);

    @Query("SELECT COALESCE(SUM(d.valorDespesa), 0) FROM Despesa d " +
            "WHERE d.usuario = :usuario AND d.dataDespesa >= :inicio AND d.dataDespesa < :fim")
    BigDecimal somarNoPeriodo(@Param("usuario") User usuario, @Param("inicio") Date inicio, @Param("fim") Date fim);

    @Query("SELECT COALESCE(SUM(d.valorDespesa), 0) FROM Despesa d " +
            "WHERE d.categoria = :categoria AND d.dataDespesa >= :inicio AND d.dataDespesa < :fim")
    BigDecimal somarPorCategoriaNoPeriodo(@Param("categoria") Categoria categoria,
                                          @Param("inicio") Date inicio, @Param("fim") Date fim);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Despesa d SET d.categoria = null WHERE d.categoria = :categoria")
    void limparCategoria(@Param("categoria") Categoria categoria);

}
