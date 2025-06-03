# Guia de Testes Camunda

Este documento fornece orientações sobre como testar processos BPM do Camunda neste projeto.

## Visão Geral

Testar processos Camunda é essencial para garantir que a lógica de negócios implementada nos diagramas BPMN funcione como esperado. Este projeto utiliza o framework de testes Camunda para testar os processos BPMN.

## Dependências

As seguintes dependências são necessárias para os testes Camunda:

```gradle
// Teste Camunda
testImplementation 'org.camunda.bpm.extension:camunda-bpm-junit5:1.1.0'
testImplementation 'org.camunda.bpm.assert:camunda-bpm-assert:15.0.0'
testImplementation 'org.camunda.bpm.extension.mockito:camunda-bpm-mockito:5.16.0'
``` 
Essas dependências já estão incluídas no arquivo build.gradle do projeto.

## Abordagens de Teste

### 1. Teste Simples de Processo

A abordagem mais simples é testar se um processo pode ser iniciado e se segue o caminho esperado. Isso é demonstrado em SimpleCamundaProcessTest.java.

Pontos chave:
- Injetar os serviços Camunda necessários (RuntimeService, TaskService, HistoryService)
- Iniciar uma instância de processo com variáveis específicas
- Aguardar a execução do processo
- Verificar se o processo terminou ou está aguardando em uma tarefa específica
- Checar se o processo seguiu o caminho esperado consultando o serviço de histórico

Exemplo:
```java
@SpringBootTest
public class SimpleCamundaProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Test
    public void testProcessPath() {
        // Preparar - Configurar variáveis do processo
        Map<String, Object> variables = new HashMap<>();
        variables.put("someVariable", "someValue");

        // Ação - Iniciar o processo
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("processDefinitionKey", variables);

        // Aguardar a execução do processo
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verificar - Confirmar o caminho do processo
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("expectedActivityId")
                .count();

        assertThat(count).isGreaterThan(0);
    }
}
```

### 2. Teste Abrangente de Processo

Para testes mais abrangentes, você pode testar diferentes caminhos pelo processo configurando diferentes variáveis e completando tarefas de usuário. Isso é demonstrado em CamundaProcessTest.java.

Pontos chave:
- Testar diferentes caminhos pelo processo
- Completar tarefas de usuário para simular a interação do usuário
- Verificar se as variáveis do processo estão configuradas corretamente
- Checar se o processo termina no evento final esperado

### 3. Mocking de Serviços

Para testes unitários de delegados e serviços, você pode usar Mockito para simular os serviços dos quais os delegados dependem. Isso é demonstrado nos testes de delegados existentes.

## Melhores Práticas

1. **Teste Cada Caminho**: Assegure-se de que cada caminho possível pelo processo seja testado.
2. **Use Variáveis Significativas**: Configure variáveis de processo que reflitam cenários do mundo real.
3. **Verifique a Execução de Atividades**: Confirme se as atividades esperadas foram executadas.
4. **Teste Tarefas de Usuário**: Se seu processo incluir tarefas de usuário, teste a conclusão delas com diferentes variáveis.
5. **Teste o Fim do Processo**: Teste o Fim do Processo

## Recursos

- [Documentação de Testes Camunda](https://docs.camunda.org/manual/latest/user-guide/testing/)
- [Camunda BPM Assert](https://github.com/camunda/camunda-bpm-assert)
- [Camunda BPM JUnit 5](https://github.com/camunda-community-hub/camunda-bpm-junit5)