package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoValidacao;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.ValidacaoSolicitacaoService;
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
class ValidacaoSolicitacaoDelegateTest {

    @Mock
    private ValidacaoSolicitacaoService validacaoService;

    @Mock
    private DelegateExecution execution;

    private ValidacaoSolicitacaoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ValidacaoSolicitacaoDelegate(validacaoService);
    }

    @Test
    @DisplayName("Deve processar solicitação válida corretamente")
    void deveProcessarSolicitacaoValidaCorretamente() {
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

        ResultadoValidacao resultadoValidacao = ResultadoValidacao.sucesso(detalhes);

        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(validacaoService.validarSolicitacao(solicitacao)).thenReturn(resultadoValidacao);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("solicitacaoValida", true);
        verify(execution).setVariable("motivoInvalidacao", null);
        verify(execution).setVariable("detalhesTransacaoOriginal", detalhes);
        verify(execution, never()).removeVariable("detalhesTransacaoOriginal");
    }

    @Test
    @DisplayName("Deve processar solicitação inválida corretamente")
    void deveProcessarSolicitacaoInvalidaCorretamente() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_INEXISTENTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        ResultadoValidacao resultadoValidacao = ResultadoValidacao.falha("Transação original não encontrada.");

        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(validacaoService.validarSolicitacao(solicitacao)).thenReturn(resultadoValidacao);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("solicitacaoValida", false);
        verify(execution).setVariable("motivoInvalidacao", "Transação original não encontrada.");
        verify(execution, never()).setVariable(eq("detalhesTransacaoOriginal"), any());
        verify(execution).removeVariable("detalhesTransacaoOriginal");
    }

    @Test
    @DisplayName("Deve tratar solicitação nula corretamente")
    void deveTratarSolicitacaoNulaCorretamente() {
        // Arrange
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(null);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("solicitacaoValida", false);
        verify(execution).setVariable("motivoInvalidacao", "Dados da solicitação não fornecidos ao processo.");
        verify(execution, never()).setVariable(eq("detalhesTransacaoOriginal"), any());
        verify(execution).removeVariable("detalhesTransacaoOriginal");
        verify(validacaoService, never()).validarSolicitacao(any());
    }

    @Test
    @DisplayName("Deve tratar resultado de validação inconsistente")
    void deveTratarResultadoValidacaoInconsistente() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        // Resultado válido mas sem detalhes da transação (inconsistente)
        ResultadoValidacao resultadoValidacao = ResultadoValidacao.sucesso();

        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(validacaoService.validarSolicitacao(solicitacao)).thenReturn(resultadoValidacao);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("solicitacaoValida", false);
        verify(execution).setVariable("motivoInvalidacao", "Falha interna ao obter detalhes da transação após validação.");
        verify(execution, never()).setVariable(eq("detalhesTransacaoOriginal"), any());
        verify(execution).removeVariable("detalhesTransacaoOriginal");
    }
}