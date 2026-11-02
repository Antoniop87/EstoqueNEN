package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado da leitura de uma mensagem do Telegram.
 *
 * @param detalhe estabelecimento (despesa) ou origem (receita); pode ser null
 */
public record LancamentoInterpretado(

        TipoLancamento tipo,
        BigDecimal valor,
        String descricao,
        String detalhe,
        LocalDate data

) {
}
