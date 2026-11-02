package com.nen.estoque.EstoqueNEN;

import com.nen.estoque.EstoqueNEN.dto.*;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.UserRepository;
import com.nen.estoque.EstoqueNEN.service.*;
import com.nen.estoque.EstoqueNEN.telegram.MensagemParser;
import com.nen.estoque.EstoqueNEN.telegram.TelegramLancamentoService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Categorias, edição/exclusão de lançamentos e planejamento contra o banco real.
 * Cada teste roda em transação revertida ao final. Requer o PostgreSQL local.
 */
@SpringBootTest
@Transactional
class PlanejamentoIntegracaoTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CategoriaService categoriaService;
    @Autowired private DespesaService despesaService;
    @Autowired private ReceitaService receitaService;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private RecorrenciaService recorrenciaService;
    @Autowired private CompromissoService compromissoService;
    @Autowired private MetaService metaService;
    @Autowired private TelegramLancamentoService telegramLancamentoService;
    @Autowired private MensagemParser parser;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = novoUsuario();
        autenticarComo(usuario);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void categoriasPadraoSaoCriadasEExclusaoDesvinculaLancamentos() {
        List<CategoriaDto.Response> categorias = categoriaService.listarDoUsuarioLogado();
        assertTrue(categorias.stream().anyMatch(c -> c.nome().equals("Alimentação") && c.tipo() == TipoLancamento.DESPESA));
        assertTrue(categorias.stream().anyMatch(c -> c.nome().equals("Salário") && c.tipo() == TipoLancamento.RECEITA));

        Long alimentacao = idDaCategoria("Alimentação");
        DespesaDto.Response despesa = despesaService.criar(despesa("Mercado", "50", alimentacao));
        assertEquals(alimentacao, despesa.categoriaId());

        categoriaService.excluir(alimentacao);

        assertNull(despesaService.listarDoUsuarioLogado().get(0).categoriaId());
    }

    @Test
    void categoriaDeOutroTipoNaoEhAceita() {
        Long salario = idDaCategoria("Salário");
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> despesaService.criar(despesa("Mercado", "50", salario)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void editaEExcluiSomenteOsProprioLancamentos() {
        DespesaDto.Response despesa = despesaService.criar(despesa("Mercado", "50", null));
        ReceitaDto.Response receita = receitaService.criar(
                new ReceitaDto.Request("Salário", "Empresa", new BigDecimal("1000"), LocalDate.of(2026, 10, 5), null));

        DespesaDto.Response editada = despesaService.atualizar(despesa.id(), despesa("Feira", "42.50", null));
        assertEquals("Feira", editada.descricao());
        assertEquals(0, new BigDecimal("42.50").compareTo(editada.valor()));
        assertEquals(LocalDate.of(2026, 10, 5), editada.data(), "a data deve voltar igual");

        // Outro usuário não enxerga nem altera.
        autenticarComo(novoUsuario());
        assertTrue(despesaService.listarDoUsuarioLogado().isEmpty());
        assertNotFound(() -> despesaService.atualizar(despesa.id(), despesa("Hack", "1", null)));
        assertNotFound(() -> receitaService.excluir(receita.id()));

        autenticarComo(usuario);
        despesaService.excluir(despesa.id());
        receitaService.excluir(receita.id());
        assertTrue(despesaService.listarDoUsuarioLogado().isEmpty());
        assertTrue(receitaService.listarDoUsuarioLogado().isEmpty());
    }

    @Test
    void contaPagaGeraLancamentoEReabrirRemove() {
        CompromissoDto.Response conta = compromissoService.criar(new CompromissoDto.Request(
                TipoLancamento.DESPESA, "Internet", "Vivo", new BigDecimal("119.90"),
                LocalDate.of(2026, 11, 10), idDaCategoria("Moradia")));

        CompromissoDto.Response paga = compromissoService.pagar(conta.id(), LocalDate.of(2026, 11, 8));
        assertEquals(LocalDate.of(2026, 11, 8), paga.pagoEm());
        DespesaDto.Response gerada = despesaService.listarDoUsuarioLogado().get(0);
        assertEquals(paga.lancamentoId(), gerada.id());
        assertEquals("Internet", gerada.descricao());
        assertEquals(idDaCategoria("Moradia"), gerada.categoriaId());

        assertThrows(ResponseStatusException.class, () -> compromissoService.pagar(conta.id(), null));

        CompromissoDto.Response reaberta = compromissoService.reabrir(conta.id());
        assertNull(reaberta.pagoEm());
        assertTrue(despesaService.listarDoUsuarioLogado().isEmpty());
    }

    @Test
    void recorrenciaGeraUmaVezPorMesQuandoChegaODia() {
        LocalDate hoje = Datas.hoje();
        YearMonth mes = YearMonth.from(hoje);

        // Dia de hoje: gera na criação.
        recorrenciaService.criar(recorrencia("Salário", TipoLancamento.RECEITA, hoje.getDayOfMonth()));
        assertEquals(1, receitaService.listarDoUsuarioLogado().size());

        // Dia 31: gera quando o agendador rodar nesse dia (ou no último dia de meses curtos).
        RecorrenciaDto.Response aluguel = recorrenciaService.criar(recorrencia("Aluguel", TipoLancamento.DESPESA, 31));
        LocalDate fimDoMes = mes.atEndOfMonth();
        if (!fimDoMes.equals(hoje)) {
            assertEquals(fimDoMes, aluguel.proximaData());
            assertTrue(despesaService.listarDoUsuarioLogado().isEmpty());
        }

        recorrenciaService.gerarPendentes(fimDoMes);
        recorrenciaService.gerarPendentes(fimDoMes); // segunda execução não duplica
        assertEquals(1, despesaService.listarDoUsuarioLogado().size());
        assertEquals(1, receitaService.listarDoUsuarioLogado().size());
        assertEquals(fimDoMes, despesaService.listarDoUsuarioLogado().get(0).data());
    }

    @Test
    void orcamentoSomaGastosDaCategoriaNoMesENaoDuplica() {
        Long alimentacao = idDaCategoria("Alimentação");
        orcamentoService.criar(new OrcamentoDto.Request(alimentacao, new BigDecimal("200")));
        despesaService.criar(despesa("Mercado", "150", alimentacao));
        despesaService.criar(despesa("Padaria", "20", alimentacao));

        var categoria = categoriaService.buscarParaLancamento(alimentacao, usuario, TipoLancamento.DESPESA);
        OrcamentoService.Situacao situacao = orcamentoService.situacao(categoria, YearMonth.of(2026, 10)).orElseThrow();
        assertEquals(0, new BigDecimal("170").compareTo(situacao.gasto()));
        assertEquals(85, situacao.percentual());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> orcamentoService.criar(new OrcamentoDto.Request(alimentacao, new BigDecimal("50"))));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void metaAceitaGuardarERetirarSemFicarNegativa() {
        MetaDto.Response meta = metaService.criar(
                new MetaDto.Request("Viagem", new BigDecimal("5000"), LocalDate.of(2027, 6, 1), "plane", "#14b8a6"));

        assertEquals(0, new BigDecimal("300").compareTo(metaService.movimentar(meta.id(), new BigDecimal("300")).valorGuardado()));
        assertEquals(0, new BigDecimal("200").compareTo(metaService.movimentar(meta.id(), new BigDecimal("-100")).valorGuardado()));
        assertThrows(ResponseStatusException.class, () -> metaService.movimentar(meta.id(), new BigDecimal("-500")));
    }

    @Test
    void botClassificaPelaDescricaoEAvisaOrcamento() {
        Long transporte = idDaCategoria("Transporte");
        orcamentoService.criar(new OrcamentoDto.Request(transporte, new BigDecimal("100")));

        TelegramLancamentoService.Registro registro = telegramLancamentoService.registrar(
                usuario, parser.interpretar("uber 90 05/10", LocalDate.of(2026, 11, 1)));

        assertEquals("Transporte", registro.categoria());
        assertNotNull(registro.orcamento(), "90% do orçamento deve gerar alerta");
        assertEquals(transporte, despesaService.listarDoUsuarioLogado().get(0).categoriaId());

        TelegramLancamentoService.Registro semPista = telegramLancamentoService.registrar(
                usuario, parser.interpretar("coisa aleatória 10", LocalDate.of(2026, 11, 1)));
        assertEquals("Outros", semPista.categoria());
        assertNull(semPista.orcamento());
    }

    // ---------- Apoio ----------

    private DespesaDto.Request despesa(String descricao, String valor, Long categoriaId) {
        return new DespesaDto.Request(descricao, null, new BigDecimal(valor), LocalDate.of(2026, 10, 5), categoriaId);
    }

    private RecorrenciaDto.Request recorrencia(String descricao, TipoLancamento tipo, int dia) {
        return new RecorrenciaDto.Request(tipo, descricao, null, new BigDecimal("100"), null, dia, true);
    }

    private Long idDaCategoria(String nome) {
        return categoriaService.listarDoUsuarioLogado().stream()
                .filter(c -> c.nome().equals(nome)).findFirst().orElseThrow().id();
    }

    private User novoUsuario() {
        return userRepository.save(User.builder()
                .email("plan-it-" + UUID.randomUUID() + "@nickfinance.test")
                .password("x").nome("Teste").sobrenome("Plano")
                .build());
    }

    private static void autenticarComo(User usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of()));
    }

    private static void assertNotFound(Runnable acao) {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, acao::run);
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }
}
