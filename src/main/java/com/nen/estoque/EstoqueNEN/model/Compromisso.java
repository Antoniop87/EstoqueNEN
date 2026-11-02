package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conta a pagar (despesa) ou a receber (receita) com vencimento.
 * Ao ser marcada como paga, gera o lançamento correspondente.
 */
@Table(name = "compromisso")
@Entity(name = "Compromisso")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Compromisso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(length = 100)
    private String detalhe;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "pago_em")
    private LocalDate pagoEm;

    /** Despesa ou receita criada quando a conta foi paga. */
    @Column(name = "lancamento_id")
    private Long lancamentoId;

    public boolean isPago() {
        return pagoEm != null;
    }
}
