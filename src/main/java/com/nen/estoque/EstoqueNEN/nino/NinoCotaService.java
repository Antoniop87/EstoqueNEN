package com.nen.estoque.EstoqueNEN.nino;

import com.nen.estoque.EstoqueNEN.model.NinoUso;
import com.nen.estoque.EstoqueNEN.model.User;
import com.nen.estoque.EstoqueNEN.repository.NinoUsoRepository;
import com.nen.estoque.EstoqueNEN.util.Datas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Limite diário por usuário, para ninguém consumir sozinho a cota gratuita do provedor. */
@Service
public class NinoCotaService {

    private final NinoUsoRepository repository;
    private final int limiteDiario;

    public NinoCotaService(NinoUsoRepository repository, @Value("${nino.limite-diario:30}") int limiteDiario) {
        this.repository = repository;
        this.limiteDiario = limiteDiario;
    }

    public int limiteDiario() {
        return limiteDiario;
    }

    @Transactional(readOnly = true)
    public int restantes(User usuario) {
        int usadas = repository.findByUsuarioAndDia(usuario, Datas.hoje()).map(NinoUso::getQuantidade).orElse(0);
        return Math.max(limiteDiario - usadas, 0);
    }

    /** Conta uma mensagem respondida. Falhas do provedor não gastam a cota do usuário. */
    @Transactional
    public int registrar(User usuario) {
        LocalDate hoje = Datas.hoje();
        NinoUso uso = repository.findByUsuarioAndDia(usuario, hoje)
                .orElseGet(() -> NinoUso.builder().usuario(usuario).dia(hoje).build());
        uso.setQuantidade(uso.getQuantidade() + 1);
        repository.save(uso);
        return Math.max(limiteDiario - uso.getQuantidade(), 0);
    }
}
