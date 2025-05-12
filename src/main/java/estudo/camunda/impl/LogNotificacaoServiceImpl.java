package estudo.camunda.impl;

import estudo.camunda.services.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificacaoServiceImpl implements NotificacaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogNotificacaoServiceImpl.class);

    @Override
    public void enviarNotificacao(String identificadorCliente, String mensagem) {
        LOGGER.info("======================================================================");
        LOGGER.info("== SIMULAÇÃO DE ENVIO DE NOTIFICAÇÃO ==");
        LOGGER.info("== Para Cliente/Identificador: {}", identificadorCliente);
        LOGGER.info("== Mensagem: {}", mensagem);
        LOGGER.info("======================================================================");
    }

}
