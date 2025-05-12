package estudo.camunda.impl;

import estudo.camunda.dto.DetalhesTransacaoPix;
import estudo.camunda.dto.ResultadoValidacao;
import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import estudo.camunda.services.ValidacaoSolicitacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ValidacaoSolicitacaoServiceImpl implements ValidacaoSolicitacaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidacaoSolicitacaoServiceImpl.class);

    private static final long PRAZO_MAXIMO_MED_DIAS = 79;

    private static final Set<String> MOTIVOS_ACEITAVEIS_MED = Set.of(
            "FRAUDE_COMPROVADA",
            "FALHA_OPERACIONAL_BANCO",
            "COBRANCA_INDEVIDA"
    );

    private static final Map<String, DetalhesTransacaoPix> repositorioTransacoesMock;

    static {
        Map<String, DetalhesTransacaoPix> aMap = new HashMap<>();
        aMap.put("TXID_VALIDA_001", new DetalhesTransacaoPix(
                "TXID_VALIDA_001", new BigDecimal("100.00"),
                "11122233344", "Cliente Pagador Um",
                "55566677788", "Comercio Recebedor A",
                LocalDateTime.now().minusDays(10), "CONCLUIDA"));
        aMap.put("TXID_VALIDA_002", new DetalhesTransacaoPix(
                "TXID_VALIDA_002", new BigDecimal("50.50"),
                "22233344455", "Cliente Pagador Dois",
                "88899900011", "Serviço Recebedor B",
                LocalDateTime.now().minusDays(90), "CONCLUIDA"));
        aMap.put("TXID_INVALIDA_PAGADOR", new DetalhesTransacaoPix(
                "TXID_INVALIDA_PAGADOR", new BigDecimal("75.00"),
                "99988877766", "Outro Pagador",
                "11122233344", "Comercio Recebedor C",
                LocalDateTime.now().minusDays(5), "CONCLUIDA"));
        aMap.put("TXID_PARA_ANALISE_MANUAL_001", new DetalhesTransacaoPix(
                "TXID_PARA_ANALISE_MANUAL_001", new BigDecimal("250.75"),
                "77788899900", "Cliente Pagador Manual",
                "33344455566", "Loja Recebedora Manual",
                LocalDateTime.now().minusDays(20), "CONCLUIDA"));
        aMap.put("TXID_RECEBEDOR_SEM_SALDO_006", new DetalhesTransacaoPix(
                "TXID_RECEBEDOR_SEM_SALDO_006",
                new BigDecimal("10.00"),
                "66677788899",
                "Cliente Pagador Saldo Teste",
                "CONTA_SEM_SALDO_MOCK",
                "Comércio Azarado",
                LocalDateTime.now().minusDays(5),
                "CONCLUIDA"));

        repositorioTransacoesMock = Map.copyOf(aMap);
    }

    @Override
    public ResultadoValidacao validarSolicitacao(SolicitacaoDevolucaoRequest solicitacao) {
        LOGGER.info("Iniciando validação para a solicitação da transação original ID: {}", solicitacao.idTransacaoOriginal());
        return buscarTransacaoOriginal(solicitacao.idTransacaoOriginal())
                .map(transacao -> validarDetalhesSolicitacao(solicitacao, transacao))
                .orElseGet(() -> {
                    LOGGER.warn("Validação falhou: Transação original ID {} não encontrada.", solicitacao.idTransacaoOriginal());
                    return ResultadoValidacao.falha("Transação original não encontrada.");
                });
    }

    private ResultadoValidacao validarDetalhesSolicitacao(SolicitacaoDevolucaoRequest solicitacao, DetalhesTransacaoPix transacaoOriginal) {
        LOGGER.debug("Validando detalhes para solicitacao: {} e transacaoOriginal: {}", solicitacao, transacaoOriginal);

        if (!solicitacao.cpfClienteSolicitante().equals(transacaoOriginal.getCpfCnpjPagador())) {
            return logFalha(String.format("Solicitante (CPF: %s) não é o pagador original (CPF: %s) da transação %s.",
                    solicitacao.cpfClienteSolicitante(), transacaoOriginal.getCpfCnpjPagador(), solicitacao.idTransacaoOriginal()));
        }

        long diasDesdeTransacao = ChronoUnit.DAYS.between(transacaoOriginal.getDataHoraTransacao(), LocalDateTime.now());
        if (diasDesdeTransacao > PRAZO_MAXIMO_MED_DIAS) {
            return logFalha(String.format("Solicitação para transação %s (%d dias) fora do prazo de %d dias para MED.",
                    solicitacao.idTransacaoOriginal(), diasDesdeTransacao, PRAZO_MAXIMO_MED_DIAS));
        }

        if (!MOTIVOS_ACEITAVEIS_MED.contains(solicitacao.motivo().toUpperCase())) {
            return logFalha(String.format("Motivo da devolução '%s' para transação %s inválido ou não coberto pelo MED.",
                    solicitacao.motivo(), solicitacao.idTransacaoOriginal()));
        }

        LOGGER.info("Validação da solicitação para transação {} concluída com sucesso.", solicitacao.idTransacaoOriginal());
        return ResultadoValidacao.sucesso(transacaoOriginal);
    }

    private ResultadoValidacao logFalha(String mensagem) {
        LOGGER.warn("Validação falhou: {}", mensagem);
        return ResultadoValidacao.falha(mensagem);
    }

    private Optional<DetalhesTransacaoPix> buscarTransacaoOriginal(String idTransacaoOriginal) {
        LOGGER.debug("Buscando (mock) transação original com ID: {}", idTransacaoOriginal);
        return Optional.ofNullable(repositorioTransacoesMock.get(idTransacaoOriginal));
    }

}
