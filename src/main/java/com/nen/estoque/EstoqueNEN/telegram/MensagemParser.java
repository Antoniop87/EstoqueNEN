package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta mensagens em português livre, por exemplo:
 * <ul>
 *   <li>"gastei 45,90 com almoço no Restaurante Sabor"</li>
 *   <li>"recebi 5.200 de salário da Empresa XPTO"</li>
 *   <li>"uber 32,40 ontem" (sem palavra-chave = despesa)</li>
 *   <li>"+1350,50 freelance 18/10" (sinal define o tipo)</li>
 * </ul>
 * Regras, em ordem: palavra-chave inicial ou sinal define o tipo; o primeiro número é o valor
 * (palavras como "reais" são descartadas); "hoje", "ontem", "anteontem" ou dd/mm[/aaaa] é a data;
 * conectores e artigos no começo do que sobra ("com", "de", "uma"...) são descartados; o texto
 * restante vira descrição, e o trecho após "no/na/em" (despesa) ou "do/da" (receita) vira
 * estabelecimento/origem. Ex.: "gastei 20 reais hoje com lazer" → descrição "Lazer".
 * Em despesas, "no crédito", "no cartão", "no débito", "no pix" ou "em dinheiro" definem a forma
 * de pagamento e saem do texto. Só a primeira palavra decide o tipo, então "crédito" no começo
 * ainda é entrada ("crédito 50 reembolso").
 */
@Component
public class MensagemParser {

    private static final Set<String> PALAVRAS_DESPESA = Set.of(
            "gastei", "gasto", "gastos", "paguei", "pago", "comprei", "compra", "despesa", "saida", "debito");
    private static final Set<String> PALAVRAS_RECEITA = Set.of(
            "recebi", "recebido", "ganhei", "entrada", "entrou", "receita", "credito");
    /** Indicam receita quando não há palavra-chave nem sinal. */
    private static final Set<String> PISTAS_RECEITA = Set.of("salario", "freela", "freelance", "rendimento");

    /** Palavras de moeda que acompanham o valor ("20 reais") e não fazem parte da descrição. */
    private static final Set<String> PALAVRAS_MOEDA = Set.of(
            "reais", "real", "r$", "rs", "brl", "conto", "contos", "pila", "pilas");

    /**
     * Conectores e artigos que só introduzem a descrição ("com lazer", "de salário", "uma pizza")
     * e são descartados quando aparecem no começo do texto restante.
     */
    private static final Set<String> CONECTORES_INICIAIS = Set.of(
            "de", "com", "em", "pra", "pro", "para", "por", "pelo", "pela", "no", "na",
            "num", "numa", "um", "uma", "uns", "umas", "o", "a", "os", "as");
    private static final Set<String> CONECTORES_DETALHE_DESPESA = Set.of("no", "na", "nos", "nas", "em", "num", "numa");
    /** No começo do texto, estes introduzem a descrição ("em gasolina"), não o estabelecimento. */
    private static final Set<String> CONECTORES_SO_DE_DESCRICAO_NO_INICIO = Set.of("em", "num", "numa");
    private static final Set<String> CONECTORES_DETALHE_RECEITA = Set.of("do", "da", "dos", "das", "pelo", "pela");
    private static final Set<String> PREFIXOS_DATA = Set.of("em", "dia", "no");
    /** Introduzem a forma de pagamento ("no crédito", "via pix", "em dinheiro") e saem junto com ela. */
    private static final Set<String> PREFIXOS_PAGAMENTO = Set.of("no", "na", "em", "via", "pelo", "pela", "com", "de");

    private static final Pattern VALOR = Pattern.compile(
            "^([+-])?(?:r\\$)?(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)$");
    private static final Pattern DATA = Pattern.compile("^(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?$");

    private static final int TAMANHO_MAXIMO = 100;

    public LancamentoInterpretado interpretar(String mensagem, LocalDate hoje) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new MensagemNaoEntendidaException("A mensagem está vazia.");
        }

        List<String> tokens = new ArrayList<>(Arrays.asList(mensagem.trim().split("\\s+")));
        TipoLancamento tipo = extrairTipoPorPalavraChave(tokens);

        Valor valor = extrairValor(tokens);
        if (tipo == null) {
            tipo = valor.sinal() != null ? valor.sinal() : inferirTipo(tokens);
        }

        tokens.removeIf(token -> PALAVRAS_MOEDA.contains(normalizar(token)));

        LocalDate data = extrairData(tokens, hoje);
        FormaPagamento forma = tipo == TipoLancamento.DESPESA ? extrairFormaPagamento(tokens) : null;
        return montar(tipo, valor.quantia(), tokens, data, forma);
    }

    // ---------- Tipo ----------

    private TipoLancamento extrairTipoPorPalavraChave(List<String> tokens) {
        String primeira = normalizar(tokens.get(0));
        if (PALAVRAS_DESPESA.contains(primeira)) {
            tokens.remove(0);
            return TipoLancamento.DESPESA;
        }
        if (PALAVRAS_RECEITA.contains(primeira)) {
            tokens.remove(0);
            return TipoLancamento.RECEITA;
        }
        return null;
    }

    private TipoLancamento inferirTipo(List<String> tokens) {
        boolean pareceReceita = tokens.stream().map(MensagemParser::normalizar).anyMatch(PISTAS_RECEITA::contains);
        return pareceReceita ? TipoLancamento.RECEITA : TipoLancamento.DESPESA;
    }

    // ---------- Valor ----------

    private record Valor(BigDecimal quantia, TipoLancamento sinal) {
    }

    private Valor extrairValor(List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            // Pontuação no fim ("150.") não faz parte do número.
            String token = tokens.get(i).toLowerCase(Locale.ROOT).replaceAll("[.,!?;:]+$", "");
            // "R$ 45,90" separado em dois tokens
            if (token.equals("r$") && i + 1 < tokens.size()) {
                continue;
            }
            Matcher matcher = VALOR.matcher(token);
            if (!matcher.matches()) {
                continue;
            }

            BigDecimal quantia = paraBigDecimal(matcher.group(2));
            tokens.remove(i);

            // "2 mil" = 2000
            if (i < tokens.size() && normalizar(tokens.get(i)).equals("mil")) {
                quantia = quantia.multiply(BigDecimal.valueOf(1000));
                tokens.remove(i);
            }
            if (quantia.signum() <= 0) {
                throw new MensagemNaoEntendidaException("O valor precisa ser maior que zero.");
            }

            if (i > 0 && tokens.get(i - 1).equalsIgnoreCase("r$")) {
                tokens.remove(i - 1);
            }

            TipoLancamento sinal = switch (matcher.group(1) == null ? "" : matcher.group(1)) {
                case "+" -> TipoLancamento.RECEITA;
                case "-" -> TipoLancamento.DESPESA;
                default -> null;
            };
            return new Valor(quantia, sinal);
        }
        throw new MensagemNaoEntendidaException("Não encontrei o valor na mensagem.");
    }

    /** Aceita "1.234,56", "1234,56", "1234.56", "1.234" (milhar) e "45,9". */
    static BigDecimal paraBigDecimal(String texto) {
        String normalizado;
        if (texto.contains(",")) {
            normalizado = texto.replace(".", "").replace(',', '.');
        } else if (texto.matches("\\d{1,3}(\\.\\d{3})+")) {
            normalizado = texto.replace(".", "");
        } else {
            normalizado = texto;
        }
        return new BigDecimal(normalizado).setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- Data ----------

    private LocalDate extrairData(List<String> tokens, LocalDate hoje) {
        for (int i = 0; i < tokens.size(); i++) {
            LocalDate data = lerData(normalizar(tokens.get(i)), hoje);
            if (data == null) {
                continue;
            }
            tokens.remove(i);
            // "em 05/10", "no dia 05/10"
            while (i > 0 && PREFIXOS_DATA.contains(normalizar(tokens.get(i - 1)))) {
                tokens.remove(--i);
            }
            return data;
        }
        return hoje;
    }

    private LocalDate lerData(String token, LocalDate hoje) {
        switch (token) {
            case "hoje":
                return hoje;
            case "ontem":
                return hoje.minusDays(1);
            case "anteontem":
                return hoje.minusDays(2);
            default:
                break;
        }

        Matcher matcher = DATA.matcher(token);
        if (!matcher.matches()) {
            return null;
        }
        int dia = Integer.parseInt(matcher.group(1));
        int mes = Integer.parseInt(matcher.group(2));
        try {
            if (matcher.group(3) != null) {
                int ano = Integer.parseInt(matcher.group(3));
                return LocalDate.of(ano < 100 ? 2000 + ano : ano, mes, dia);
            }
            // Sem ano: a data mais recente que não esteja no futuro.
            LocalDate data = LocalDate.of(hoje.getYear(), mes, dia);
            return data.isAfter(hoje) ? data.minusYears(1) : data;
        } catch (DateTimeException e) {
            throw new MensagemNaoEntendidaException("A data " + token + " não existe.");
        }
    }

    // ---------- Forma de pagamento ----------

    private FormaPagamento extrairFormaPagamento(List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            String palavra = normalizar(tokens.get(i));
            FormaPagamento forma;
            int fim = i + 1;
            switch (palavra) {
                case "cartao" -> {
                    // "cartão", "cartão de crédito", "cartão débito"...
                    int j = i + 1;
                    if (j < tokens.size() && normalizar(tokens.get(j)).equals("de")) {
                        j++;
                    }
                    String tipoCartao = j < tokens.size() ? normalizar(tokens.get(j)) : "";
                    if (tipoCartao.equals("credito") || tipoCartao.equals("debito")) {
                        fim = j + 1;
                        forma = tipoCartao.equals("debito") ? FormaPagamento.DEBITO : FormaPagamento.CREDITO;
                    } else {
                        forma = FormaPagamento.CREDITO;
                    }
                }
                case "credito" -> forma = FormaPagamento.CREDITO;
                case "debito" -> forma = FormaPagamento.DEBITO;
                case "pix" -> forma = FormaPagamento.PIX;
                case "dinheiro", "especie" -> forma = FormaPagamento.DINHEIRO;
                default -> forma = null;
            }
            if (forma == null) {
                continue;
            }
            int inicio = i;
            while (inicio > 0 && PREFIXOS_PAGAMENTO.contains(normalizar(tokens.get(inicio - 1)))) {
                inicio--;
            }
            tokens.subList(inicio, fim).clear();
            return forma;
        }
        return null;
    }

    // ---------- Descrição e detalhe ----------

    private LancamentoInterpretado montar(TipoLancamento tipo, BigDecimal valor, List<String> tokens, LocalDate data,
                                          FormaPagamento forma) {
        // Descarta conectores que só introduzem a descrição ("de salário", "com almoço"),
        // mas mantém os que já introduzem o detalhe ("no mercado").
        while (!tokens.isEmpty()) {
            String primeiro = normalizar(tokens.get(0));
            boolean introduzDetalhe = conectoresDetalhe(tipo).contains(primeiro)
                    && !CONECTORES_SO_DE_DESCRICAO_NO_INICIO.contains(primeiro);
            if (!CONECTORES_INICIAIS.contains(primeiro) || introduzDetalhe) {
                break;
            }
            tokens.remove(0);
        }

        int corte = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (conectoresDetalhe(tipo).contains(normalizar(tokens.get(i))) && i + 1 < tokens.size()) {
                corte = i;
                break;
            }
        }

        String descricao = corte < 0 ? juntar(tokens, 0, tokens.size()) : juntar(tokens, 0, corte);
        String detalhe = corte < 0 ? null : juntar(tokens, corte + 1, tokens.size());

        if (descricao == null) {
            descricao = detalhe != null ? detalhe : (tipo == TipoLancamento.RECEITA ? "Entrada" : "Despesa");
        }
        return new LancamentoInterpretado(tipo, valor, limitar(descricao), limitar(detalhe), data, forma);
    }

    private static Set<String> conectoresDetalhe(TipoLancamento tipo) {
        return tipo == TipoLancamento.RECEITA ? CONECTORES_DETALHE_RECEITA : CONECTORES_DETALHE_DESPESA;
    }

    private static String juntar(List<String> tokens, int de, int ate) {
        // Conectores que sobram soltos no fim ("tênis por", depois de tirar o valor) são descartados.
        while (ate > de && CONECTORES_INICIAIS.contains(normalizar(tokens.get(ate - 1)))) {
            ate--;
        }
        String texto = String.join(" ", tokens.subList(de, ate)).trim().replaceAll("[.,!?;:]+$", "");
        if (texto.isEmpty()) {
            return null;
        }
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String limitar(String texto) {
        return texto == null || texto.length() <= TAMANHO_MAXIMO ? texto : texto.substring(0, TAMANHO_MAXIMO);
    }

    /** Minúsculas e sem acento, para comparar palavras-chave. */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.,!?;:]+$", "");
    }
}
