package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.ReceitaDto;
import com.nen.estoque.EstoqueNEN.model.Receita;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceitaService {

    private final ReceitaRepository repository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final CategoriaService categoriaService;

    @Transactional(readOnly = true)
    public List<ReceitaDto.Response> listarDoUsuarioLogado() {
        return repository.findAllByUsuarioOrderByDataDescIdDesc(usuarioLogadoService.getUsuario())
                .stream()
                .map(ReceitaDto.Response::from)
                .toList();
    }

    @Transactional
    public ReceitaDto.Response criar(ReceitaDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Receita receita = Receita.builder().usuario(usuario).build();
        aplicar(receita, dto, usuario);
        return ReceitaDto.Response.from(repository.save(receita));
    }

    @Transactional
    public ReceitaDto.Response atualizar(Long id, ReceitaDto.Request dto) {
        User usuario = usuarioLogadoService.getUsuario();
        Receita receita = buscar(id, usuario);
        aplicar(receita, dto, usuario);
        return ReceitaDto.Response.from(receita);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id, usuarioLogadoService.getUsuario()));
    }

    private Receita buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada não encontrada."));
    }

    private void aplicar(Receita receita, ReceitaDto.Request dto, User usuario) {
        receita.setDescricao(dto.descricao().trim());
        receita.setOrigem(DespesaService.textoOuNulo(dto.origem()));
        receita.setValor(dto.valor());
        receita.setData(dto.data());
        receita.setCategoria(categoriaService.buscarParaLancamento(dto.categoriaId(), usuario, TipoLancamento.RECEITA));
    }
}
