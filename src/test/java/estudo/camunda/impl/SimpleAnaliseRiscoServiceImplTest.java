package estudo.camunda.impl;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoAnaliseRisco;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SimpleAnaliseRiscoServiceImplTest {

    private final SimpleAnaliseRiscoServiceImpl analiseRiscoService = new SimpleAnaliseRiscoServiceImpl();

    @Test
    @DisplayName("Deve classificar como alto risco quando valor acima do limite")
    void deveClassificarComoAltoRiscoQuandoValorAcimaDoLimite() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "COBRANCA_INDEVIDA",
                "11122233344"
        );
        
        DetalhesTransacaoPix transacao = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("1500.00"), // Valor acima do limite de R$1000.00
                "11122233344",
                "Cliente Teste",
                "55566677788",
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        // Act
        ResultadoAnaliseRisco resultado = analiseRiscoService.analisarRisco(solicitacao, transacao);

        // Assert
        assertEquals("ALTO", resultado.nivelRisco());
        assertFalse(resultado.aprovacaoAutomaticaSugerida());
        assertTrue(resultado.justificativa().contains("Valor da transação"));
        assertTrue(resultado.justificativa().contains("acima do limite"));
    }

    @Test
    @DisplayName("Deve classificar como médio risco quando motivo é fraude comprovada")
    void deveClassificarComoMedioRiscoQuandoMotivoFraudeComprovada() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );
        
        DetalhesTransacaoPix transacao = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("500.00"), // Valor abaixo do limite de alto risco
                "11122233344",
                "Cliente Teste",
                "55566677788",
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        // Act
        ResultadoAnaliseRisco resultado = analiseRiscoService.analisarRisco(solicitacao, transacao);

        // Assert
        assertEquals("MEDIO", resultado.nivelRisco());
        assertFalse(resultado.aprovacaoAutomaticaSugerida());
        assertTrue(resultado.justificativa().contains("FRAUDE_COMPROVADA"));
    }

    @Test
    @DisplayName("Deve classificar como baixo risco e sugerir aprovação automática para falha operacional com valor baixo")
    void deveClassificarComoBaixoRiscoEAprovarAutomaticamenteParaFalhaOperacionalComValorBaixo() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FALHA_OPERACIONAL_BANCO",
                "11122233344"
        );
        
        DetalhesTransacaoPix transacao = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("30.00"), // Valor abaixo do limite de R$50.00 para falha operacional
                "11122233344",
                "Cliente Teste",
                "55566677788",
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        // Act
        ResultadoAnaliseRisco resultado = analiseRiscoService.analisarRisco(solicitacao, transacao);

        // Assert
        assertEquals("BAIXO", resultado.nivelRisco());
        assertTrue(resultado.aprovacaoAutomaticaSugerida());
        assertTrue(resultado.justificativa().contains("FALHA_OPERACIONAL_BANCO"));
        assertTrue(resultado.justificativa().contains("valor baixo"));
    }

    @Test
    @DisplayName("Deve classificar como médio risco para casos padrão")
    void deveClassificarComoMedioRiscoParaCasosPadrao() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "COBRANCA_INDEVIDA",
                "11122233344"
        );
        
        DetalhesTransacaoPix transacao = new DetalhesTransacaoPix(
                "TXID_TESTE",
                new BigDecimal("500.00"), // Valor médio
                "11122233344",
                "Cliente Teste",
                "55566677788",
                "Comercio Teste",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"
        );

        // Act
        ResultadoAnaliseRisco resultado = analiseRiscoService.analisarRisco(solicitacao, transacao);

        // Assert
        assertEquals("MEDIO", resultado.nivelRisco());
        assertFalse(resultado.aprovacaoAutomaticaSugerida());
        assertTrue(resultado.justificativa().contains("Análise padrão"));
    }
}