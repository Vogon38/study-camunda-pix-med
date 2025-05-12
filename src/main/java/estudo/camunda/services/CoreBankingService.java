package estudo.camunda.services;

import estudo.camunda.dto.ResultadoOperacaoFinanceira;

import java.math.BigDecimal;

public interface CoreBankingService {

    ResultadoOperacaoFinanceira efetuarDevolucaoFinanceira(
            String idOperacaoDevolucao,
            String identificadorContaDebito,
            String identificadorContaCredito,
            BigDecimal valor
    );

}
