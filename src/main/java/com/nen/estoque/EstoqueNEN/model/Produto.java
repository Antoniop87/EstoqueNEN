package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Table(name = "Produto")
@Entity(name = "Produto")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String produtoNome;

    private int quantidade;

    private String marca;

    private BigDecimal valor;

}
