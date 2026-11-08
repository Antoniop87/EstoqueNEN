package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Quantas mensagens o usuário trocou com o Nino em um dia. As conversas não são gravadas. */
@Entity
@Table(name = "nino_uso", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "dia"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NinoUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private LocalDate dia;

    @Column(nullable = false)
    private int quantidade;
}
