package estudo.camunda.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogNotificacaoServiceImplTest {

    private final LogNotificacaoServiceImpl notificacaoService = new LogNotificacaoServiceImpl();

    @Test
    @DisplayName("Deve enviar notificação sem lançar exceções")
    void deveEnviarNotificacaoSemLancarExcecoes() {
        // Arrange
        String identificadorCliente = "11122233344";
        String mensagem = "Sua solicitação de devolução foi processada com sucesso.";

        // Act & Assert
        assertDoesNotThrow(() -> 
            notificacaoService.enviarNotificacao(identificadorCliente, mensagem)
        );
    }

    @Test
    @DisplayName("Deve aceitar identificador e mensagem vazios")
    void deveAceitarIdentificadorEMensagemVazios() {
        // Arrange
        String identificadorCliente = "";
        String mensagem = "";

        // Act & Assert
        assertDoesNotThrow(() -> 
            notificacaoService.enviarNotificacao(identificadorCliente, mensagem)
        );
    }

    @Test
    @DisplayName("Deve aceitar identificador e mensagem nulos")
    void deveAceitarIdentificadorEMensagemNulos() {
        // Arrange
        String identificadorCliente = null;
        String mensagem = null;

        // Act & Assert
        assertDoesNotThrow(() -> 
            notificacaoService.enviarNotificacao(identificadorCliente, mensagem)
        );
    }
}