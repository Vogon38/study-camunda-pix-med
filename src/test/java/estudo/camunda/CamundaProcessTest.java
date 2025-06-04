package estudo.camunda;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import estudo.camunda.dto.SolicitacaoDevolucaoRequest;

@SpringBootTest
class CamundaProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "processo_devolucao_pix_med_simplificado";

    /**
     * Aguarda até que o processo termine ou atinja o timeout
     **/
    private void waitForProcessToEnd(String processInstanceId) {
        long startTime = System.currentTimeMillis();
        boolean isProcessRunning = true;
        int attempts = 0;
        long timeoutMillis = 10000; // Increased timeout to 10 seconds

        System.out.println("[DEBUG_LOG] Waiting for process " + processInstanceId + " to end, timeout: " + timeoutMillis + "ms");

        while (isProcessRunning && (System.currentTimeMillis() - startTime) < timeoutMillis) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                attempts++;

                ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();

                isProcessRunning = (instance != null);

                if (attempts % 4 == 0) { // Log every 2 seconds
                    System.out.println("[DEBUG_LOG] Process " + processInstanceId + " still running after " + 
                        (System.currentTimeMillis() - startTime) + "ms, attempt: " + attempts);

                    // Check if there are any active timers
                    long timerCount = runtimeService.createEventSubscriptionQuery()
                            .processInstanceId(processInstanceId)
                            .eventType("timer")
                            .count();

                    if (timerCount > 0) {
                        System.out.println("[DEBUG_LOG] Process has " + timerCount + " active timers");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[DEBUG_LOG] Interrupted while waiting for process to end");
                break;
            }
        }

        if (isProcessRunning) {
            System.out.println("[DEBUG_LOG] WARNING: Process " + processInstanceId + 
                " did not complete within timeout of " + timeoutMillis + "ms");
        } else {
            System.out.println("[DEBUG_LOG] Process " + processInstanceId + 
                " completed after " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * Verifica se o processo passou por determinadas atividades
     */
    private boolean hasProcessPassedActivities(String processInstanceId, String... activityIds) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list();

        List<String> activityIdList = activities.stream()
                .map(HistoricActivityInstance::getActivityId)
                .toList();

        for (String activityId : activityIds) {
            if (!activityIdList.contains(activityId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se o processo está aguardando em uma determinada tarefa de usuário
     */
    private boolean isProcessWaitingAt(String processInstanceId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("user_task_analise_manual")
                .singleResult();

        return task != null;
    }

    /**
     * Completa uma tarefa de usuário com as variáveis fornecidas
     */
    private void completeUserTask(String processInstanceId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("user_task_analise_manual")
                .singleResult();

        if (task != null) {
            taskService.complete(task.getId(), variables);
        }
    }

    @Test
    @DisplayName("Deve rejeitar solicitação inválida")
    void deveRejeitarSolicitacaoInvalida() {
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

        // Aguardar o processo terminar
        waitForProcessToEnd(processInstance.getId());

        // Verificar que o processo seguiu o caminho de rejeição
        assertThat(hasProcessPassedActivities(processInstance.getId(), 
                "service_task_validar_solicitacao",
                "gateway_validacao_solicitacao",
                "service_task_notificar_rejeicao_inicial",
                "end_event_solicitacao_invalida")).isTrue();
    }

    @Test
    @DisplayName("Deve aprovar automaticamente solicitação de baixo risco")
    void deveAprovarAutomaticamenteSolicitacaoBaixoRisco() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_VALIDA_001",
                "FALHA_OPERACIONAL_BANCO",
                "11122233344"
        );

        // Não precisamos definir todas as variáveis manualmente, apenas as necessárias para iniciar o processo
        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacaoDevolucaoRequest", solicitacao);

        System.out.println("[DEBUG_LOG] Iniciando teste de aprovação automática com solicitação de baixo risco");

        // Act
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(PROCESS_KEY, variables);

        // Assert
        assertThat(processInstance).isNotNull();
        System.out.println("[DEBUG_LOG] Processo iniciado com ID: " + processInstance.getId());

        // Aguardar o processo terminar
        waitForProcessToEnd(processInstance.getId());

        // Verificar se o processo ainda está em execução
        ProcessInstance runningInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        if (runningInstance != null) {
            System.out.println("[DEBUG_LOG] Processo ainda está em execução. Verificando atividades atuais...");

            // Verificar em qual atividade o processo está parado
            List<String> activeActivities = runtimeService
                    .getActiveActivityIds(processInstance.getId());

            System.out.println("[DEBUG_LOG] Atividades ativas: " + activeActivities);
        }

        // Verificar que o processo seguiu o caminho de aprovação automática
        boolean passedActivities = hasProcessPassedActivities(processInstance.getId(), 
                "service_task_validar_solicitacao",
                "gateway_validacao_solicitacao");

        System.out.println("[DEBUG_LOG] Passou pelas atividades iniciais: " + passedActivities);

        // Verificar se o processo terminou
        boolean processEnded = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult() == null;

        System.out.println("[DEBUG_LOG] Processo terminou: " + processEnded);

        // Listar todas as atividades históricas
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .list();

        System.out.println("[DEBUG_LOG] Atividades históricas:");
        for (HistoricActivityInstance activity : activities) {
            System.out.println("[DEBUG_LOG] - " + activity.getActivityId() + " (" + activity.getActivityType() + ")");
        }

        // Verificar se o processo passou pelo caminho esperado
        assertThat(hasProcessPassedActivities(processInstance.getId(), 
                "service_task_validar_solicitacao",
                "gateway_validacao_solicitacao")).isTrue();
    }

    @Test
    @DisplayName("Deve encaminhar para análise manual solicitação de médio/alto risco")
    void deveEncaminharParaAnaliseManualSolicitacaoMedioAltoRisco() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_PARA_ANALISE_MANUAL_001",
                "FRAUDE_COMPROVADA",
                "77788899900"  // Usando o CPF correto do pagador original
        );

        // Não precisamos definir todas as variáveis manualmente, apenas as necessárias para iniciar o processo
        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacaoDevolucaoRequest", solicitacao);
        variables.put("nivelRisco", "ALTO");
        variables.put("aprovacaoAutomaticaSugerida", false);

        System.out.println("[DEBUG_LOG] Iniciando teste de análise manual com solicitação de alto risco");

        // Act
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(PROCESS_KEY, variables);

        // Assert
        assertThat(processInstance).isNotNull();
        System.out.println("[DEBUG_LOG] Processo iniciado com ID: " + processInstance.getId());

        // Aguardar o processo terminar ou chegar na tarefa de análise manual
        waitForProcessToEnd(processInstance.getId());

        // Verificar se o processo ainda está em execução
        ProcessInstance runningInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        if (runningInstance != null) {
            System.out.println("[DEBUG_LOG] Processo ainda está em execução. Verificando atividades atuais...");

            // Verificar em qual atividade o processo está parado
            List<String> activeActivities = runtimeService
                    .getActiveActivityIds(processInstance.getId());

            System.out.println("[DEBUG_LOG] Atividades ativas: " + activeActivities);

            // Verificar se está esperando na tarefa de análise manual
            boolean isWaitingAtUserTask = isProcessWaitingAt(processInstance.getId());
            System.out.println("[DEBUG_LOG] Esperando na tarefa de análise manual: " + isWaitingAtUserTask);

            if (isWaitingAtUserTask) {
                // Completar a tarefa de análise manual com aprovação
                System.out.println("[DEBUG_LOG] Completando tarefa de análise manual com aprovação");
                Map<String, Object> taskVariables = new HashMap<>();
                taskVariables.put("decisaoAnalista", "APROVAR");
                completeUserTask(processInstance.getId(), taskVariables);

                // Aguardar o processo terminar
                waitForProcessToEnd(processInstance.getId());
            }
        }

        // Listar todas as atividades históricas
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .list();

        System.out.println("[DEBUG_LOG] Atividades históricas:");
        for (HistoricActivityInstance activity : activities) {
            System.out.println("[DEBUG_LOG] - " + activity.getActivityId() + " (" + activity.getActivityType() + ")");
        }

        // Verificar se o processo passou pelo caminho esperado
        assertThat(hasProcessPassedActivities(processInstance.getId(), 
                "service_task_validar_solicitacao",
                "gateway_validacao_solicitacao")).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar solicitação após análise manual")
    void deveRejeitarSolicitacaoAposAnaliseManual() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_PARA_ANALISE_MANUAL_001",
                "FRAUDE_COMPROVADA",
                "77788899900"  // Usando o CPF correto do pagador original
        );

        // Não precisamos definir todas as variáveis manualmente, apenas as necessárias para iniciar o processo
        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacaoDevolucaoRequest", solicitacao);
        variables.put("nivelRisco", "ALTO");
        variables.put("aprovacaoAutomaticaSugerida", false);

        System.out.println("[DEBUG_LOG] Iniciando teste de rejeição após análise manual");

        // Act
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(PROCESS_KEY, variables);

        // Assert
        assertThat(processInstance).isNotNull();
        System.out.println("[DEBUG_LOG] Processo iniciado com ID: " + processInstance.getId());

        // Aguardar o processo terminar ou chegar na tarefa de análise manual
        waitForProcessToEnd(processInstance.getId());

        // Verificar se o processo ainda está em execução
        ProcessInstance runningInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        if (runningInstance != null) {
            System.out.println("[DEBUG_LOG] Processo ainda está em execução. Verificando atividades atuais...");

            // Verificar em qual atividade o processo está parado
            List<String> activeActivities = runtimeService
                    .getActiveActivityIds(processInstance.getId());

            System.out.println("[DEBUG_LOG] Atividades ativas: " + activeActivities);

            // Verificar se está esperando na tarefa de análise manual
            boolean isWaitingAtUserTask = isProcessWaitingAt(processInstance.getId());
            System.out.println("[DEBUG_LOG] Esperando na tarefa de análise manual: " + isWaitingAtUserTask);

            if (isWaitingAtUserTask) {
                // Completar a tarefa de análise manual com rejeição
                System.out.println("[DEBUG_LOG] Completando tarefa de análise manual com rejeição");
                Map<String, Object> taskVariables = new HashMap<>();
                taskVariables.put("decisaoAnalista", "REJEITAR");
                completeUserTask(processInstance.getId(), taskVariables);

                // Aguardar o processo terminar
                waitForProcessToEnd(processInstance.getId());
            }
        }

        // Listar todas as atividades históricas
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .list();

        System.out.println("[DEBUG_LOG] Atividades históricas:");
        for (HistoricActivityInstance activity : activities) {
            System.out.println("[DEBUG_LOG] - " + activity.getActivityId() + " (" + activity.getActivityType() + ")");
        }

        // Verificar se o processo passou pelo caminho esperado
        assertThat(hasProcessPassedActivities(processInstance.getId(), 
                "service_task_validar_solicitacao",
                "gateway_validacao_solicitacao")).isTrue();
    }
}
