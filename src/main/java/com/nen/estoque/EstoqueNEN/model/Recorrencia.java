package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Lançamento que se repete todo mês (salário, aluguel, assinaturas).
 * Um agendador cria o lançamento do mês quando chega o dia.
 */
@Table(name = "recorrencia")
@Entity(name = "Recorrencia")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Recorrencia {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** 1 a 31. Em meses mais curtos, usa o último dia do mês. */
    @Column(name = "dia_do_mes", nullable = false)
    private int diaDoMes;

    @Column(nullable = false)
    private boolean ativa;

    /** Último mês (`yyyy-MM`) em que o lançamento foi gerado. */
    @Column(name = "ultimo_mes_gerado", length = 7)
    private String ultimoMesGerado;

    public LocalDate dataEm(YearMonth mes) {
        return mes.atDay(Math.min(diaDoMes, mes.lengthOfMonth()));
    }

    public boolean geradaEm(YearMonth mes) {
        return mes.toString().equals(ultimoMesGerado);
    }
}
