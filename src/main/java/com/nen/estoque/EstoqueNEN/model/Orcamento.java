package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Limite mensal de gastos para uma categoria de despesa. */
@Table(name = "orcamento", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "categoria_id"}))
@Entity(name = "Orcamento")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "valor_limite", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLimite;
}
