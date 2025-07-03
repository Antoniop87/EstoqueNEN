package com.nen.estoque.EstoqueNEN.controller;

import com.nen.estoque.EstoqueNEN.dto.LoginUserDto;
import com.nen.estoque.EstoqueNEN.dto.RecoveryJwtTokenDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/produto")
public class ProdutoController {

    @PostMapping
    public ResponseEntity<Void> cadastraProduto(@RequestBody LoginUserDto loginUserDto) {
//        RecoveryJwtTokenDto token = userService.authenticateUser(loginUserDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
