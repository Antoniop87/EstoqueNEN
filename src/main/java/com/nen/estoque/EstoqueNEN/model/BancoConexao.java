package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Um banco conectado pela Pluggy (um "item" deles). Não guarda senha nem token do banco. */
@Entity
@Table(name = "banco_conexao")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class BancoConexao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    /** Id do item na Pluggy. */
    @Column(name = "item_id", nullable = false, unique = true, length = 64)
    private String itemId;

    @Column(length = 100)
    private String nomeBanco;

    /** Status do item na Pluggy (UPDATED, UPDATING, LOGIN_ERROR...). */
    @Column(length = 40)
    private String status;

    private Instant ultimaSincronizacao;

    @Column(length = 300)
    private String erro;

    private Instant criadaEm;
}
