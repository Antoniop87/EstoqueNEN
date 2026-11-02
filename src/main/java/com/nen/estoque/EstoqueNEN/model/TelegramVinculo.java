package com.nen.estoque.EstoqueNEN.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Liga uma conta do Nick Finance a um chat do Telegram.
 * Enquanto não há `chatId`, guarda o código temporário usado para fazer a ligação.
 */
@Table(name = "telegram_vinculo")
@Entity(name = "TelegramVinculo")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TelegramVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User usuario;

    @Column(name = "chat_id", unique = true)
    private Long chatId;

    @Column(length = 16, unique = true)
    private String codigo;

    @Column(name = "codigo_expira_em")
    private Instant codigoExpiraEm;

    @Column(name = "vinculado_em")
    private Instant vinculadoEm;

    public boolean isVinculado() {
        return chatId != null;
    }
}
