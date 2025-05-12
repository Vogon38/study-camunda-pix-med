package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoOperacaoFinanceira;
import estudo.camunda.services.CoreBankingService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("processamentoDevolucaoDelegate")
public class ProcessamentoDevolucaoDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoDevolucaoDelegate.class);

    private final CoreBankingService coreBankingService;

    @Autowired
    public ProcessamentoDevolucaoDelegate(CoreBankingService coreBankingService) {
        this.coreBankingService = coreBankingService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Executando ProcessamentoDevolucaoDelegate para o process instance ID: {}", execution.getProcessInstanceId());

        DetalhesTransacaoPix detalhesTransacaoOriginal =
                (DetalhesTransacaoPix) execution.getVariable("detalhesTransacaoOriginal");

        if (detalhesTransacaoOriginal == null) {
            handleErroDetalhesNaoEncontrados(execution);
            return;
        }

        String idOperacaoDevolucao = gerarIdOperacaoDevolucao();
        LOGGER.info("Preparando para efetuar devolução financeira: ID Operação '{}', Débito Conta '{}', Crédito Conta '{}', Valor R$ {}",
                idOperacaoDevolucao, detalhesTransacaoOriginal.getCpfCnpjRecebedor(),
                detalhesTransacaoOriginal.getCpfCnpjPagador(), detalhesTransacaoOriginal.getValor());

        ResultadoOperacaoFinanceira resultadoFinanceiro = coreBankingService.efetuarDevolucaoFinanceira(
                idOperacaoDevolucao,
                detalhesTransacaoOriginal.getCpfCnpjRecebedor(),
                detalhesTransacaoOriginal.getCpfCnpjPagador(),
                detalhesTransacaoOriginal.getValor()
        );

        processarResultadoFinanceiro(execution, idOperacaoDevolucao, resultadoFinanceiro);
    }

    private void handleErroDetalhesNaoEncontrados(DelegateExecution execution) {
        LOGGER.error("Variável 'detalhesTransacaoOriginal' não encontrada para processar a devolução financeira.");
        execution.setVariable("devolucaoFinanceiraEfetuada", false);
        execution.setVariable("mensagemResultadoFinanceiro", "Falha interna: Detalhes da transação original não encontrados para processamento financeiro.");
    }

    private String gerarIdOperacaoDevolucao() {
        return "DEV-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
    }

    private void processarResultadoFinanceiro(DelegateExecution execution, String idOperacaoDevolucao, ResultadoOperacaoFinanceira resultadoFinanceiro) {
        execution.setVariable("devolucaoFinanceiraEfetuada", resultadoFinanceiro.sucesso());
        execution.setVariable("mensagemResultadoFinanceiro", resultadoFinanceiro.mensagem());

        if (resultadoFinanceiro.sucesso()) {
            execution.setVariable("idTransacaoDevolucaoGerada", resultadoFinanceiro.idTransacaoDevolucao());
            LOGGER.info("Devolução financeira para ID Operação {} efetuada com sucesso. ID da Transação de Devolução: {}",
                    idOperacaoDevolucao, resultadoFinanceiro.idTransacaoDevolucao());
        } else {
            LOGGER.error("Falha ao efetuar a devolução financeira para ID Operação {}: {}",
                    idOperacaoDevolucao, resultadoFinanceiro.mensagem());
        }
    }

}