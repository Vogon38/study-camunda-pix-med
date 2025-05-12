package estudo.camunda.delegates;

import estudo.camunda.dto.ResultadoValidacao;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.ValidacaoSolicitacaoService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("validacaoSolicitacaoDelegate")
public class ValidacaoSolicitacaoDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidacaoSolicitacaoDelegate.class);
    private final ValidacaoSolicitacaoService validacaoService;

    @Autowired
    public ValidacaoSolicitacaoDelegate(ValidacaoSolicitacaoService validacaoService) {
        this.validacaoService = validacaoService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Executando ValidacaoSolicitacaoDelegate para o process instance ID: {}", execution.getProcessInstanceId());

        SolicitacaoDevolucaoRequest solicitacaoRequest =
                (SolicitacaoDevolucaoRequest) execution.getVariable("solicitacaoDevolucaoRequest");

        if (solicitacaoRequest == null) {
            handleInvalidRequest(execution, "Dados da solicitação não fornecidos ao processo.");
            return;
        }

        ResultadoValidacao resultadoValidacao = validacaoService.validarSolicitacao(solicitacaoRequest);
        LOGGER.info("Resultado da validação do serviço: {}", resultadoValidacao);

        execution.setVariable("solicitacaoValida", resultadoValidacao.isValida());
        execution.setVariable("motivoInvalidacao", resultadoValidacao.isValida() ? null : resultadoValidacao.mensagemErro());

        if (resultadoValidacao.isValida()) {
            if (resultadoValidacao.detalhesTransacaoPix() != null) {
                execution.setVariable("detalhesTransacaoOriginal", resultadoValidacao.detalhesTransacaoPix());
                LOGGER.info("Variável de processo 'detalhesTransacaoOriginal' definida.");
            } else {
                LOGGER.error("Validação bem-sucedida, mas detalhesTransacaoOriginal é nulo no ResultadoValidacao. Verifique ValidacaoSolicitacaoServiceImpl.");
                handleInvalidRequest(execution, "Falha interna ao obter detalhes da transação após validação.");
                return;
            }
            LOGGER.info("Validação da solicitação bem-sucedida.");
        } else {
            LOGGER.warn("Validação da solicitação falhou: {}", resultadoValidacao.mensagemErro());
            execution.removeVariable("detalhesTransacaoOriginal");
        }
    }

    private void handleInvalidRequest(DelegateExecution execution, String mensagem) {
        LOGGER.error(mensagem);
        execution.setVariable("solicitacaoValida", false);
        execution.setVariable("motivoInvalidacao", mensagem);
        execution.removeVariable("detalhesTransacaoOriginal");
    }
}