package estudo.camunda.dto;

import java.util.Objects;

public record ResultadoValidacao(
        boolean isValida,
        String mensagemErro,
        DetalhesTransacaoPix detalhesTransacaoPix
) {
    public static ResultadoValidacao sucesso() {
        return new ResultadoValidacao(true, null, null);
    }

    public static ResultadoValidacao sucesso(DetalhesTransacaoPix detalhes) {
        return new ResultadoValidacao(true, null, detalhes);
    }

    public static ResultadoValidacao falha(String mensagem) {
        return new ResultadoValidacao(false,
                Objects.requireNonNull(mensagem, "Mensagem de erro não pode ser nula ou vazia para um resultado de falha."),
                null);
    }
}