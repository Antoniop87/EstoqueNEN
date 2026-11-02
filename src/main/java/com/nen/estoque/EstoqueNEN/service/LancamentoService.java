package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.model.*;
import com.nen.estoque.EstoqueNEN.repository.DespesaRepository;
import com.nen.estoque.EstoqueNEN.repository.ReceitaRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cria e remove despesas/receitas em nome de um usuário, para quem gera lançamentos
 * automaticamente (bot, recorrências, contas pagas). As telas usam os services específicos.
 */
@Service
@RequiredArgsConstructor
public class LancamentoService {

    private final DespesaRepository despesaRepository;
    private final ReceitaRepository receitaRepository;

    public record Novo(TipoLancamento tipo, String descricao, String detalhe, BigDecimal valor,
                       LocalDate data, Categoria categoria) {
    }

    /** @return id do lançamento criado */
    @Transactional
    public Long criar(User usuario, Novo novo) {
        if (novo.tipo() == TipoLancamento.RECEITA) {
            return receitaRepository.save(Receita.builder()
                    .descricao(novo.descricao())
                    .origem(novo.detalhe())
                    .valor(novo.valor())
                    .data(novo.data())
                    .categoria(novo.categoria())
                    .usuario(usuario)
                    .build()).getId();
        }
        return despesaRepository.save(Despesa.builder()
                .nomeDespesa(novo.descricao())
                .estabelecimento(novo.detalhe())
                .valorDespesa(novo.valor())
                .dataDespesa(Datas.paraDate(novo.data()))
                .categoria(novo.categoria())
                .usuario(usuario)
                .build()).getId();
    }

    /** Remove o lançamento se ele existir e pertencer ao usuário. */
    @Transactional
    public boolean excluir(User usuario, TipoLancamento tipo, Long id) {
        if (id == null) {
            return false;
        }
        if (tipo == TipoLancamento.RECEITA) {
            return receitaRepository.findByIdAndUsuario(id, usuario).map(r -> {
                receitaRepository.delete(r);
                return true;
            }).orElse(false);
        }
        return despesaRepository.findByIdAndUsuario(id, usuario).map(d -> {
            despesaRepository.delete(d);
            return true;
        }).orElse(false);
    }
}
