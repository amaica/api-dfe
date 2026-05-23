package br.com.synki.apidfe.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.synki.apidfe.service.DistribuicaoService;

@Service

@DisallowConcurrentExecution
public class AgendadorConsulta {

	private final DistribuicaoService distribuicaoService;

	public AgendadorConsulta(DistribuicaoService distribuicaoService) {
		this.distribuicaoService = distribuicaoService;
	}

	@Scheduled(initialDelay = (1000 * 60 * 10), fixedDelay = (1000 * 60 * 60))
	public void efetuaConsulta() {
		try {
			System.out.println("Iniciando Consulta Notas");
			distribuicaoService.consultaNotas();
			System.out.println("Finalizado COnsulta Notas");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
