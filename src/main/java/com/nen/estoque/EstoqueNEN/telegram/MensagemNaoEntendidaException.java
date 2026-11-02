package com.nen.estoque.EstoqueNEN.telegram;

/** A mensagem não pôde ser interpretada; a mensagem da exceção é mostrada ao usuário. */
public class MensagemNaoEntendidaException extends RuntimeException {

    public MensagemNaoEntendidaException(String message) {
        super(message);
    }
}
