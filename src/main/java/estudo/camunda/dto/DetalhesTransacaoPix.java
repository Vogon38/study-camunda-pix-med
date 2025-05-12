package estudo.camunda.dto;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DetalhesTransacaoPix implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String idTransacao;
    private BigDecimal valor;
    private String cpfCnpjPagador;
    private String nomePagador;
    private String cpfCnpjRecebedor;
    private String nomeRecebedor;
    private LocalDateTime dataHoraTransacao;
    private String status;

    public DetalhesTransacaoPix(String idTransacao, BigDecimal valor, String cpfCnpjPagador, String nomePagador, String cpfCnpjRecebedor, String nomeRecebedor, LocalDateTime dataHoraTransacao, String status) {
        if (idTransacao == null || idTransacao.isBlank()) {
            throw new IllegalArgumentException("ID da transação não pode ser nulo ou vazio.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transação deve ser positivo.");
        }
        this.idTransacao = idTransacao;
        this.valor = valor;
        this.cpfCnpjPagador = cpfCnpjPagador;
        this.nomePagador = nomePagador;
        this.cpfCnpjRecebedor = cpfCnpjRecebedor;
        this.nomeRecebedor = nomeRecebedor;
        this.dataHoraTransacao = dataHoraTransacao;
        this.status = status;
    }

}