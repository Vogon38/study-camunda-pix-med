package estudo.camunda.controller;

import estudo.camunda.dto.SolicitacaoDevolucaoRequest;
import jakarta.validation.Valid;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pix/devolucoes")
public class DevolucaoPixController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevolucaoPixController.class);

    private final RuntimeService runtimeService;

    private static final String PROCESS_DEFINITION_KEY = "processo_devolucao_pix_med_simplificado";

    @Autowired
    public DevolucaoPixController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("/solicitar")
    public ResponseEntity<String> solicitarDevolucao(@Valid @RequestBody SolicitacaoDevolucaoRequest solicitacaoRequest) {
        try {
            Map<String, Object> variables = Map.of(
                    "solicitacaoDevolucaoRequest", solicitacaoRequest,
                    "idTransacaoOriginal", solicitacaoRequest.idTransacaoOriginal(),
                    "cpfPagadorOriginal", solicitacaoRequest.cpfClienteSolicitante()
            );

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, variables);

            String responseMessage = String.format(
                    "Solicitação de devolução para PIX ID '%s' recebida e processo iniciado. ID do Processo: %s",
                    solicitacaoRequest.idTransacaoOriginal(), processInstance.getId()
            );

            LOGGER.info(responseMessage);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseMessage);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Dados inválidos na solicitação de devolução: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Dados inválidos na solicitação: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Erro ao iniciar o processo de devolução PIX.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao processar a solicitação de devolução.");
        }
    }

}