package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Objetivo de economia (viagem, reserva de emergência...). */
@Table(name = "meta")
@Entity(name = "Meta")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "valor_alvo", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorAlvo;

    @Column(name = "valor_guardado", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorGuardado;

    private LocalDate prazo;

    @Column(nullable = false, length = 40)
    private String icone;

    @Column(nullable = false, length = 9)
    private String cor;

    @Column(name = "criada_em", nullable = false)
    private LocalDate criadaEm;
}
