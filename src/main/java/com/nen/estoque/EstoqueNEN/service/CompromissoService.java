package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.CompromissoDto;
import com.nen.estoque.EstoqueNEN.model.Compromisso;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.CompromissoRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Contas a pagar/receber. Marcar como paga cria o lançamento (despesa ou receita);
 * reabrir remove esse lançamento.
 */
@Service
@RequiredArgsConstructor
public class CompromissoService {

    private final CompromissoRepository repository;
    private final CategoriaService categoriaService;
    private final LancamentoService lancamentoService;
    private final UsuarioLogadoService usuarioLogadoService;

    @Transactional(readOnly = true)
    public List<CompromissoDto.Response> listarDoUsuarioLogado() {
        return repository.findAllByUsuarioOrderByVencimentoAscIdAsc(usuarioLogadoService.getUsuario()).stream()
                .map(CompromissoDto.Response::from)
                .toList();
    }

    @Transactional
    public CompromissoDto.Response criar(CompromissoDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Compromisso compromisso = Compromisso.builder().usuario(usuario).build();
        aplicar(compromisso, dto, usuario);
        return CompromissoDto.Response.from(repository.save(compromisso));
    }

    @Transactional
    public CompromissoDto.Response atualizar(Long id, CompromissoDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Compromisso compromisso = buscar(id, usuario);
        if (compromisso.isPago()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reabra a conta para editá-la.");
        }
        aplicar(compromisso, dto, usuario);
        return CompromissoDto.Response.from(compromisso);
    }

    /** Exclui a conta. Se já estava paga, o lançamento gerado continua no extrato. */
    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id, usuarioLogadoService.getUsuario()));
    }

    @Transactional
    public CompromissoDto.Response pagar(Long id, LocalDate data) {
        User usuario = usuarioLogadoService.getUsuario();
        Compromisso c = buscar(id, usuario);
        if (c.isPago()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Essa conta já está paga.");
        }
        LocalDate pagamento = data != null ? data : Datas.hoje();
        Long lancamentoId = lancamentoService.criar(usuario, new LancamentoService.Novo(
                c.getTipo(), c.getDescricao(), c.getDetalhe(), c.getValor(), pagamento, c.getCategoria()));
        c.setPagoEm(pagamento);
        c.setLancamentoId(lancamentoId);
        return CompromissoDto.Response.from(c);
    }

    @Transactional
    public CompromissoDto.Response reabrir(Long id) {
        User usuario = usuarioLogadoService.getUsuario();
        Compromisso c = buscar(id, usuario);
        if (c.isPago()) {
            lancamentoService.excluir(usuario, c.getTipo(), c.getLancamentoId());
            c.setPagoEm(null);
            c.setLancamentoId(null);
        }
        return CompromissoDto.Response.from(c);
    }

    /** Contas em aberto que vencem até a data (inclui atrasadas). Usado pelos lembretes. */
    @Transactional(readOnly = true)
    public List<Compromisso> emAbertoAte(User usuario, LocalDate ate) {
        return repository.findAllByUsuarioAndPagoEmIsNullAndVencimentoLessThanEqualOrderByVencimentoAsc(usuario, ate);
    }

    private Compromisso buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    private void aplicar(Compromisso c, CompromissoDto.Request dto, User usuario) {
        c.setTipo(dto.tipo());
        c.setDescricao(dto.descricao().trim());
        c.setDetalhe(DespesaService.textoOuNulo(dto.detalhe()));
        c.setValor(dto.valor());
        c.setVencimento(dto.vencimento());
        c.setCategoria(categoriaService.buscarParaLancamento(dto.categoriaId(), usuario, dto.tipo()));
    }
}
