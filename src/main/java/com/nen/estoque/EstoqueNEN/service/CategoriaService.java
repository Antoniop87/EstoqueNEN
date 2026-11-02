package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.CategoriaDto;
import com.nen.estoque.EstoqueNEN.model.Categoria;
import com.nen.estoque.EstoqueNEN.model.TipoLancamento;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;
    private final DespesaRepository despesaRepository;
    private final ReceitaRepository receitaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final RecorrenciaRepository recorrenciaRepository;
    private final CompromissoRepository compromissoRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    @Transactional
    public List<CategoriaDto.Response> listarDoUsuarioLogado() {
        User usuario = usuarioLogadoService.getUsuario();
        garantirPadroes(usuario);
        return repository.findAllByUsuarioOrderByTipoAscNomeAsc(usuario).stream()
                .map(CategoriaDto.Response::from)
                .toList();
    }

    @Transactional
    public CategoriaDto.Response criar(CategoriaDto.Request dto) {
        Categoria categoria = Categoria.builder().usuario(usuarioLogadoService.getUsuario()).build();
        aplicar(categoria, dto);
        return CategoriaDto.Response.from(repository.save(categoria));
    }

    @Transactional
    public CategoriaDto.Response atualizar(Long id, CategoriaDto.Request dto) {
        Categoria categoria = buscar(id, usuarioLogadoService.getUsuario());
        if (categoria.getTipo() != dto.tipo()) {
            // Trocar o tipo deixaria lançamentos de um tipo apontando para categoria de outro.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo da categoria não pode ser alterado.");
        }
        aplicar(categoria, dto);
        return CategoriaDto.Response.from(categoria);
    }

    /** Exclui a categoria. Lançamentos e planos que a usavam ficam sem categoria; o orçamento dela é removido. */
    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscar(id, usuarioLogadoService.getUsuario());
        despesaRepository.limparCategoria(categoria);
        receitaRepository.limparCategoria(categoria);
        recorrenciaRepository.limparCategoria(categoria);
        compromissoRepository.limparCategoria(categoria);
        orcamentoRepository.deleteAllByCategoria(categoria);
        repository.delete(categoria);
    }

    /**
     * Busca uma categoria do usuário e confere se é do tipo esperado.
     * `null` em `id` significa "sem categoria".
     */
    @Transactional(readOnly = true)
    public Categoria buscarParaLancamento(Long id, User usuario, TipoLancamento tipo) {
        if (id == null) {
            return null;
        }
        Categoria categoria = buscar(id, usuario);
        if (categoria.getTipo() != tipo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A categoria não é do tipo do lançamento.");
        }
        return categoria;
    }

    /** Sugere uma categoria do usuário a partir do texto (usado pelo bot). */
    @Transactional
    public Optional<Categoria> sugerir(User usuario, TipoLancamento tipo, String texto) {
        garantirPadroes(usuario);
        List<Categoria> categorias = repository.findAllByUsuarioAndTipo(usuario, tipo);
        return CategoriasPadrao.sugerir(tipo, texto)
                .or(() -> Optional.of(CategoriasPadrao.OUTROS))
                .flatMap(nome -> categorias.stream()
                        .filter(c -> CategoriasPadrao.normalizar(c.getNome()).equals(CategoriasPadrao.normalizar(nome)))
                        .findFirst());
    }

    /** Cria as categorias padrão para quem ainda não tem nenhuma. */
    @Transactional
    public void garantirPadroes(User usuario) {
        if (repository.existsByUsuario(usuario)) {
            return;
        }
        repository.saveAll(CategoriasPadrao.TODAS.stream()
                .map(m -> Categoria.builder()
                        .nome(m.nome()).icone(m.icone()).cor(m.cor()).tipo(m.tipo()).usuario(usuario)
                        .build())
                .toList());
    }

    private Categoria buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));
    }

    private static void aplicar(Categoria categoria, CategoriaDto.Request dto) {
        categoria.setNome(dto.nome().trim());
        categoria.setIcone(dto.icone());
        categoria.setCor(dto.cor().toLowerCase());
        categoria.setTipo(dto.tipo());
    }
}
