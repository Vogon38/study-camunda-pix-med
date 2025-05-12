package estudo.camunda.controller;

import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucaoPixControllerTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ProcessInstance processInstance;

    @InjectMocks
    private DevolucaoPixController controller;

    @Test
    @DisplayName("Deve iniciar processo de devolução com sucesso")
    void deveIniciarProcessoDeDevolucaoComSucesso() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        when(processInstance.getId()).thenReturn("PROCESS-ID-123");
        when(runtimeService.startProcessInstanceByKey(eq("processo_devolucao_pix_med_simplificado"), anyMap()))
                .thenReturn(processInstance);

        // Act
        ResponseEntity<String> response = controller.solicitarDevolucao(solicitacao);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().contains("Solicitação de devolução para PIX ID 'TXID_TESTE' recebida"));
        assertTrue(response.getBody().contains("PROCESS-ID-123"));

        // Verificar que as variáveis corretas foram passadas para o processo
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("processo_devolucao_pix_med_simplificado"), variablesCaptor.capture());
        
        Map<String, Object> variables = variablesCaptor.getValue();
        assertEquals(solicitacao, variables.get("solicitacaoDevolucaoRequest"));
        assertEquals("TXID_TESTE", variables.get("idTransacaoOriginal"));
        assertEquals("11122233344", variables.get("cpfPagadorOriginal"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando solicitação é inválida")
    void deveRetornarErro400QuandoSolicitacaoInvalida() {
        // Arrange
        when(runtimeService.startProcessInstanceByKey(anyString(), anyMap()))
                .thenThrow(new IllegalArgumentException("ID da transação original não pode ser nulo ou vazio."));

        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        // Act
        ResponseEntity<String> response = controller.solicitarDevolucao(solicitacao);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Dados inválidos na solicitação"));
    }

    @Test
    @DisplayName("Deve retornar erro 500 quando ocorre erro interno")
    void deveRetornarErro500QuandoOcorreErroInterno() {
        // Arrange
        when(runtimeService.startProcessInstanceByKey(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Erro interno de teste"));

        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        // Act
        ResponseEntity<String> response = controller.solicitarDevolucao(solicitacao);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Erro interno ao processar a solicitação de devolução"));
    }
}