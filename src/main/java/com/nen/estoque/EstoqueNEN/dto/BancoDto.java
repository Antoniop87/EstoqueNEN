package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.model.BancoConexao;
import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.TransacaoBancaria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Integração bancária (Pluggy). */
public final class BancoDto {

    private BancoDto() {
    }

    /** @param incluirSandbox mostra os bancos de teste da Pluggy no widget */
    public record Status(boolean ativo, boolean incluirSandbox, List<Conexao> conexoes, long pendentes) {
    }

    public record Conexao(Long id, String nomeBanco, String status, Instant ultimaSincronizacao, String erro) {

        public static Conexao from(BancoConexao c) {
            return new Conexao(c.getId(), c.getNomeBanco(), c.getStatus(), c.getUltimaSincronizacao(), c.getErro());
        }
    }

    public record ConnectToken(String accessToken) {
    }

    public record NovaConexao(@NotBlank @Size(max = 64) String itemId) {
    }

    public record Transacao(Long id, LocalDate data, String descricao, String conta, BigDecimal valor,
                            TipoLancamento tipo, FormaPagamento formaPagamento, Long categoriaSugeridaId,
                            boolean possivelDuplicado) {

        public static Transacao from(TransacaoBancaria t) {
            return new Transacao(t.getId(), t.getData(), t.getDescricao(), t.getConta(), t.getValor(), t.getTipo(),
                    t.getFormaPagamento(),
                    t.getCategoriaSugerida() == null ? null : t.getCategoriaSugerida().getId(),
                    t.isPossivelDuplicado());
        }
    }

    /** @param categoriaId categoria escolhida na revisão; nula = sem categoria */
    public record Importacao(Long categoriaId) {
    }

    public record Resultado(int quantidade) {
    }
}
