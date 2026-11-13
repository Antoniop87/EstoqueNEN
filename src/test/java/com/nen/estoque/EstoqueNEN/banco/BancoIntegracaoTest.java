package com.nen.estoque.EstoqueNEN.banco;

import com.nen.estoque.EstoqueNEN.dto.BancoDto;
import com.nen.estoque.EstoqueNEN.dto.DespesaDto;
import com.nen.estoque.EstoqueNEN.model.FormaPagamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.UserRepository;
import com.nen.estoque.EstoqueNEN.service.DespesaService;
import com.nen.estoque.EstoqueNEN.util.Datas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Fluxo de conexão e revisão com a Pluggy simulada, contra o banco real. Cada teste é revertido. */
@SpringBootTest(properties = "telegram.bot.token=")
@Transactional
class BancoIntegracaoTest {

    @MockitoBean private PluggyCliente pluggy;
    @Autowired private BancoService bancos;
    @Autowired private DespesaService despesas;
    @Autowired private UserRepository users;

    private User usuario;
    private String itemId;

    @BeforeEach
    void setUp() {
        usuario = users.save(User.builder()
                .email("banco-it-" + UUID.randomUUID() + "@nickfinance.test")
                .password("x").nome("Teste").sobrenome("Banco").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of()));

        itemId = UUID.randomUUID().toString();
        when(pluggy.configurado()).thenReturn(true);
        when(pluggy.buscarItem(itemId)).thenReturn(
                new PluggyCliente.Item(itemId, String.valueOf(usuario.getId()), "Banco Teste", "UPDATED", null));
        when(pluggy.contas(itemId)).thenReturn(List.of(new PluggyCliente.Conta("conta-1", "BANK", "Conta corrente")));
        when(pluggy.transacoes(eq("conta-1"), any())).thenReturn(List.of(
                transacao("a", "-50", "Mercado"),
                transacao("b", "-20", "Uber"),
                transacao("c", "1000", "Salário")));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private static PluggyCliente.Transacao transacao(String id, String valor, String descricao) {
        return new PluggyCliente.Transacao(id + "-" + UUID.randomUUID(), descricao, new BigDecimal(valor),
                Datas.hoje(), "DEBIT", "POSTED", null);
    }

    @Test
    void conectaSincronizaEImporta() {
        despesas.criar(new DespesaDto.Request("Mercado", null, new BigDecimal("50.00"), Datas.hoje(), null, FormaPagamento.DEBITO));

        BancoDto.Conexao conexao = bancos.conectar(itemId);
        assertEquals("Banco Teste", conexao.nomeBanco());

        List<BancoDto.Transacao> pendentes = bancos.pendentes();
        assertEquals(3, pendentes.size());
        BancoDto.Transacao mercado = pendentes.stream().filter(t -> t.descricao().equals("Mercado")).findFirst().orElseThrow();
        assertTrue(mercado.possivelDuplicado());

        // Sincronizar de novo não duplica a fila.
        assertEquals(0, bancos.sincronizar(conexao.id()).quantidade());

        bancos.ignorar(mercado.id());
        assertEquals(2, bancos.importarTodas().quantidade());
        assertTrue(bancos.pendentes().isEmpty());
        assertEquals(0, bancos.status().pendentes());
        assertThrows(ResponseStatusException.class, () -> bancos.ignorar(mercado.id()));
    }

    @Test
    void mesmoBancoConectadoDuasVezesMarcaPossivelDuplicado() {
        bancos.conectar(itemId);

        String segundo = UUID.randomUUID().toString();
        when(pluggy.buscarItem(segundo)).thenReturn(
                new PluggyCliente.Item(segundo, String.valueOf(usuario.getId()), "Banco Teste", "UPDATED", null));
        when(pluggy.contas(segundo)).thenReturn(List.of(new PluggyCliente.Conta("conta-2", "BANK", "Conta corrente")));
        when(pluggy.transacoes(eq("conta-2"), any())).thenReturn(List.of(transacao("d", "-20", "Uber")));
        bancos.conectar(segundo);

        List<BancoDto.Transacao> uber = bancos.pendentes().stream().filter(t -> t.descricao().equals("Uber")).toList();
        assertEquals(2, uber.size());
        assertEquals(1, uber.stream().filter(BancoDto.Transacao::possivelDuplicado).count());
    }

    @Test
    void recusaItemDeOutroUsuario() {
        when(pluggy.buscarItem(itemId)).thenReturn(
                new PluggyCliente.Item(itemId, "outro", "Banco Teste", "UPDATED", null));
        assertThrows(ResponseStatusException.class, () -> bancos.conectar(itemId));
        assertTrue(bancos.status().conexoes().isEmpty());
    }
}
