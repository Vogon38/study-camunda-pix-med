package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoAnaliseRisco;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.AnaliseRiscoService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("analiseRiscoDelegate")
public class AnaliseRiscoDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnaliseRiscoDelegate.class);

    private final AnaliseRiscoService analiseRiscoService;

    @Autowired
    public AnaliseRiscoDelegate(AnaliseRiscoService analiseRiscoService) {
        this.analiseRiscoService = analiseRiscoService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Executando AnaliseRiscoDelegate para o process instance ID: {}", execution.getProcessInstanceId());

        SolicitacaoDevolucaoRequest solicitacaoRequest =
                (SolicitacaoDevolucaoRequest) execution.getVariable("solicitacaoDevolucaoRequest");
        DetalhesTransacaoPix detalhesTransacaoOriginal =
                (DetalhesTransacaoPix) execution.getVariable("detalhesTransacaoOriginal");

        if (solicitacaoRequest == null || detalhesTransacaoOriginal == null) {
            String erro = solicitacaoRequest == null
                    ? "solicitacaoDevolucaoRequest"
                    : "detalhesTransacaoOriginal";
            LOGGER.error("Variável '{}' não encontrada para análise de risco.", erro);
            execution.setVariable("nivelRisco", "INDETERMINADO");
            execution.setVariable("aprovacaoAutomaticaSugerida", false);
            execution.setVariable("justificativaAnaliseRisco",
                    "Falha interna: Variável '" + erro + "' não encontrada.");
            return;
        }

        LOGGER.debug("Dados para análise de risco: Solicitacao={}, TransacaoOriginal={}",
                solicitacaoRequest, detalhesTransacaoOriginal);

        ResultadoAnaliseRisco resultadoAnalise =
                analiseRiscoService.analisarRisco(solicitacaoRequest, detalhesTransacaoOriginal);

        LOGGER.info("Resultado da análise de risco: Nível='{}', Sugestão Automática='{}', Justificativa='{}'",
                resultadoAnalise.nivelRisco(),
                resultadoAnalise.aprovacaoAutomaticaSugerida(),
                resultadoAnalise.justificativa());

        execution.setVariable("nivelRisco", resultadoAnalise.nivelRisco());
        execution.setVariable("aprovacaoAutomaticaSugerida", resultadoAnalise.aprovacaoAutomaticaSugerida());
        execution.setVariable("justificativaAnaliseRisco", resultadoAnalise.justificativa());
    }

}
