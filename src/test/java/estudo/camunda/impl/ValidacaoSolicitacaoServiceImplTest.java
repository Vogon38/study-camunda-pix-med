package estudo.camunda.impl;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoValidacao;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidacaoSolicitacaoServiceImplTest {

    private final ValidacaoSolicitacaoServiceImpl validacaoService = new ValidacaoSolicitacaoServiceImpl();

    @Test
    @DisplayName("Deve validar com sucesso uma solicitação válida")
    void deveValidarComSucessoSolicitacaoValida() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_VALIDA_001",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        // Act
        ResultadoValidacao resultado = validacaoService.validarSolicitacao(solicitacao);

        // Assert
        assertTrue(resultado.isValida());
        assertNull(resultado.mensagemErro());
        assertNotNull(resultado.detalhesTransacaoPix());
        assertEquals("TXID_VALIDA_001", resultado.detalhesTransacaoPix().getIdTransacao());
        assertEquals("11122233344", resultado.detalhesTransacaoPix().getCpfCnpjPagador());
    }

    @Test
    @DisplayName("Deve falhar quando a transação não existe")
    void deveFalharQuandoTransacaoNaoExiste() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_INEXISTENTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        // Act
        ResultadoValidacao resultado = validacaoService.validarSolicitacao(solicitacao);

        // Assert
        assertFalse(resultado.isValida());
        assertEquals("Transação original não encontrada.", resultado.mensagemErro());
        assertNull(resultado.detalhesTransacaoPix());
    }

    @Test
    @DisplayName("Deve falhar quando o solicitante não é o pagador original")
    void deveFalharQuandoSolicitanteNaoEPagadorOriginal() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_VALIDA_001",
                "FRAUDE_COMPROVADA",
                "99999999999" // CPF diferente do pagador original
        );

        // Act
        ResultadoValidacao resultado = validacaoService.validarSolicitacao(solicitacao);

        // Assert
        assertFalse(resultado.isValida());
        assertTrue(resultado.mensagemErro().contains("não é o pagador original"));
        assertNull(resultado.detalhesTransacaoPix());
    }

    @Test
    @DisplayName("Deve falhar quando a transação está fora do prazo")
    void deveFalharQuandoTransacaoForaDoPrazo() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_VALIDA_002", // Transação com mais de 79 dias
                "FRAUDE_COMPROVADA",
                "22233344455"
        );

        // Act
        ResultadoValidacao resultado = validacaoService.validarSolicitacao(solicitacao);

        // Assert
        assertFalse(resultado.isValida());
        assertTrue(resultado.mensagemErro().contains("fora do prazo"));
        assertNull(resultado.detalhesTransacaoPix());
    }

    @Test
    @DisplayName("Deve falhar quando o motivo é inválido")
    void deveFalharQuandoMotivoInvalido() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_VALIDA_001",
                "MOTIVO_INVALIDO",
                "11122233344"
        );

        // Act
        ResultadoValidacao resultado = validacaoService.validarSolicitacao(solicitacao);

        // Assert
        assertFalse(resultado.isValida());
        assertTrue(resultado.mensagemErro().contains("Motivo da devolução"));
        assertNull(resultado.detalhesTransacaoPix());
    }
}