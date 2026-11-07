package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.RecorrenciaDto;
import com.nen.estoque.EstoqueNEN.model.Recorrencia;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.RecorrenciaRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Lançamentos recorrentes. Regra de geração: no mês corrente, quando chega o dia,
 * o lançamento é criado uma única vez. Meses anteriores à criação (ou à reativação)
 * não são gerados retroativamente, para não duplicar o que o usuário já lançou à mão.
 */
@Service
@RequiredArgsConstructor
public class RecorrenciaService {

    private final RecorrenciaRepository repository;
    private final CategoriaService categoriaService;
    private final LancamentoService lancamentoService;
    private final UsuarioLogadoService usuarioLogadoService;

    @Transactional(readOnly = true)
    public List<RecorrenciaDto.Response> listarDoUsuarioLogado() {
        LocalDate hoje = Datas.hoje();
        return repository.findAllByUsuarioOrderByDiaDoMesAscIdAsc(usuarioLogadoService.getUsuario()).stream()
                .map(r -> RecorrenciaDto.Response.from(r, proximaData(r, hoje)))
                .toList();
    }

    @Transactional
    public RecorrenciaDto.Response criar(RecorrenciaDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Recorrencia recorrencia = Recorrencia.builder().usuario(usuario).build();
        return salvar(recorrencia, dto, usuario);
    }

    @Transactional
    public RecorrenciaDto.Response atualizar(Long id, RecorrenciaDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        return salvar(buscar(id, usuario), dto, usuario);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id, usuarioLogadoService.getUsuario()));
    }

    /** Cria os lançamentos do mês que já chegaram no dia. Chamado pelo agendador. */
    @Transactional
    public int gerarPendentes(LocalDate hoje) {
        int gerados = 0;
        for (Recorrencia recorrencia : repository.findAllByAtivaTrue()) {
            if (gerarSeDevida(recorrencia, hoje)) {
                gerados++;
            }
        }
        return gerados;
    }

    static LocalDate proximaData(Recorrencia r, LocalDate hoje) {
        if (!r.isAtiva()) {
            return null;
        }
        YearMonth mes = YearMonth.from(hoje);
        return r.geradaEm(mes) ? r.dataEm(mes.plusMonths(1)) : r.dataEm(mes);
    }

    private RecorrenciaDto.Response salvar(Recorrencia r, RecorrenciaDto.Request dto, User usuario) {
        r.setTipo(dto.tipo());
        r.setDescricao(dto.descricao().trim());
        r.setDetalhe(DespesaService.textoOuNulo(dto.detalhe()));
        r.setValor(dto.valor());
        r.setCategoria(categoriaService.buscarParaLancamento(dto.categoriaId(), usuario, dto.tipo()));
        r.setDiaDoMes(dto.diaDoMes());
        r.setAtiva(dto.ativa());

        LocalDate hoje = Datas.hoje();
        YearMonth mes = YearMonth.from(hoje);
        // O dia deste mês já passou: começa no próximo mês, sem lançar retroativamente.
        if (r.isAtiva() && !r.geradaEm(mes) && r.dataEm(mes).isBefore(hoje)) {
            r.setUltimoMesGerado(mes.toString());
        }
        repository.save(r);
        gerarSeDevida(r, hoje);
        return RecorrenciaDto.Response.from(r, proximaData(r, hoje));
    }

    private boolean gerarSeDevida(Recorrencia r, LocalDate hoje) {
        YearMonth mes = YearMonth.from(hoje);
        LocalDate data = r.dataEm(mes);
        if (!r.isAtiva() || r.geradaEm(mes) || data.isAfter(hoje)) {
            return false;
        }
        lancamentoService.criar(r.getUsuario(), new LancamentoService.Novo(
                r.getTipo(), r.getDescricao(), r.getDetalhe(), r.getValor(), data, r.getCategoria(), null));
        r.setUltimoMesGerado(mes.toString());
        return true;
    }

    private Recorrencia buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recorrência não encontrada."));
    }
}
