package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.enums.RoleName;

import java.time.LocalDate;

public record CreateUserDto(

        String email,
        String password,
        RoleName role,
        String nome,
        String sobrenome,
        LocalDate dataNascimento

) {
}