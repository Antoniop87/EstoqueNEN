package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.Compromisso;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompromissoRepository extends JpaRepository<Compromisso, Long> {

    List<Compromisso> findAllByUsuarioOrderByVencimentoAscIdAsc(User usuario);

    Optional<Compromisso> findByIdAndUsuario(Long id, User usuario);

    /** Contas em aberto do usuário que vencem até a data (inclui as atrasadas). */
    List<Compromisso> findAllByUsuarioAndPagoEmIsNullAndVencimentoLessThanEqualOrderByVencimentoAsc(
            User usuario, LocalDate ate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Compromisso c SET c.categoria = null WHERE c.categoria = :categoria")
    void limparCategoria(@Param("categoria") Categoria categoria);

}
