package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoOperacaoFinanceira;
import estudo.camunda.services.CoreBankingService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoDevolucaoDelegateTest {

    @Mock
    private CoreBankingService coreBankingService;

    @Mock
    private DelegateExecution execution;

    private ProcessamentoDevolucaoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ProcessamentoDevolucaoDelegate(coreBankingService);
    }

    @Test
    @DisplayName("Deve processar devolução financeira com sucesso")
    void deveProcessarDevolucaoFinanceiraComSucesso() throws Exception {
        // Arrange
        DetalhesTransacaoPix detalhes = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("100.00"),
                "11122233344", // CPF do pagador
                "Cliente Teste",
                "55566677788", // CPF do recebedor
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(detalhes);
        
        // Capturar o ID da operação gerado
        ArgumentCaptor<String> idOperacaoCaptor = ArgumentCaptor.forClass(String.class);
        
        ResultadoOperacaoFinanceira resultadoFinanceiro = ResultadoOperacaoFinanceira.sucesso(
                "DEV-123456789", 
                "Devolução financeira processada com sucesso."
        );
        
        when(coreBankingService.efetuarDevolucaoFinanceira(
                idOperacaoCaptor.capture(),
                eq("55566677788"), // Conta débito (recebedor)
                eq("11122233344"), // Conta crédito (pagador)
                eq(new BigDecimal("100.00"))
        )).thenReturn(resultadoFinanceiro);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("devolucaoFinanceiraEfetuada", true);
        verify(execution).setVariable("mensagemResultadoFinanceiro", "Devolução financeira processada com sucesso.");
        verify(execution).setVariable("idTransacaoDevolucaoGerada", "DEV-123456789");
        
        // Verificar que o ID da operação foi gerado corretamente
        String idOperacaoGerado = idOperacaoCaptor.getValue();
        assertNotNull(idOperacaoGerado);
        assertTrue(idOperacaoGerado.startsWith("DEV-"));
    }

    @Test
    @DisplayName("Deve processar falha na devolução financeira")
    void deveProcessarFalhaNaDevolucaoFinanceira() throws Exception {
        // Arrange
        DetalhesTransacaoPix detalhes = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("100.00"),
                "11122233344", // CPF do pagador
                "Cliente Teste",
                "CONTA_SEM_SALDO_MOCK", // Conta sem saldo suficiente
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(detalhes);
        
        ResultadoOperacaoFinanceira resultadoFinanceiro = ResultadoOperacaoFinanceira.falha(
                "Saldo insuficiente na conta de débito CONTA_SEM_SALDO_MOCK para devolver R$ 100.00."
        );
        
        when(coreBankingService.efetuarDevolucaoFinanceira(
                anyString(),
                eq("CONTA_SEM_SALDO_MOCK"), // Conta débito (recebedor)
                eq("11122233344"), // Conta crédito (pagador)
                eq(new BigDecimal("100.00"))
        )).thenReturn(resultadoFinanceiro);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("devolucaoFinanceiraEfetuada", false);
        verify(execution).setVariable("mensagemResultadoFinanceiro", 
                "Saldo insuficiente na conta de débito CONTA_SEM_SALDO_MOCK para devolver R$ 100.00.");
        verify(execution, never()).setVariable(eq("idTransacaoDevolucaoGerada"), anyString());
    }

    @Test
    @DisplayName("Deve tratar detalhes da transação nulos")
    void deveTratarDetalhesTransacaoNulos() throws Exception {
        // Arrange
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(null);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("devolucaoFinanceiraEfetuada", false);
        verify(execution).setVariable("mensagemResultadoFinanceiro", 
                "Falha interna: Detalhes da transação original não encontrados para processamento financeiro.");
        verify(coreBankingService, never()).efetuarDevolucaoFinanceira(anyString(), anyString(), anyString(), any(BigDecimal.class));
    }
}