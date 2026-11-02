package com.nen.estoque.EstoqueNEN.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/** Conversões de data usadas em toda a API. */
public final class Datas {

    /** Fuso usado para decidir o que é "hoje" (lembretes, recorrências, bot). */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Datas() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    /**
     * A coluna de data da despesa é um `java.util.Date`. As datas são gravadas como
     * meia-noite UTC, então a conversão de ida e volta preserva o dia.
     */
    public static Date paraDate(LocalDate data) {
        return data == null ? null : Date.from(data.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    public static LocalDate paraLocalDate(Date data) {
        return data == null ? null : data.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
