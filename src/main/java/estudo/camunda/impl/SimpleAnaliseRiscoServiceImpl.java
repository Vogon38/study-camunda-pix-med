package estudo.camunda.impl;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoAnaliseRisco;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.AnaliseRiscoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SimpleAnaliseRiscoServiceImpl implements AnaliseRiscoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAnaliseRiscoServiceImpl.class);

    private static final BigDecimal VALOR_ALTO_RISCO = new BigDecimal("1000.00");
    private static final BigDecimal VALOR_BAIXO_RISCO_PARA_FALHA_OPERACIONAL = new BigDecimal("50.00");

    @Override
    public ResultadoAnaliseRisco analisarRisco(
            SolicitacaoDevolucaoRequest solicitacao,
            DetalhesTransacaoPix transacaoOriginal) {

        LOGGER.info("Iniciando análise de risco para transação ID: {} no valor de R$ {}",
                transacaoOriginal.getIdTransacao(), transacaoOriginal.getValor());

        String nivelRisco = "MEDIO";
        boolean aprovacaoAutomaticaSugerida = false;
        StringBuilder justificativaBuilder = new StringBuilder("Análise de risco: ");

        BigDecimal valorTransacao = transacaoOriginal.getValor();
        String motivo = solicitacao.motivo();

        if (valorTransacao.compareTo(VALOR_ALTO_RISCO) > 0) {
            nivelRisco = "ALTO";
            justificativaBuilder.append("Valor da transação (R$").append(valorTransacao)
                    .append(") acima do limite de R$").append(VALOR_ALTO_RISCO).append(". ");
        } else if ("FRAUDE_COMPROVADA".equalsIgnoreCase(motivo)) {
            justificativaBuilder.append("Motivo 'FRAUDE_COMPROVADA'. Requer atenção. ");
        } else if ("FALHA_OPERACIONAL_BANCO".equalsIgnoreCase(motivo) &&
                valorTransacao.compareTo(VALOR_BAIXO_RISCO_PARA_FALHA_OPERACIONAL) <= 0) {
            nivelRisco = "BAIXO";
            aprovacaoAutomaticaSugerida = true;
            justificativaBuilder.append("Motivo 'FALHA_OPERACIONAL_BANCO' com valor baixo (R$")
                    .append(valorTransacao).append("). ");
        } else {
            justificativaBuilder.append("Análise padrão. Sem regras específicas acionadas. ");
        }

        justificativaBuilder.append("Solicitante: ").append(solicitacao.cpfClienteSolicitante())
                .append(", Motivo: ").append(motivo)
                .append(", Valor: R$").append(valorTransacao);

        ResultadoAnaliseRisco resultado = new ResultadoAnaliseRisco(
                nivelRisco,
                aprovacaoAutomaticaSugerida,
                justificativaBuilder.toString()
        );

        LOGGER.info("Análise concluída para transação {}: Nível='{}', Sugestão Automática='{}'",
                transacaoOriginal.getIdTransacao(), nivelRisco, aprovacaoAutomaticaSugerida);

        return resultado;
    }

}
