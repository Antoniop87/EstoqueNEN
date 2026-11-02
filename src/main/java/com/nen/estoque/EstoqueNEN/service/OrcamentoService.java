package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.OrcamentoDto;
import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.Orcamento;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.DespesaRepository;
import com.nen.estoque.EstoqueNEN.repository.OrcamentoRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository repository;
    private final DespesaRepository despesaRepository;
    private final CategoriaService categoriaService;
    private final UsuarioLogadoService usuarioLogadoService;

    /** Limite e gasto de uma categoria em um mês. */
    public record Situacao(BigDecimal limite, BigDecimal gasto) {
        public int percentual() {
            return gasto.multiply(BigDecimal.valueOf(100)).divide(limite, 0, java.math.RoundingMode.DOWN).intValue();
        }
    }

    @Transactional(readOnly = true)
    public List<OrcamentoDto.Response> listarDoUsuarioLogado() {
        return repository.findAllByUsuario(usuarioLogadoService.getUsuario()).stream()
                .map(OrcamentoDto.Response::from)
                .toList();
    }

    @Transactional
    public OrcamentoDto.Response criar(OrcamentoDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Orcamento orcamento = Orcamento.builder().usuario(usuario).build();
        aplicar(orcamento, dto, usuario);
        return OrcamentoDto.Response.from(repository.save(orcamento));
    }

    @Transactional
    public OrcamentoDto.Response atualizar(Long id, OrcamentoDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Orcamento orcamento = buscar(id, usuario);
        aplicar(orcamento, dto, usuario);
        return OrcamentoDto.Response.from(orcamento);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id, usuarioLogadoService.getUsuario()));
    }

    /** Situação do orçamento da categoria no mês, se ela tiver orçamento. */
    @Transactional(readOnly = true)
    public Optional<Situacao> situacao(Categoria categoria, YearMonth mes) {
        return repository.findByCategoria(categoria).map(orcamento -> new Situacao(
                orcamento.getValorLimite(),
                despesaRepository.somarPorCategoriaNoPeriodo(categoria,
                        Datas.paraDate(mes.atDay(1)), Datas.paraDate(mes.plusMonths(1).atDay(1)))
        ));
    }

    private Orcamento buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orçamento não encontrado."));
    }

    private void aplicar(Orcamento orcamento, OrcamentoDto.Request dto, User usuario) {
        Categoria categoria = categoriaService.buscarParaLancamento(dto.categoriaId(), usuario, TipoLancamento.DESPESA);
        repository.findByCategoria(categoria)
                .filter(existente -> !existente.getId().equals(orcamento.getId()))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa categoria já tem orçamento.");
                });
        orcamento.setCategoria(categoria);
        orcamento.setValorLimite(dto.valorLimite());
    }
}
