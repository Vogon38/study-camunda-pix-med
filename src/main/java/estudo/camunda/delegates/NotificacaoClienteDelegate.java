package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.NotificacaoService;
import lombok.Setter;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component("notificacaoClienteDelegate")
public class NotificacaoClienteDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoClienteDelegate.class);

    public static final String TIPO_REJEICAO_INICIAL = "REJEICAO_INICIAL";
    public static final String TIPO_REJEICAO_ANALISE = "REJEICAO_ANALISE";
    public static final String TIPO_RESULTADO_PROCESSAMENTO = "RESULTADO_PROCESSAMENTO";

    private final NotificacaoService notificacaoService;

    @Setter
    private Expression tipoNotificacao;

    @Autowired
    public NotificacaoClienteDelegate(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    private <T> T getVariableAsType(DelegateExecution execution, String variableName, Class<T> expectedType) {
        Object variable = execution.getVariable(variableName);
        if (variable == null) {
            return null;
        }
        if (expectedType.isInstance(variable)) {
            return expectedType.cast(variable);
        } else {
            LOGGER.warn("Variable '{}' in execution id {} is not of expected type {}. Actual type: {}. Returning null.",
                    variableName, execution.getId(), expectedType.getName(), variable.getClass().getName());
            return null;
        }
    }

    private String getVariableAsString(DelegateExecution execution, String variableName) {
        return getVariableAsType(execution, variableName, String.class);
    }


    @Override
    public void execute(DelegateExecution execution) {
        String activityName = Optional.ofNullable(execution.getCurrentActivityName())
                .orElseGet(execution::getActivityInstanceId);
        LOGGER.info("Executando NotificacaoClienteDelegate para a atividade '{}' (Process Instance ID: {})",
                activityName, execution.getProcessInstanceId());

        if (this.tipoNotificacao == null) {
            LOGGER.error("Erro crítico: 'tipoNotificacao' não foi injetado para a atividade '{}' (Process Instance ID: {}). Verifique a configuração do Service Task no BPMN (Field Injection com name='tipoNotificacao').",
                    activityName, execution.getProcessInstanceId());
            return;
        }

        String tipoNotificacaoValor = "";
        try {
            Object value = this.tipoNotificacao.getValue(execution);
            if (value instanceof String) {
                tipoNotificacaoValor = ((String) value).trim();
            } else if (value != null) {
                LOGGER.warn("Field injection 'tipoNotificacao' (name='tipoNotificacao') para a atividade '{}' (Process Instance ID: {}) resolveu para um valor não-String: {}. Tipo: {}",
                        activityName, execution.getProcessInstanceId(), value, value.getClass().getName());
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao avaliar a expressão para 'tipoNotificacao' na atividade '{}' (Process Instance ID: {}): {}. Verifique a configuração no BPMN.",
                    activityName, execution.getProcessInstanceId(), e.getMessage(), e);
        }

        if (tipoNotificacaoValor.isEmpty()) {
            LOGGER.error("Erro crítico: O valor de 'tipoNotificacao' resolvido da expressão para a atividade '{}' (Process Instance ID: {}) é nulo, vazio ou não é uma String.",
                    activityName, execution.getProcessInstanceId());
            return;
        }

        LOGGER.info("Tipo de Notificação a ser processada para a atividade '{}': {}", activityName, tipoNotificacaoValor);

        SolicitacaoDevolucaoRequest solicitacaoRequest = getVariableAsType(execution, "solicitacaoDevolucaoRequest", SolicitacaoDevolucaoRequest.class);

        String identificadorCliente = Optional.ofNullable(solicitacaoRequest)
                .map(SolicitacaoDevolucaoRequest::cpfClienteSolicitante)
                .orElseGet(() -> getVariableAsString(execution, "cpfPagadorOriginal"));

        String idTransacaoOriginalParaLog = Optional.ofNullable(solicitacaoRequest)
                .map(SolicitacaoDevolucaoRequest::idTransacaoOriginal)
                .orElse(Optional.ofNullable(getVariableAsString(execution, "idTransacaoOriginal"))
                        .orElse("N/A"));

        if (identificadorCliente == null || identificadorCliente.isBlank()) {
            LOGGER.error("Não foi possível determinar o identificador do cliente para a notificação tipo '{}' da transação {} na atividade '{}' (Process Instance ID: {}).",
                    tipoNotificacaoValor, idTransacaoOriginalParaLog, activityName, execution.getProcessInstanceId());
            return;
        }

        String mensagemNotificacao = gerarMensagemNotificacao(tipoNotificacaoValor, execution, idTransacaoOriginalParaLog, activityName);

        if (mensagemNotificacao == null || mensagemNotificacao.isEmpty()) {
            LOGGER.warn("Tipo de notificação desconhecido ou não tratado: '{}' para a atividade '{}' (Process Instance ID: {}), ou falha ao gerar mensagem. Nenhuma notificação será enviada.",
                    tipoNotificacaoValor, activityName, execution.getProcessInstanceId());
            return;
        }

        notificacaoService.enviarNotificacao(identificadorCliente, mensagemNotificacao);
        LOGGER.info("Notificação (tipo: {}, atividade: '{}') enviada (simulada) para o cliente {}. Mensagem: {}",
                tipoNotificacaoValor, activityName, identificadorCliente, mensagemNotificacao);
    }

    private String gerarMensagemNotificacao(String tipoNotificacao, DelegateExecution execution, String idTransacaoOriginalParaLog, String activityName) {
        return switch (tipoNotificacao) {
            case TIPO_REJEICAO_INICIAL -> {
                String motivo = Optional.ofNullable(getVariableAsString(execution, "motivoInvalidacao")).orElse("Motivo não especificado");
                yield formatarMensagem("Prezado(a) cliente, sua solicitação de devolução para o PIX (ID Original: %s) não pôde ser aceita. Motivo: %s.",
                        idTransacaoOriginalParaLog, motivo);
            }
            case TIPO_REJEICAO_ANALISE -> {
                String motivo = Optional.ofNullable(getVariableAsString(execution, "motivoRejeicaoAnalista"))
                        .orElseGet(() -> Optional.ofNullable(getVariableAsString(execution, "justificativaAnaliseRisco")).orElse("Decisão da análise interna"));
                yield formatarMensagem("Prezado(a) cliente, após análise, sua solicitação de devolução para o PIX (ID Original: %s) não pôde ser aprovada. Motivo: %s.",
                        idTransacaoOriginalParaLog, motivo);
            }
            case TIPO_RESULTADO_PROCESSAMENTO ->
                    gerarMensagemResultadoProcessamento(execution, idTransacaoOriginalParaLog, activityName);
            default -> {
                LOGGER.warn("Tentativa de gerar mensagem para tipo de notificação desconhecido '{}' na atividade '{}'.", tipoNotificacao, activityName);
                yield "";
            }
        };
    }

    private String gerarMensagemResultadoProcessamento(DelegateExecution execution, String idTransacaoOriginalParaLog, String activityName) {
        DetalhesTransacaoPix transacaoOriginal = getVariableAsType(execution, "detalhesTransacaoOriginal", DetalhesTransacaoPix.class);
        Boolean devolucaoEfetuada = getVariableAsType(execution, "devolucaoFinanceiraEfetuada", Boolean.class);

        String idTransacaoEfetivo = Optional.ofNullable(transacaoOriginal)
                .map(DetalhesTransacaoPix::getIdTransacao)
                .orElse(idTransacaoOriginalParaLog);

        BigDecimal valorEfetivo = Optional.ofNullable(transacaoOriginal)
                .map(DetalhesTransacaoPix::getValor)
                .orElse(BigDecimal.ZERO);


        if (Boolean.TRUE.equals(devolucaoEfetuada)) {
             String idDevolucaoGerada = Optional.ofNullable(getVariableAsString(execution, "idTransacaoDevolucaoGerada")).orElse("N/A");
            return formatarMensagem("Prezado(a) cliente, sua solicitação de devolução para o PIX (ID Original: %s) no valor de R$ %.2f foi PROCESSADA COM SUCESSO. ID da transação de devolução: %s.",
                    idTransacaoEfetivo,
                    valorEfetivo,
                    idDevolucaoGerada);
        } else {
            String detalheFalha = Optional.ofNullable(getVariableAsString(execution, "mensagemResultadoFinanceiro")).orElse("Detalhe não informado");
            return formatarMensagem("Prezado(a) cliente, houve um problema ao processar financeiramente sua solicitação de devolução para o PIX (ID Original: %s). Detalhe: %s.",
                    idTransacaoEfetivo,
                    detalheFalha);
        }
    }

    private String formatarMensagem(String template, Object... args) {
        try {
            return String.format(template, args);
        } catch (Exception e) {
            LOGGER.error("Erro ao formatar mensagem de notificação. Template: '{}', Args: {}", template, args, e);
            return "Erro ao gerar mensagem de notificação. Por favor, contate o suporte.";
        }
    }
}