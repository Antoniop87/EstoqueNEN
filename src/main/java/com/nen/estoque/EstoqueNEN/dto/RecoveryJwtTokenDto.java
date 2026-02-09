package com.nen.estoque.EstoqueNEN.dto;

public record RecoveryJwtTokenDto(

        String token,
        Long id,
        String nome,
        String sobrenome
) {
}
