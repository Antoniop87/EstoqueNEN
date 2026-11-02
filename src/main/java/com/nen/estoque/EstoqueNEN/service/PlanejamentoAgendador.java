package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gera os lançamentos recorrentes do dia. Roda logo após a meia-noite e também
 * quando a API sobe (para recuperar dias em que o servidor estava dormindo).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanejamentoAgendador {

    private final RecorrenciaService recorrenciaService;

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        gerarRecorrencias();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void gerarRecorrencias() {
        int gerados = recorrenciaService.gerarPendentes(Datas.hoje());
        if (gerados > 0) {
            log.info("{} lançamento(s) recorrente(s) gerado(s).", gerados);
        }
    }
}
