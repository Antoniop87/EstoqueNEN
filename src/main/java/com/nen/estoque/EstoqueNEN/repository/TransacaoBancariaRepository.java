package com.nen.estoque.EstoqueNEN.repository;

import com.nen.estoque.EstoqueNEN.model.BancoConexao;
import com.nen.estoque.EstoqueNEN.model.TransacaoBancaria;
import com.nen.estoque.EstoqueNEN.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TransacaoBancariaRepository extends JpaRepository<TransacaoBancaria, Long> {

    List<TransacaoBancaria> findAllByUsuarioAndSituacaoOrderByDataDescIdDesc(User usuario, TransacaoBancaria.Situacao situacao);

    Optional<TransacaoBancaria> findByIdAndUsuario(Long id, User usuario);

    long countByUsuarioAndSituacao(User usuario, TransacaoBancaria.Situacao situacao);

    @Query("SELECT t.pluggyId FROM TransacaoBancaria t WHERE t.pluggyId IN :ids")
    Set<String> idsExistentes(@Param("ids") Collection<String> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TransacaoBancaria t WHERE t.conexao = :conexao")
    void excluirDaConexao(@Param("conexao") BancoConexao conexao);
}
