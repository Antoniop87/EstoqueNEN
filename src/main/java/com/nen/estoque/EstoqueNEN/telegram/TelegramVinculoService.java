package com.nen.estoque.EstoqueNEN.telegram;

import com.nen.estoque.EstoqueNEN.dto.TelegramCodigoDto;
import com.nen.estoque.EstoqueNEN.dto.TelegramStatusDto;
import com.nen.estoque.EstoqueNEN.model.TelegramVinculo;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.TelegramVinculoRepository;
import com.nen.estoque.EstoqueNEN.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Liga e desliga contas do Nick Finance de chats do Telegram.
 *
 * Fluxo: o usuário gera um código no app (expira em poucos minutos) e o envia ao bot;
 * o bot chama {@link #vincular(String, Long)} e, a partir daí, o chat fica ligado à conta.
 */
@Service
@RequiredArgsConstructor
public class TelegramVinculoService {

    static final Duration VALIDADE_CODIGO = Duration.ofMinutes(10);

    /** Sem 0/O e 1/I/L, para não confundir ao digitar. */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;

    private final TelegramVinculoRepository repository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final TelegramBotProperties botProperties;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public TelegramStatusDto status() {
        Optional<TelegramVinculo> vinculo = repository.findByUsuario(usuarioLogadoService.getUsuario())
                .filter(TelegramVinculo::isVinculado);
        return new TelegramStatusDto(
                botProperties.isConfigurado(),
                botProperties.isConfigurado() ? botProperties.getUsername() : null,
                vinculo.isPresent(),
                vinculo.map(TelegramVinculo::getVinculadoEm).orElse(null)
        );
    }

    @Transactional
    public TelegramCodigoDto gerarCodigo() {
        User usuario = usuarioLogadoService.getUsuario();
        TelegramVinculo vinculo = repository.findByUsuario(usuario)
                .orElseGet(() -> TelegramVinculo.builder().usuario(usuario).build());

        vinculo.setCodigo(novoCodigoUnico());
        vinculo.setCodigoExpiraEm(Instant.now().plus(VALIDADE_CODIGO));
        repository.save(vinculo);

        return new TelegramCodigoDto(
                vinculo.getCodigo(),
                vinculo.getCodigoExpiraEm(),
                botProperties.linkComCodigo(vinculo.getCodigo())
        );
    }

    @Transactional
    public void desvincularUsuarioLogado() {
        repository.findByUsuario(usuarioLogadoService.getUsuario()).ifPresent(repository::delete);
    }

    /**
     * Liga o chat à conta dona do código. Um chat só pode estar ligado a uma conta:
     * se já estava ligado a outra, essa ligação antiga é desfeita.
     *
     * @return o usuário vinculado, ou vazio se o código for inválido ou estiver expirado
     */
    @Transactional
    public Optional<User> vincular(String codigo, Long chatId) {
        Optional<TelegramVinculo> encontrado = repository.findByCodigo(codigo.trim().toUpperCase())
                .filter(v -> v.getCodigoExpiraEm() != null && v.getCodigoExpiraEm().isAfter(Instant.now()));
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        TelegramVinculo vinculo = encontrado.get();
        repository.findByChatId(chatId)
                .filter(antigo -> !antigo.getId().equals(vinculo.getId()))
                .ifPresent(antigo -> {
                    repository.delete(antigo);
                    repository.flush();
                });

        vinculo.setChatId(chatId);
        vinculo.setVinculadoEm(Instant.now());
        vinculo.setCodigo(null);
        vinculo.setCodigoExpiraEm(null);
        repository.save(vinculo);
        return Optional.of(vinculo.getUsuario());
    }

    @Transactional
    public boolean desvincularChat(Long chatId) {
        Optional<TelegramVinculo> vinculo = repository.findByChatId(chatId);
        vinculo.ifPresent(repository::delete);
        return vinculo.isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<User> usuarioDoChat(Long chatId) {
        return repository.findByChatId(chatId).map(TelegramVinculo::getUsuario);
    }

    private String novoCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (repository.findByCodigo(codigo).isPresent());
        return codigo;
    }
}
