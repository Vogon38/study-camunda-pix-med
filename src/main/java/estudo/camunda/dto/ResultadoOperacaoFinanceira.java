package estudo.camunda.dto;

public record ResultadoOperacaoFinanceira(
        boolean sucesso,
        String mensagem,
        String idTransacaoDevolucao
) {
    public static ResultadoOperacaoFinanceira sucesso(String idTransacaoDevolucao, String mensagem) {
        validateField(idTransacaoDevolucao, "ID da transação de devolução não pode ser nulo ou vazio para uma operação bem-sucedida.");
        return new ResultadoOperacaoFinanceira(true, mensagem, idTransacaoDevolucao);
    }

    public static ResultadoOperacaoFinanceira falha(String mensagem) {
        validateField(mensagem, "Mensagem não pode ser nula ou vazia para uma operação de falha.");
        return new ResultadoOperacaoFinanceira(false, mensagem, null);
    }

    private static void validateField(String field, String errorMessage) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

}
