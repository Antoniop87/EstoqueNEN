package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.NinoUso;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface NinoUsoRepository extends JpaRepository<NinoUso, Long> {

    Optional<NinoUso> findByUsuarioAndDia(User usuario, LocalDate dia);
}
