package estudo.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServicoDevolucaoPixApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicoDevolucaoPixApplication.class, args);
		System.out.println("\n✅ Aplicação de Devolução PIX iniciada! Acesse o Camunda em http://localhost:8080/camunda/app/\n");
	}

}
