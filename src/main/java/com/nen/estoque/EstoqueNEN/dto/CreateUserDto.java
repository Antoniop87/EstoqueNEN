package com.nen.estoque.EstoqueNEN.dto;

import com.nen.estoque.EstoqueNEN.enums.RoleName;

public record CreateUserDto(

        String email,
        String password,
        RoleName role

) {
}