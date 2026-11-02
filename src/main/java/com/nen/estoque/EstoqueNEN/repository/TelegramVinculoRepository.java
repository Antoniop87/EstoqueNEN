package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.TelegramVinculo;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramVinculoRepository extends JpaRepository<TelegramVinculo, Long> {

    Optional<TelegramVinculo> findByUsuario(User usuario);

    Optional<TelegramVinculo> findByChatId(Long chatId);

    Optional<TelegramVinculo> findByCodigo(String codigo);

}
