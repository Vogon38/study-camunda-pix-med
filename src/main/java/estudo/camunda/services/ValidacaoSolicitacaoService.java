package estudo.camunda.services;

import estudo.camunda.dto.ResultadoValidacao;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;

public interface ValidacaoSolicitacaoService {

    ResultadoValidacao validarSolicitacao(SolicitacaoDevolucaoRequest solicitacao);

}
