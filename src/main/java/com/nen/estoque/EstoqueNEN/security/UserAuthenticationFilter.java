package com.nen.estoque.EstoqueNEN.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nen.estoque.EstoqueNEN.details.UserDetailsImpl;
import com.nen.estoque.EstoqueNEN.filter.SecurityConfigurationFilter;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.UserRepository;
import com.nen.estoque.EstoqueNEN.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class UserAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService; // Service que definimos anteriormente

    @Autowired
    private UserRepository userRepository; // Repository que definimos anteriormente

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Verifica se o endpoint requer autenticação antes de processar a requisição
        if (checkIfEndpointIsNotPublic(request)) {
            String token = recoveryToken(request); // Recupera o token do cabeçalho Authorization da requisição
            if (token != null) {
                try {
                    String subject = jwtTokenService.getSubjectFromToken(token); // Obtém o assunto (neste caso, o nome de usuário) do token
                    userRepository.findByEmail(subject).ifPresent(this::authenticate); // Busca o usuário pelo email (que é o assunto do token)
                } catch (JWTVerificationException exception) {
                    // Token inválido ou expirado: segue sem autenticação e o Spring Security responde 403
                }
            }
            // Sem token, a requisição segue anônima e o Spring Security responde 403 (em vez de um 500)
        }
        filterChain.doFilter(request, response); // Continua o processamento da requisição
    }

    // Define o usuário autenticado no contexto de segurança do Spring Security
    private void authenticate(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Recupera o token do cabeçalho Authorization da requisição
    private String recoveryToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }

    // Verifica se o endpoint requer autenticação antes de processar a requisição
    private boolean checkIfEndpointIsNotPublic(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return !Arrays.asList(SecurityConfigurationFilter.ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED).contains(requestURI);
    }

}
