package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada de dinheiro do usuário (salário, freelance, venda, etc.).
 */
@Table(name = "receita")
@Entity(name = "Receita")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(length = 100)
    private String origem;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
