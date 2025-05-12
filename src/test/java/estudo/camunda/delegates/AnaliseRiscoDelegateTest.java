package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoAnaliseRisco;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.AnaliseRiscoService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnaliseRiscoDelegateTest {

    @Mock
    private AnaliseRiscoService analiseRiscoService;

    @Mock
    private DelegateExecution execution;

    private AnaliseRiscoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new AnaliseRiscoDelegate(analiseRiscoService);
    }

    @Test
    @DisplayName("Deve processar análise de risco corretamente")
    void deveProcessarAnaliseRiscoCorretamente() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        DetalhesTransacaoPix detalhes = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("100.00"),
                "11122233344",
                "Cliente Teste",
                "55566677788",
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        ResultadoAnaliseRisco resultadoAnalise = new ResultadoAnaliseRisco(
                "MEDIO",
                false,
                "Análise de risco: Motivo 'FRAUDE_COMPROVADA'. Requer atenção."
        );

        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(detalhes);
        when(analiseRiscoService.analisarRisco(solicitacao, detalhes)).thenReturn(resultadoAnalise);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("nivelRisco", "MEDIO");
        verify(execution).setVariable("aprovacaoAutomaticaSugerida", false);
        verify(execution).setVariable("justificativaAnaliseRisco", 
                "Análise de risco: Motivo 'FRAUDE_COMPROVADA'. Requer atenção.");
    }

    @Test
    @DisplayName("Deve processar análise de risco com aprovação automática")
    void deveProcessarAnaliseRiscoComAprovacaoAutomatica() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FALHA_OPERACIONAL_BANCO",
                "11122233344"
        );

        DetalhesTransacaoPix detalhes = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("30.00"),
                "11122233344",
                "Cliente Teste",
                "55566677788",
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        ResultadoAnaliseRisco resultadoAnalise = new ResultadoAnaliseRisco(
                "BAIXO",
                true,
                "Análise de risco: Motivo 'FALHA_OPERACIONAL_BANCO' com valor baixo (R$30.00)."
        );

        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(detalhes);
        when(analiseRiscoService.analisarRisco(solicitacao, detalhes)).thenReturn(resultadoAnalise);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("nivelRisco", "BAIXO");
        verify(execution).setVariable("aprovacaoAutomaticaSugerida", true);
        verify(execution).setVariable("justificativaAnaliseRisco", 
                "Análise de risco: Motivo 'FALHA_OPERACIONAL_BANCO' com valor baixo (R$30.00).");
    }

    @Test
    @DisplayName("Deve tratar solicitação nula corretamente")
    void deveTratarSolicitacaoNulaCorretamente() {
        // Arrange
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(null);
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(mock(DetalhesTransacaoPix.class));

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("nivelRisco", "INDETERMINADO");
        verify(execution).setVariable("aprovacaoAutomaticaSugerida", false);
        verify(execution).setVariable("justificativaAnaliseRisco", 
                "Falha interna: Variável 'solicitacaoDevolucaoRequest' não encontrada.");
        verify(analiseRiscoService, never()).analisarRisco(any(), any());
    }

    @Test
    @DisplayName("Deve tratar detalhes da transação nulos corretamente")
    void deveTratarDetalhesTransacaoNulosCorretamente() {
        // Arrange
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(mock(SolicitacaoDevolucaoRequest.class));
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(null);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("nivelRisco", "INDETERMINADO");
        verify(execution).setVariable("aprovacaoAutomaticaSugerida", false);
        verify(execution).setVariable("justificativaAnaliseRisco", 
                "Falha interna: Variável 'detalhesTransacaoOriginal' não encontrada.");
        verify(analiseRiscoService, never()).analisarRisco(any(), any());
    }
}