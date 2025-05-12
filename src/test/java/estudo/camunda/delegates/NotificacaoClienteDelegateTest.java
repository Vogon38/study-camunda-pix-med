package estudo.camunda.delegates;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.NotificacaoService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
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
class NotificacaoClienteDelegateTest {

    @BeforeEach
    void setUpMockito() {
        // Use lenient mode to avoid strict stubbing issues
        lenient().when(execution.getVariable("idTransacaoOriginal")).thenReturn(null);
    }

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private DelegateExecution execution;

    @Mock
    private Expression tipoNotificacao;

    private NotificacaoClienteDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new NotificacaoClienteDelegate(notificacaoService);
        delegate.setTipoNotificacao(tipoNotificacao);
    }

    @Test
    @DisplayName("Deve enviar notificação de rejeição inicial")
    void deveEnviarNotificacaoDeRejeicaoInicial() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        when(tipoNotificacao.getValue(execution)).thenReturn(NotificacaoClienteDelegate.TIPO_REJEICAO_INICIAL);
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(execution.getVariable("motivoInvalidacao")).thenReturn("Transação original não encontrada.");

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService).enviarNotificacao(
                eq("11122233344"),
                contains("sua solicitação de devolução para o PIX (ID Original: TXID_TESTE) não pôde ser aceita")
        );
    }

    @Test
    @DisplayName("Deve enviar notificação de rejeição após análise")
    void deveEnviarNotificacaoDeRejeicaoAposAnalise() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        when(tipoNotificacao.getValue(execution)).thenReturn(NotificacaoClienteDelegate.TIPO_REJEICAO_ANALISE);
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(execution.getVariable("motivoRejeicaoAnalista")).thenReturn("Documentação insuficiente para comprovar fraude.");

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService).enviarNotificacao(
                eq("11122233344"),
                contains("após análise, sua solicitação de devolução para o PIX (ID Original: TXID_TESTE) não pôde ser aprovada")
        );
    }

    @Test
    @DisplayName("Deve enviar notificação de processamento com sucesso")
    void deveEnviarNotificacaoDeProcessamentoComSucesso() {
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

        when(tipoNotificacao.getValue(execution)).thenReturn(NotificacaoClienteDelegate.TIPO_RESULTADO_PROCESSAMENTO);
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(detalhes);
        when(execution.getVariable("devolucaoFinanceiraEfetuada")).thenReturn(true);
        when(execution.getVariable("idTransacaoDevolucaoGerada")).thenReturn("DEV-123456789");

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService).enviarNotificacao(
                eq("11122233344"),
                contains("sua solicitação de devolução para o PIX (ID Original: TXID_TESTE) no valor de R$ 100,00 foi PROCESSADA COM SUCESSO")
        );
    }

    @Test
    @DisplayName("Deve enviar notificação de processamento com falha")
    void deveEnviarNotificacaoDeProcessamentoComFalha() {
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

        when(tipoNotificacao.getValue(execution)).thenReturn(NotificacaoClienteDelegate.TIPO_RESULTADO_PROCESSAMENTO);
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);
        when(execution.getVariable("detalhesTransacaoOriginal")).thenReturn(detalhes);
        when(execution.getVariable("devolucaoFinanceiraEfetuada")).thenReturn(false);
        when(execution.getVariable("mensagemResultadoFinanceiro")).thenReturn("Saldo insuficiente na conta do recebedor.");

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService).enviarNotificacao(
                eq("11122233344"),
                contains("houve um problema ao processar financeiramente sua solicitação de devolução para o PIX (ID Original: TXID_TESTE)")
        );
    }

    @Test
    @DisplayName("Não deve enviar notificação quando tipo de notificação é desconhecido")
    void naoDeveEnviarNotificacaoQuandoTipoNotificacaoDesconhecido() {
        // Arrange
        SolicitacaoDevolucaoRequest solicitacao = new SolicitacaoDevolucaoRequest(
                "TXID_TESTE",
                "FRAUDE_COMPROVADA",
                "11122233344"
        );

        when(tipoNotificacao.getValue(execution)).thenReturn("TIPO_DESCONHECIDO");
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(solicitacao);

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService, never()).enviarNotificacao(anyString(), anyString());
    }

    @Test
    @DisplayName("Não deve enviar notificação quando tipoNotificacao é nulo")
    void naoDeveEnviarNotificacaoQuandoTipoNotificacaoNulo() {
        // Arrange
        delegate.setTipoNotificacao(null);

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService, never()).enviarNotificacao(anyString(), anyString());
    }

    @Test
    @DisplayName("Não deve enviar notificação quando identificador do cliente não pode ser determinado")
    void naoDeveEnviarNotificacaoQuandoIdentificadorClienteNaoPodeSerDeterminado() {
        // Arrange
        when(tipoNotificacao.getValue(execution)).thenReturn(NotificacaoClienteDelegate.TIPO_REJEICAO_INICIAL);
        when(execution.getVariable("solicitacaoDevolucaoRequest")).thenReturn(null);
        when(execution.getVariable("cpfPagadorOriginal")).thenReturn(null);

        // Act
        delegate.execute(execution);

        // Assert
        verify(notificacaoService, never()).enviarNotificacao(anyString(), anyString());
    }
}
