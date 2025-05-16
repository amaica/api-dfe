package br.com.synki.apidfe.service;

import br.com.synki.apidfe.entity.NotaEntrada;
import br.com.synki.apidfe.exception.SistemaException;
import br.com.synki.apidfe.repository.NotaEntradaRepository;
import br.com.synki.apidfe.util.ArquivoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class NotaEntradaService {

    private final NotaEntradaRepository repository;

    public NotaEntradaService(NotaEntradaRepository repository) {
        this.repository = repository;
    }

    public void salvar(List<NotaEntrada> notasEntrada) {
        repository.saveAll(notasEntrada);
    }

    public List<NotaEntrada> listarTudo() {
        return repository.findAll();
    }

    public NotaEntrada listarPorId(Long idNota) {
        return repository.findById(idNota)
                .orElseThrow(() -> new SistemaException("Nota não encontrada com id: " + idNota));
    }

    public String getXml(Long idNota) throws IOException {
        NotaEntrada nota = listarPorId(idNota);
        return ArquivoUtil.descompactaXml(nota.getXml());
    }

    public NotaEntrada getPorChave(String chave) {
        return repository.findFirstByChave(chave)
                .orElseThrow(() -> new SistemaException("Nota não encontrada com chave: " + chave));
    }
}
