package estudo.camunda.services;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoAnaliseRisco;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;

public interface AnaliseRiscoService {

    ResultadoAnaliseRisco analisarRisco(
            SolicitacaoDevolucaoRequest solicitacao,
            DetalhesTransacaoPix transacaoOriginal
    );

}
