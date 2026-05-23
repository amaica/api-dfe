package br.com.synki.apidfe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiNFeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiNFeApplication.class, args);
	}

}
