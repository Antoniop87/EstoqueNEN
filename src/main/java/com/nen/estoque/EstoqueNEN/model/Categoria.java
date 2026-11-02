package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Categoria de lançamento (Alimentação, Salário...). Cada usuário tem as suas;
 * as padrão são criadas no primeiro acesso.
 */
@Table(name = "categoria")
@Entity(name = "Categoria")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nome;

    /** Nome do ícone usado pelo front (ex.: "utensils"). */
    @Column(nullable = false, length = 40)
    private String icone;

    /** Cor em hexadecimal (ex.: "#f97316"). */
    @Column(nullable = false, length = 9)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;
}
