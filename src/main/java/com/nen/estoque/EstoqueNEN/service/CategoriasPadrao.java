package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.model.TipoLancamento;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Categorias criadas para todo usuário novo e as palavras usadas para sugerir uma
 * categoria a partir do texto do lançamento (usado pelo bot do Telegram).
 */
public final class CategoriasPadrao {

    public record Modelo(String nome, String icone, String cor, TipoLancamento tipo, Set<String> palavras) {
    }

    public static final String OUTROS = "Outros";

    public static final List<Modelo> TODAS = List.of(
            despesa("Alimentação", "utensils", "#f97316", "mercado", "supermercado", "padaria", "almoco", "almoço",
                    "jantar", "lanche", "restaurante", "ifood", "pizza", "cafe", "café", "acougue", "açougue",
                    "feira", "hamburguer", "comida", "marmita", "sorvete"),
            despesa("Transporte", "car", "#3b82f6", "uber", "99", "taxi", "táxi", "gasolina", "combustivel",
                    "combustível", "posto", "onibus", "ônibus", "metro", "metrô", "estacionamento", "pedagio",
                    "pedágio", "passagem", "etanol", "oficina", "ipva"),
            despesa("Moradia", "home", "#8b5cf6", "aluguel", "condominio", "condomínio", "luz", "energia", "agua",
                    "água", "internet", "gas", "gás", "iptu", "reforma", "moveis", "móveis"),
            despesa("Saúde", "heartPulse", "#ef4444", "farmacia", "farmácia", "remedio", "remédio", "medico",
                    "médico", "consulta", "exame", "dentista", "academia", "plano", "hospital", "terapia"),
            despesa("Educação", "book", "#0ea5e9", "curso", "livro", "faculdade", "escola", "mensalidade", "material",
                    "aula", "udemy"),
            despesa("Lazer", "gamepad", "#ec4899", "lazer", "cinema", "show", "bar", "balada", "jogo", "festa",
                    "passeio", "teatro", "cerveja"),
            despesa("Compras", "shoppingBag", "#f59e0b", "roupa", "roupas", "tenis", "tênis", "shopping", "loja",
                    "presente", "amazon", "shopee", "mercadolivre", "eletronico", "eletrônico", "celular"),
            despesa("Assinaturas", "tv", "#6366f1", "netflix", "spotify", "assinatura", "prime", "disney", "hbo",
                    "youtube", "icloud", "streaming"),
            despesa("Viagem", "plane", "#14b8a6", "viagem", "hotel", "airbnb", "voo", "pousada", "hospedagem"),
            despesa(OUTROS, "dots", "#64748b"),
            receita("Salário", "briefcase", "#10b981", "salario", "salário", "pagamento", "holerite",
                    "adiantamento", "13", "decimo", "décimo", "ferias", "férias", "bonus", "bônus", "plr"),
            receita("Freelance", "laptop", "#06b6d4", "freela", "freelance", "projeto", "cliente", "job", "servico",
                    "serviço"),
            receita("Investimentos", "trendingUp", "#22c55e", "rendimento", "rendimentos", "dividendo",
                    "dividendos", "juros", "investimento", "cdb", "tesouro", "acoes", "ações"),
            receita("Vendas", "tag", "#84cc16", "venda", "vendi", "vendas"),
            receita("Presentes", "gift", "#f43f5e", "presente", "presentes", "aniversario", "aniversário"),
            receita(OUTROS, "dots", "#64748b")
    );

    private CategoriasPadrao() {
    }

    /** Nome da categoria padrão que combina com o texto, se alguma palavra-chave aparecer. */
    public static Optional<String> sugerir(TipoLancamento tipo, String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        // HashSet: o texto pode repetir palavras ("Mercado" na descrição e no estabelecimento).
        Set<String> palavras = new HashSet<>(Arrays.asList(normalizar(texto).split("[^\\p{L}\\p{N}]+")));
        return TODAS.stream()
                .filter(modelo -> modelo.tipo() == tipo)
                .filter(modelo -> modelo.palavras().stream().map(CategoriasPadrao::normalizar).anyMatch(palavras::contains))
                .map(Modelo::nome)
                .findFirst();
    }

    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static Modelo despesa(String nome, String icone, String cor, String... palavras) {
        return new Modelo(nome, icone, cor, TipoLancamento.DESPESA, Set.of(palavras));
    }

    private static Modelo receita(String nome, String icone, String cor, String... palavras) {
        return new Modelo(nome, icone, cor, TipoLancamento.RECEITA, Set.of(palavras));
    }
}
