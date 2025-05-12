package estudo.camunda.impl;

import estudo.camunda.dto.ResultadoOperacaoFinanceira;
import estudo.camunda.services.CoreBankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MockCoreBankingServiceImpl implements CoreBankingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCoreBankingServiceImpl.class);

    private static final Map<String, BigDecimal> saldosContasMock = new HashMap<>(Map.of(
            "55566677788", new BigDecimal("1000.00"),
            "88899900011", new BigDecimal("500.00"),
            "11122233344", new BigDecimal("200.00"),
            "22233344455", new BigDecimal("300.00"),
            "CONTA_SEM_SALDO_MOCK", new BigDecimal("5.00")
    ));

    private static final String CONTA_BLOQUEADA_MOCK = "CONTA_BLOQUEADA_MOCK";

    @Override
    public ResultadoOperacaoFinanceira efetuarDevolucaoFinanceira(
            String idOperacaoDevolucao,
            String identificadorContaDebito,
            String identificadorContaCredito,
            BigDecimal valor) {

        LOGGER.info("Core Banking (Mock): Iniciando processamento financeiro para devolução ID {}", idOperacaoDevolucao);

        if (CONTA_BLOQUEADA_MOCK.equals(identificadorContaDebito)) {
            return logAndReturnFalha(idOperacaoDevolucao, "Conta de débito " + identificadorContaDebito + " está bloqueada.");
        }

        synchronized (saldosContasMock) {
            BigDecimal saldoContaDebito = saldosContasMock.getOrDefault(identificadorContaDebito, BigDecimal.ZERO);

            if (saldoContaDebito.compareTo(valor) < 0) {
                return logAndReturnFalha(idOperacaoDevolucao, String.format(
                        "Saldo insuficiente (R$ %.2f) na conta de débito %s para devolver R$ %.2f.",
                        saldoContaDebito, identificadorContaDebito, valor));
            }

            atualizarSaldo(identificadorContaDebito, saldoContaDebito.subtract(valor), "Débito");
            atualizarSaldo(identificadorContaCredito, saldosContasMock.getOrDefault(identificadorContaCredito, BigDecimal.ZERO).add(valor), "Crédito");
        }

        String mensagemSucesso = String.format("Devolução financeira %s de R$ %.2f processada com sucesso.", idOperacaoDevolucao, valor);
        LOGGER.info("Core Banking (Mock): {}", mensagemSucesso);
        return ResultadoOperacaoFinanceira.sucesso(idOperacaoDevolucao, mensagemSucesso);
    }

    private ResultadoOperacaoFinanceira logAndReturnFalha(String idOperacaoDevolucao, String mensagemFalha) {
        LOGGER.warn("Core Banking (Mock): {}", mensagemFalha);
        return ResultadoOperacaoFinanceira.falha(mensagemFalha);
    }

    private void atualizarSaldo(String identificadorConta, BigDecimal novoSaldo, String operacao) {
        BigDecimal saldoAnterior = saldosContasMock.getOrDefault(identificadorConta, BigDecimal.ZERO);
        BigDecimal valorOperacao = novoSaldo.subtract(saldoAnterior).abs();
        saldosContasMock.put(identificadorConta, novoSaldo);
        LOGGER.info("Core Banking (Mock): {} de R$ {} na conta {} realizado. Novo saldo: R$ {}",
                operacao, valorOperacao, identificadorConta, novoSaldo);
    }

}
