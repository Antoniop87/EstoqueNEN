package com.nen.estoque.EstoqueNEN.service;

import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Recupera o usuário dono do token da requisição atual.
 * Use isto em vez de receber o id do usuário pela URL ou pelo body.
 */
@Service
@RequiredArgsConstructor
public class UsuarioLogadoService {

    private final UserRepository userRepository;

    public User getUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UsernameNotFoundException("Nenhum usuário autenticado.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
