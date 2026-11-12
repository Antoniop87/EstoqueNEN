package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transação que veio do banco e espera revisão. Só vira despesa ou entrada quando o
 * usuário importa: assim o pagamento da fatura e o que já foi lançado à mão não duplicam.
 */
@Entity
@Table(name = "transacao_bancaria")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TransacaoBancaria {

    public enum Situacao { PENDENTE, IMPORTADA, IGNORADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conexao_id", nullable = false)
    private BancoConexao conexao;

    /** Id da transação na Pluggy; evita importar a mesma duas vezes. */
    @Column(name = "pluggy_id", nullable = false, unique = true, length = 64)
    private String pluggyId;

    @Column(length = 100)
    private String conta;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FormaPagamento formaPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoriaSugerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Situacao situacao;

    /** Já existe um lançamento com o mesmo valor em data próxima. */
    private boolean possivelDuplicado;

    /** Lançamento criado ao importar. */
    private Long lancamentoId;
}
