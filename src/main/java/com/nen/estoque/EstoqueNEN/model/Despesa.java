package com.nen.estoque.EstoqueNEN.model;

import com.fasterxml.jackson.annotation.JsonSetter;
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

    private Date dataDespesa;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private User usuario;

    public User getUsuario() {
        return usuario;
    }

    @JsonSetter("usuario")
    public void setUsuarioById(Long usuarioId) {
        if (usuarioId != null) {
            this.usuario = new User();
            this.usuario.setId(usuarioId);
        } else {
            this.usuario = null;
        }
    }
}