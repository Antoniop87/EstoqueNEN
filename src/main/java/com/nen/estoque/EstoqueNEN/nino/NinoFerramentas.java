package com.nen.estoque.EstoqueNEN.nino;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nen.estoque.EstoqueNEN.dto.*;
import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.nino.ModeloLinguagem.Ferramenta;
import com.nen.estoque.EstoqueNEN.service.*;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * O que o Nino pode consultar. Tudo é somente leitura e passa pelos services de
 * "usuário logado", então o modelo nunca escolhe de quem são os dados. Nome e e-mail
 * do usuário não são enviados ao provedor.
 */
@Component
@RequiredArgsConstructor
class NinoFerramentas {

    private static final int LIMITE_PADRAO = 15;
    private static final int LIMITE_MAXIMO = 30;

    private static final Map<String, Object> PROP_MES = Map.of(
            "type", "string", "description", "Mês no formato AAAA-MM. Sem informar, usa o mês atual.");

    private final DespesaService despesas;
    private final ReceitaService receitas;
    private final CategoriaService categorias;
    private final OrcamentoService orcamentos;
    private final CompromissoService contas;
    private final RecorrenciaService recorrencias;
    private final MetaService metas;
    private final ObjectMapper json;

    private record Lancamento(LocalDate data, TipoLancamento tipo, String descricao, String local,
                              BigDecimal valor, Long categoriaId, FormaPagamento forma) {
    }

    List<Ferramenta> definicoes() {
        return List.of(
                new Ferramenta("resumo_do_mes",
                        "Saldo total acumulado e, no mês: entradas, saídas, saldo e divisão das saídas entre cartão de crédito, à vista (débito, Pix, dinheiro) e não informado.",
                        objeto(Map.of("mes", PROP_MES))),
                new Ferramenta("gastos_por_categoria",
                        "Total de despesas por categoria no mês, do maior para o menor.",
                        objeto(Map.of("mes", PROP_MES))),
                new Ferramenta("listar_lancamentos",
                        "Lista entradas e despesas do mês, das mais recentes para as mais antigas, com filtros opcionais.",
                        objeto(Map.of(
                                "mes", PROP_MES,
                                "tipo", Map.of("type", "string", "enum", List.of("RECEITA", "DESPESA")),
                                "busca", Map.of("type", "string", "description", "Trecho da descrição, do local ou da categoria."),
                                "limite", Map.of("type", "integer", "description", "Máximo de itens, até " + LIMITE_MAXIMO + ".")))),
                new Ferramenta("orcamentos_do_mes",
                        "Limites mensais por categoria e quanto já foi gasto no mês atual.",
                        objeto(Map.of())),
                new Ferramenta("contas_em_aberto",
                        "Contas a pagar e a receber ainda não quitadas, por vencimento.",
                        objeto(Map.of())),
                new Ferramenta("recorrentes",
                        "Lançamentos que se repetem todo mês.",
                        objeto(Map.of())),
                new Ferramenta("metas",
                        "Metas de economia: valor alvo, valor guardado e prazo.",
                        objeto(Map.of())));
    }

    private static Map<String, Object> objeto(Map<String, Object> propriedades) {
        return Map.of("type", "object", "properties", propriedades);
    }

    /** Executa a ferramenta. Erros voltam como JSON, para o modelo poder se corrigir. */
    String executar(String nome, String argumentos) {
        try {
            JsonNode args = lerArgumentos(argumentos);
            Object resultado = switch (nome) {
                case "resumo_do_mes" -> resumoDoMes(mes(args));
                case "gastos_por_categoria" -> gastosPorCategoria(mes(args));
                case "listar_lancamentos" -> listarLancamentos(mes(args), args);
                case "orcamentos_do_mes" -> orcamentosDoMes();
                case "contas_em_aberto" -> contasEmAberto();
                case "recorrentes" -> recorrentes();
                case "metas" -> metas();
                default -> throw new IllegalArgumentException("Ferramenta desconhecida.");
            };
            return json.writeValueAsString(resultado);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return "{\"erro\":\"Argumentos inválidos para " + nome.replaceAll("\\W", "") + ".\"}";
        }
    }

    private JsonNode lerArgumentos(String argumentos) throws JsonProcessingException {
        return argumentos == null || argumentos.isBlank() ? json.createObjectNode() : json.readTree(argumentos);
    }

    private static YearMonth mes(JsonNode args) {
        String texto = args.path("mes").asText("");
        if (texto.isBlank()) {
            return YearMonth.from(Datas.hoje());
        }
        try {
            return YearMonth.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mês inválido.");
        }
    }

    // ---------- Consultas ----------

    private Map<String, Object> resumoDoMes(YearMonth mes) {
        List<Lancamento> todos = todosLancamentos();
        List<Lancamento> doMes = todos.stream().filter(l -> YearMonth.from(l.data()).equals(mes)).toList();
        List<Lancamento> saidas = doTipo(doMes, TipoLancamento.DESPESA);

        BigDecimal totalEntradas = soma(doTipo(doMes, TipoLancamento.RECEITA));
        BigDecimal totalSaidas = soma(saidas);
        BigDecimal credito = soma(saidas.stream().filter(l -> l.forma() == FormaPagamento.CREDITO).toList());
        BigDecimal naoInformado = soma(saidas.stream().filter(l -> l.forma() == null).toList());

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("mes", mes.toString());
        resumo.put("saldoTotalAcumulado", soma(doTipo(todos, TipoLancamento.RECEITA)).subtract(soma(doTipo(todos, TipoLancamento.DESPESA))));
        resumo.put("entradas", totalEntradas);
        resumo.put("saidas", totalSaidas);
        resumo.put("saldoDoMes", totalEntradas.subtract(totalSaidas));
        resumo.put("saidasNoCredito", credito);
        resumo.put("saidasAVista", totalSaidas.subtract(credito).subtract(naoInformado));
        resumo.put("saidasSemFormaInformada", naoInformado);
        resumo.put("quantidadeDeLancamentos", doMes.size());
        return resumo;
    }

    private List<Map<String, Object>> gastosPorCategoria(YearMonth mes) {
        Map<Long, String> nomes = nomesDeCategoria();
        List<Lancamento> saidas = doTipo(todosLancamentos(), TipoLancamento.DESPESA).stream()
                .filter(l -> YearMonth.from(l.data()).equals(mes)).toList();
        BigDecimal total = soma(saidas);

        return saidas.stream()
                .collect(Collectors.groupingBy(l -> nomes.getOrDefault(l.categoriaId(), "Sem categoria")))
                .entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "categoria", e.getKey(),
                        "total", soma(e.getValue()),
                        "percentual", percentual(soma(e.getValue()), total)))
                .sorted(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("total")).reversed())
                .toList();
    }

    private Map<String, Object> listarLancamentos(YearMonth mes, JsonNode args) {
        Map<Long, String> nomes = nomesDeCategoria();
        TipoLancamento tipo = args.hasNonNull("tipo") && !args.path("tipo").asText().isBlank()
                ? TipoLancamento.valueOf(args.path("tipo").asText())
                : null;
        String busca = args.path("busca").asText("").toLowerCase(Locale.ROOT).trim();
        int limite = Math.min(Math.max(args.path("limite").asInt(LIMITE_PADRAO), 1), LIMITE_MAXIMO);

        List<Lancamento> encontrados = todosLancamentos().stream()
                .filter(l -> YearMonth.from(l.data()).equals(mes))
                .filter(l -> tipo == null || l.tipo() == tipo)
                .filter(l -> busca.isEmpty() || contem(l, nomes.get(l.categoriaId()), busca))
                .sorted(Comparator.comparing(Lancamento::data).reversed())
                .toList();

        List<Map<String, Object>> itens = encontrados.stream().limit(limite).map(l -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("data", l.data());
            item.put("tipo", l.tipo());
            item.put("descricao", l.descricao());
            item.put("local", l.local());
            item.put("valor", l.valor());
            item.put("categoria", nomes.getOrDefault(l.categoriaId(), "Sem categoria"));
            item.put("formaPagamento", l.forma() == null ? null : l.forma().getRotulo());
            return item;
        }).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalEncontrado", encontrados.size());
        resultado.put("somaDoTotalEncontrado", soma(encontrados));
        resultado.put("itensMostrados", itens);
        return resultado;
    }

    private List<Map<String, Object>> orcamentosDoMes() {
        Map<Long, String> nomes = nomesDeCategoria();
        YearMonth mes = YearMonth.from(Datas.hoje());
        Map<Long, List<Lancamento>> porCategoria = doTipo(todosLancamentos(), TipoLancamento.DESPESA).stream()
                .filter(l -> l.categoriaId() != null && YearMonth.from(l.data()).equals(mes))
                .collect(Collectors.groupingBy(Lancamento::categoriaId));

        return orcamentos.listarDoUsuarioLogado().stream().map(o -> {
            BigDecimal gasto = soma(porCategoria.getOrDefault(o.categoriaId(), List.of()));
            return Map.<String, Object>of(
                    "categoria", nomes.getOrDefault(o.categoriaId(), "Sem categoria"),
                    "limite", o.valorLimite(),
                    "gasto", gasto,
                    "restante", o.valorLimite().subtract(gasto),
                    "percentualUsado", percentual(gasto, o.valorLimite()));
        }).toList();
    }

    private List<Map<String, Object>> contasEmAberto() {
        LocalDate hoje = Datas.hoje();
        return contas.listarDoUsuarioLogado().stream()
                .filter(c -> c.pagoEm() == null)
                .sorted(Comparator.comparing(CompromissoDto.Response::vencimento))
                .map(c -> Map.<String, Object>of(
                        "descricao", c.descricao(),
                        "tipo", c.tipo() == TipoLancamento.DESPESA ? "A pagar" : "A receber",
                        "valor", c.valor(),
                        "vencimento", c.vencimento(),
                        "atrasada", c.vencimento().isBefore(hoje)))
                .toList();
    }

    private List<Map<String, Object>> recorrentes() {
        return recorrencias.listarDoUsuarioLogado().stream()
                .map(r -> Map.<String, Object>of(
                        "descricao", r.descricao(),
                        "tipo", r.tipo(),
                        "valor", r.valor(),
                        "diaDoMes", r.diaDoMes(),
                        "ativa", r.ativa()))
                .toList();
    }

    private List<Map<String, Object>> metas() {
        return metas.listarDoUsuarioLogado().stream().map(m -> {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("nome", m.nome());
            meta.put("valorAlvo", m.valorAlvo());
            meta.put("valorGuardado", m.valorGuardado());
            meta.put("prazo", m.prazo());
            return meta;
        }).toList();
    }

    // ---------- Apoio ----------

    private List<Lancamento> todosLancamentos() {
        List<Lancamento> todos = new ArrayList<>();
        for (DespesaDto.Response d : despesas.listarDoUsuarioLogado()) {
            todos.add(new Lancamento(d.data(), TipoLancamento.DESPESA, d.descricao(), d.estabelecimento(),
                    d.valor(), d.categoriaId(), d.formaPagamento()));
        }
        for (ReceitaDto.Response r : receitas.listarDoUsuarioLogado()) {
            todos.add(new Lancamento(r.data(), TipoLancamento.RECEITA, r.descricao(), r.origem(),
                    r.valor(), r.categoriaId(), null));
        }
        return todos;
    }

    private Map<Long, String> nomesDeCategoria() {
        return categorias.listarDoUsuarioLogado().stream()
                .collect(Collectors.toMap(CategoriaDto.Response::id, CategoriaDto.Response::nome, (a, b) -> a));
    }

    private static List<Lancamento> doTipo(List<Lancamento> lancamentos, TipoLancamento tipo) {
        return lancamentos.stream().filter(l -> l.tipo() == tipo).toList();
    }

    private static BigDecimal soma(List<Lancamento> lancamentos) {
        return lancamentos.stream().map(Lancamento::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return 0;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    private static boolean contem(Lancamento l, String categoria, String busca) {
        return Stream.of(l.descricao(), l.local(), categoria)
                .anyMatch(texto -> texto != null && texto.toLowerCase(Locale.ROOT).contains(busca));
    }
}
