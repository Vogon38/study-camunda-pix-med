package estudo.camunda;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import estudo.camunda.dto.SolicitacaoDevolucaoRequest;

@SpringBootTest
class SimpleCamundaProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "processo_devolucao_pix_med_simplificado";

    @Test
    @DisplayName("Deve iniciar processo com solicitação inválida")
    void deveIniciarProcessoComSolicitacaoInvalida() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_INVALIDO",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacaoDevolucaoRequest", solicitacao);
        variables.put("solicitacaoValida", false);
        variables.put("motivoInvalidacao", "Transação original não encontrada");

        // Act
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(PROCESS_KEY, variables);

        // Assert
        assertThat(processInstance).isNotNull();

        // Aguardar um pouco para o processo executar
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verificar que o processo foi executado e finalizado
        ProcessInstance runningInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        // Como definimos solicitacaoValida=false, o processo deve ter terminado
        assertThat(runningInstance).isNull();

        // Verificar que o processo passou pelo caminho de rejeição
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("service_task_notificar_rejeicao_inicial")
                .count();

        assertThat(count).isGreaterThan(0);
    }
}
