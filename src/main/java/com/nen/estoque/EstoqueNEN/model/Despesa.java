package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Table(name = "Despesa")
@Entity(name = "Despesa")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeDespesa;

    private String estabelecimento;

    private BigDecimal valorDespesa;

    /** Gravada como meia-noite UTC; use {@link com.nen.estoque.EstoqueNEN.util.Datas} para converter. */
    private Date dataDespesa;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** Nulo nas despesas criadas antes deste campo existir. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FormaPagamento formaPagamento;
}
