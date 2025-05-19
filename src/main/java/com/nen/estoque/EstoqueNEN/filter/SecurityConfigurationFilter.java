package com.nen.estoque.EstoqueNEN.filter;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfigurationFilter {

    public static final String [] ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED = {
            "/users/login", //url que usaremos para fazer login
            "/users" //url que usaremos para criar um usuário
    };

}
