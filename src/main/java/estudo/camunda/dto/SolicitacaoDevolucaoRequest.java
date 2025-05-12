package estudo.camunda.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record SolicitacaoDevolucaoRequest(
        String idTransacaoOriginal,
        String motivo,
        String cpfClienteSolicitante
) implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public SolicitacaoDevolucaoRequest {
        validateField(idTransacaoOriginal, "O ID da transação original não pode ser nulo ou vazio.");
        validateField(motivo, "O motivo da devolução não pode ser nulo ou vazio.");
        validateField(cpfClienteSolicitante, "O CPF do cliente solicitante não pode ser nulo ou vazio.");
    }

    private static void validateField(String field, String errorMessage) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

}
