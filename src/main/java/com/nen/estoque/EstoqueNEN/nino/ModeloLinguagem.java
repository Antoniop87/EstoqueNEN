package com.nen.estoque.EstoqueNEN.nino;

import java.util.List;
import java.util.Map;

/** Provedor do modelo de linguagem. Trocar de provedor é implementar esta interface. */
public interface ModeloLinguagem {

    boolean configurado();

    /** @throws ModeloIndisponivelException se o provedor falhar ou recusar a chamada */
    Resposta conversar(List<Mensagem> mensagens, List<Ferramenta> ferramentas);

    /** @param parametros JSON Schema dos argumentos */
    record Ferramenta(String nome, String descricao, Map<String, Object> parametros) {
    }

    /** @param argumentos JSON gerado pelo modelo; não é confiável até ser validado */
    record Chamada(String id, String nome, String argumentos) {
    }

    record Resposta(String texto, List<Chamada> chamadas) {
    }

    /**
     * @param papel     system, user, assistant ou tool
     * @param chamadas  ferramentas pedidas pelo modelo (só em assistant)
     * @param idChamada chamada que esta mensagem responde (só em tool)
     */
    record Mensagem(String papel, String conteudo, List<Chamada> chamadas, String idChamada) {

        public static Mensagem de(String papel, String conteudo) {
            return new Mensagem(papel, conteudo, List.of(), null);
        }

        public static Mensagem doModelo(Resposta resposta) {
            return new Mensagem("assistant", resposta.texto(), resposta.chamadas(), null);
        }

        public static Mensagem daFerramenta(String idChamada, String resultado) {
            return new Mensagem("tool", resultado, List.of(), idChamada);
        }
    }

    class ModeloIndisponivelException extends RuntimeException {
        public ModeloIndisponivelException(String mensagem) {
            super(mensagem);
        }
    }
}
