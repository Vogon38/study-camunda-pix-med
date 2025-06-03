# Projeto de Devolução PIX com Camunda & Spring Boot

## Funcionalidades

*   **API para Solicitações:** Endpoint `POST` para receber pedidos de devolução PIX de forma simples.
*   **Validações:**
    *   Verificação da existência da transação original.
    *   Confirmação da titularidade (solicitante vs. pagador original).
    *   Análise do prazo para devolução (79 dias).
    *   Validação do motivo da devolução.
*   **Análise de Risco Dinâmica:** Avalia o risco da solicitação baseado em critérios como valor e motivo, podendo sugerir aprovações automáticas para cenários de baixo risco.
*   **Orquestração com Camunda BPM:** Utiliza o Camunda para modelar e executar o workflow `processo_devolucao_pix_med_simplificado`.

## Stack

*   **Linguagem:** Java 21
*   **Framework:** Spring Boot
    *   Spring MVC
    *   Spring Data JPA
*   **Motor de Workflow:** Camunda BPM
*   **Build Tool:** Gradle
*   **Utilitários:** Lombok (para reduzir boilerplate), SLF4J (para logging)

## Pré-requisitos

*   JDK 21 ou superior instalado.
*   Gradle 7.x ou superior.

## Como Rodar o Projeto

1.  **Clone este universo:**
    ```bash
    git clone estudo-camunda-pix-med.git
    ```

2.  **Build com Gradle:**
    ```bash
    ./gradlew build
    ```
    *(No Windows, pode ser `gradlew build`)*

3.  **Inicie a aplicação Spring Boot:**
    ```bash
    ./gradlew bootRun
    ```
    Ou execute a classe principal `ServicoDevolucaoPixApplication.java` a partir da sua IDE.

🎉 A aplicação está em `http://localhost:8080` (ou a porta configurada no seu `application.properties`).

## Explorando a API

### Endpoint Principal: Solicitar Devolução

*   **Método:** `POST`
*   **URL:** `/api/v1/pix/devolucoes/solicitar`
*   **Content-Type:** `application/json`

*   **Corpo da Requisição (Exemplo):**
    ```json
    {
      "idTransacaoOriginal": "TXID_VALIDA_001",
      "motivo": "FRAUDE_COMPROVADA",
      "cpfClienteSolicitante": "11122233344"
    }
    ```

*   **Resposta de Sucesso (202 Accepted):**
    ```text
    "Solicitação de devolução para PIX ID 'TXID_VALIDA_001' recebida e processo iniciado. ID do Processo: <id-do-processo-camunda>"
    ```

*   **Respostas de Erro Comuns:**
    *   `400 Bad Request`: Se os dados da solicitação forem inválidos (campos faltando, formatos incorretos).
    *   `500 Internal Server Error`: Se algo inesperado acontecer no servidor.

🔑 **Dica:** Dê uma olhada no arquivo `postman_examples.md`! Tem exemplos para testar os cenários.

## Lógica de Negócio & Dados Mock (Simulados)

*   **`TXID_VALIDA_001`**: Válida, dentro do prazo, pronta para ação.
*   **`TXID_VALIDA_002`**: Um pouco mais "experiente" (90 dias).
*   **`TXID_PARA_ANALISE_MANUAL_001`**: Simula uma transação que cairá na análise manual.
*   **`TXID_RECEBEDOR_SEM_SALDO_006`**: Simula um recebedor sem saldo (para testes futuros de integração financeira).

**Regras:**
*   **Prazo MED:** 79 dias.
*   **Motivos Válidos:** `FRAUDE_COMPROVADA`, `FALHA_OPERACIONAL_BANCO`, `COBRANCA_INDEVIDA`.
*   **Análise de Risco (`SimpleAnaliseRiscoServiceImpl`):**
    *   Valor > R$1000.00 ➡️ Alto Risco.
    *   Motivo "FALHA\_OPERACIONAL\_BANCO" & Valor ≤ R$50.00 ➡️ Baixo Risco (sugestão de aprovação automática).

## Alterando o Desenho do Processo (BPMN)

O fluxo da devolução PIX é modelado como um diagrama BPMN, que o Camunda utiliza para executar o processo. O arquivo BPMN principal deste projeto é o `processo_devolucao_pix_med_simplificado.bpmn`.

**Onde encontrar o arquivo BPMN?**
Em `src/main/resources/`.

**Ferramenta Recomendada: Camunda Modeler**
A melhor forma de visualizar e editar diagramas BPMN para o Camunda é utilizando o **Camunda Modeler**. É uma ferramenta desktop gratuita.
*   **Download:** [Camunda Modeler](https://camunda.com/download/modeler/)

**Passos para Modificar o Diagrama:**

1.  **Abra o Camunda Modeler.**
2.  **Abra o arquivo BPMN:**
    *   Vá em `File > Open File...` e navegue até o arquivo `.bpmn` do seu projeto (ex: `src/main/resources/processo_devolucao_pix_med_simplificado.bpmn`).
3.  **Edite o Diagrama:**
    *   Você pode arrastar e soltar novos elementos da paleta (tarefas, gateways, eventos).
    *   Conecte os elementos para definir o fluxo.
    *   Configure as propriedades de cada elemento no painel "Properties Panel" à direita (IDs, nomes, implementações de service tasks, condições de gateways, etc.).
4.  **Salve as Alterações:**
    *   Vá em `File > Save File` ou use o atalho `Ctrl+S` (ou `Cmd+S` no Mac).
5.  **Importante: ID do Processo (Process Definition Key)**
    *   Ao desenhar seu processo, o elemento principal (o "pool" ou o diagrama em si) tem um `ID` (também chamado de "Process Definition Key").
    *   **Este ID DEVE corresponder à chave que sua aplicação Java usa para iniciar o processo.** No nosso caso, a chave é definida na classe `DevolucaoPixController.java`:
        ```java
        private static final String PROCESS_DEFINITION_KEY = "processo_devolucao_pix_med_simplificado";
        ```
    *   Se você alterar o ID no Camunda Modeler, lembre-se de atualizar esta constante no seu código Java!

6.  **Recompile e Reexecute:**
    Após salvar o BPMN e, se necessário, ajustar o código Java, recompile e reexecute sua aplicação Spring Boot para que o Camunda carregue a nova versão do processo.

    ```bash
    ./gradlew build
    ./gradlew bootRun
    ```
    
## Processo Camunda em Ação

O fluxo é definido pelo processo com a chave: `processo_devolucao_pix_med_simplificado`.
Para verificar as instâncias do processo, ver variáveis etc, acesse o **Camunda Cockpit**.
*   **URL Típica:** `http://localhost:8080/camunda/app/cockpit/`.

## Testando

*   **Testes Unitários (Java):**
    Testam métodos e classes isoladamente, como validações e regras de negócio. Veja exemplos em `src/test/java` (ex: `DevolucaoPixControllerTest.java`).

*   **Testes de API (Integração):**
    Validam os endpoints REST simulando requisições reais. Exemplos de uso estão em `postman_examples.md` (em `src/test/resources`).

*   **Testes de Processo Camunda (BPMN):**
    Garantem que o fluxo BPMN está correto e as integrações funcionam. Veja instruções em `tutorial_camunda_test.md` (em `src/test/resources`) e exemplos em `src/test/java` (`CamundaProcessTest.java`, `SimpleCamundaProcessTest.java`).

    Execute-os com Gradle:
    ```bash
    ./gradlew test
    ```
    
---

*Sinta-se à vontade para contribuir e melhorar este projeto!*
