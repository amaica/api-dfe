package br.com.synki.apidfe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiNFeApplication {

	public static void main(String[] args) {
		// SEFAZ costuma rejeitar chunked encoding do Axis2/commons-httpclient 3.1
		System.setProperty("axis2.transport.http.disableChunking", "true");
		System.setProperty("httpclient.protocol.expect-continue", "false");
		System.setProperty("http.keepAlive", "false");
		System.setProperty("https.keepAlive", "false");
		SpringApplication.run(ApiNFeApplication.class, args);
	}

}
