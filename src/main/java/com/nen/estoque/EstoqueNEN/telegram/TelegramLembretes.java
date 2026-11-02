package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.Compromisso;
import com.nen.estoque.EstoqueNEN.model.TelegramVinculo;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.TelegramVinculoRepository;
import com.nen.estoque.EstoqueNEN.service.CompromissoService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Lembretes de contas a pagar/receber no Telegram: todo dia de manhã, para quem tem o chat
 * vinculado, avisa o que vence hoje, amanhã ou já está atrasado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramLembretes {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");
    /** Janela do comando /contas. */
    private static final int DIAS_COMANDO = 7;

    private final TelegramVinculoRepository vinculoRepository;
    private final CompromissoService compromissoService;
    /** O bot só existe quando há token; e ele depende deste componente, por isso o acesso é tardio. */
    private final ObjectProvider<NickFinanceBot> bot;

    @Scheduled(cron = "0 0 9 * * *", zone = "America/Sao_Paulo")
    @Transactional(readOnly = true)
    public void enviarLembretesDoDia() {
        NickFinanceBot instancia = bot.getIfAvailable();
        if (instancia == null) {
            return;
        }
        for (TelegramVinculo vinculo : vinculoRepository.findAll()) {
            if (!vinculo.isVinculado()) {
                continue;
            }
            List<Compromisso> contas = compromissoService.emAbertoAte(vinculo.getUsuario(), Datas.hoje().plusDays(1));
            if (!contas.isEmpty()) {
                instancia.enviarAviso(vinculo.getChatId(), "🔔 <b>Lembrete de contas</b>\n\n" + listar(contas));
            }
        }
    }

    /**
     * Texto do comando /contas: contas em aberto até daqui a {@value #DIAS_COMANDO} dias
     * (ou só hoje/amanhã quando `semana` é falso).
     */
    @Transactional(readOnly = true)
    public String mensagemContas(User usuario, boolean semana) {
        LocalDate ate = Datas.hoje().plusDays(semana ? DIAS_COMANDO : 1);
        List<Compromisso> contas = compromissoService.emAbertoAte(usuario, ate);
        if (contas.isEmpty()) {
            return "✅ Nenhuma conta em aberto para os próximos " + DIAS_COMANDO + " dias.";
        }
        return "📋 <b>Contas dos próximos " + DIAS_COMANDO + " dias</b>\n\n" + listar(contas);
    }

    static String listar(List<Compromisso> contas) {
        LocalDate hoje = Datas.hoje();
        StringBuilder texto = new StringBuilder();
        for (Compromisso c : contas) {
            boolean pagar = c.getTipo() == TipoLancamento.DESPESA;
            texto.append(pagar ? "💸 " : "💰 ")
                    .append(c.getDescricao().replace("<", "&lt;"))
                    .append(" — ").append(NumberFormat.getCurrencyInstance(PT_BR).format(c.getValor()))
                    .append(" · ").append(quando(c.getVencimento(), hoje))
                    .append('\n');
        }
        texto.append("\nMarque como paga no app, em <b>Contas</b>.");
        return texto.toString();
    }

    private static String quando(LocalDate vencimento, LocalDate hoje) {
        if (vencimento.isBefore(hoje)) {
            return "<b>atrasada desde " + vencimento.format(DATA) + "</b>";
        }
        if (vencimento.equals(hoje)) {
            return "<b>vence hoje</b>";
        }
        if (vencimento.equals(hoje.plusDays(1))) {
            return "vence amanhã";
        }
        return "vence " + vencimento.format(DATA);
    }
}
