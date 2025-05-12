package estudo.camunda.dto;

public record ResultadoAnaliseRisco(
        String nivelRisco,
        boolean aprovacaoAutomaticaSugerida,
        String justificativa
) {
    public ResultadoAnaliseRisco {
        if (nivelRisco == null || nivelRisco.isBlank()) {
            throw new IllegalArgumentException("Nível de risco não pode ser nulo ou vazio.");
        }
    }

}
