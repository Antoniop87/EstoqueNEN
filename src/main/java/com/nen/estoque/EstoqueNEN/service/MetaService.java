package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.dto.MetaDto;
import com.nen.estoque.EstoqueNEN.model.Meta;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.MetaRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetaService {

    private final MetaRepository repository;
    private final UsuarioLogadoService usuarioLogadoService;

    @Transactional(readOnly = true)
    public List<MetaDto.Response> listarDoUsuarioLogado() {
        return repository.findAllByUsuarioOrderByCriadaEmAscIdAsc(usuarioLogadoService.getUsuario()).stream()
                .map(MetaDto.Response::from)
                .toList();
    }

    @Transactional
    public MetaDto.Response criar(MetaDto.Request dto) {
        Meta meta = Meta.builder()
                .usuario(usuarioLogadoService.getUsuario())
                .valorGuardado(BigDecimal.ZERO)
                .criadaEm(Datas.hoje())
                .build();
        aplicar(meta, dto);
        return MetaDto.Response.from(repository.save(meta));
    }

    @Transactional
    public MetaDto.Response atualizar(Long id, MetaDto.Request dto) {
        Meta meta = buscar(id, usuarioLogadoService.getUsuario());
        aplicar(meta, dto);
        return MetaDto.Response.from(meta);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id, usuarioLogadoService.getUsuario()));
    }

    /** Guarda (valor positivo) ou retira (negativo). O total guardado nunca fica negativo. */
    @Transactional
    public MetaDto.Response movimentar(Long id, BigDecimal valor) {
        Meta meta = buscar(id, usuarioLogadoService.getUsuario());
        BigDecimal novoTotal = meta.getValorGuardado().add(valor);
        if (valor.signum() == 0 || novoTotal.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor inválido para esta meta.");
        }
        meta.setValorGuardado(novoTotal);
        return MetaDto.Response.from(meta);
    }

    private Meta buscar(Long id, User usuario) {
        return repository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta não encontrada."));
    }

    private static void aplicar(Meta meta, MetaDto.Request dto) {
        meta.setNome(dto.nome().trim());
        meta.setValorAlvo(dto.valorAlvo());
        meta.setPrazo(dto.prazo());
        meta.setIcone(dto.icone());
        meta.setCor(dto.cor().toLowerCase());
    }
}
