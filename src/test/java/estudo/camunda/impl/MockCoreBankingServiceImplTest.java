package estudo.camunda.impl;

import estudo.camunda.dto.ResultadoOperacaoFinanceira;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockCoreBankingServiceImplTest {

    private final MockCoreBankingServiceImpl coreBankingService = new MockCoreBankingServiceImpl();

    @Test
    @DisplayName("Deve efetuar devolução financeira com sucesso quando há saldo suficiente")
    void deveEfetuarDevolucaoFinanceiraComSucessoQuandoHaSaldoSuficiente() {
        // Arrange
        String idOperacao = "OP_TESTE_001";
        String contaDebito = "55566677788"; // Conta com saldo de R$1000.00
        String contaCredito = "11122233344"; // Conta com saldo de R$200.00
        BigDecimal valor = new BigDecimal("500.00");

        // Act
        ResultadoOperacaoFinanceira resultado = coreBankingService.efetuarDevolucaoFinanceira(
                idOperacao, contaDebito, contaCredito, valor);

        // Assert
        assertTrue(resultado.sucesso());
        assertNotNull(resultado.mensagem());
        assertEquals(idOperacao, resultado.idTransacaoDevolucao());
        assertTrue(resultado.mensagem().contains("processada com sucesso"));
    }

    @Test
    @DisplayName("Deve falhar quando a conta de débito não tem saldo suficiente")
    void deveFalharQuandoContaDebitoNaoTemSaldoSuficiente() {
        // Arrange
        String idOperacao = "OP_TESTE_002";
        String contaDebito = "CONTA_SEM_SALDO_MOCK"; // Conta com saldo de R$5.00
        String contaCredito = "11122233344";
        BigDecimal valor = new BigDecimal("10.00");

        // Act
        ResultadoOperacaoFinanceira resultado = coreBankingService.efetuarDevolucaoFinanceira(
                idOperacao, contaDebito, contaCredito, valor);

        // Assert
        assertFalse(resultado.sucesso());
        assertNotNull(resultado.mensagem());
        assertNull(resultado.idTransacaoDevolucao());
        assertTrue(resultado.mensagem().contains("Saldo insuficiente"));
    }

    @Test
    @DisplayName("Deve falhar quando a conta de débito está bloqueada")
    void deveFalharQuandoContaDebitoEstaBloqueada() {
        // Arrange
        String idOperacao = "OP_TESTE_003";
        String contaDebito = "CONTA_BLOQUEADA_MOCK";
        String contaCredito = "11122233344";
        BigDecimal valor = new BigDecimal("50.00");

        // Act
        ResultadoOperacaoFinanceira resultado = coreBankingService.efetuarDevolucaoFinanceira(
                idOperacao, contaDebito, contaCredito, valor);

        // Assert
        assertFalse(resultado.sucesso());
        assertNotNull(resultado.mensagem());
        assertNull(resultado.idTransacaoDevolucao());
        assertTrue(resultado.mensagem().contains("bloqueada"));
    }

    @Test
    @DisplayName("Deve criar conta de crédito automaticamente se não existir")
    void deveCriarContaCreditoAutomaticamenteSeNaoExistir() {
        // Arrange
        String idOperacao = "OP_TESTE_004";
        String contaDebito = "55566677788"; // Conta com saldo de R$1000.00
        String contaCredito = "CONTA_NOVA_TESTE"; // Conta que não existe ainda
        BigDecimal valor = new BigDecimal("100.00");

        // Act
        ResultadoOperacaoFinanceira resultado = coreBankingService.efetuarDevolucaoFinanceira(
                idOperacao, contaDebito, contaCredito, valor);

        // Assert
        assertTrue(resultado.sucesso());
        assertNotNull(resultado.mensagem());
        assertEquals(idOperacao, resultado.idTransacaoDevolucao());
        
        // Verificar se a operação foi bem-sucedida fazendo uma nova operação
        // que usa a conta recém-criada como débito
        ResultadoOperacaoFinanceira segundaOperacao = coreBankingService.efetuarDevolucaoFinanceira(
                "OP_TESTE_004_VERIFICACAO", contaCredito, contaDebito, new BigDecimal("50.00"));
        
        assertTrue(segundaOperacao.sucesso());
    }
}