package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.DespesaDto;
import com.nen.estoque.EstoqueNEN.model.Despesa;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.DespesaRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository repository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final CategoriaService categoriaService;

    @Transactional(readOnly = true)
    public List<DespesaDto.Response> listarDoUsuarioLogado() {
        return repository.findAllByUsuarioOrderByDataDespesaDescIdDesc(usuarioLogadoService.getUsuario())
                .stream()
                .map(DespesaDto.Response::from)
                .toList();
    }

    @Transactional
    public DespesaDto.Response criar(DespesaDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Despesa despesa = Despesa.builder().usuario(usuario).build();
        aplicar(despesa, dto, usuario);
        return DespesaDto.Response.from(repository.save(despesa));
    }

    @Transactional
    public DespesaDto.Response atualizar(Long id, DespesaDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Despesa despesa = buscar(id, usuario);
        aplicar(despesa, dto, usuario);
        return DespesaDto.Response.from(despesa);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id, usuarioLogadoService.getUsuario()));
    }

    private Despesa buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa não encontrada."));
    }

    private void aplicar(Despesa despesa, DespesaDto.Request dto, User usuario) {
        despesa.setNomeDespesa(dto.descricao().trim());
        despesa.setEstabelecimento(textoOuNulo(dto.estabelecimento()));
        despesa.setValorDespesa(dto.valor());
        despesa.setDataDespesa(Datas.paraDate(dto.data()));
        despesa.setCategoria(categoriaService.buscarParaLancamento(dto.categoriaId(), usuario, TipoLancamento.DESPESA));
        despesa.setFormaPagamento(dto.formaPagamento());
    }

    static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
