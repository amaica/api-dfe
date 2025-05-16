package br.com.synki.apidfe.service;

import br.com.swconsultoria.nfe.util.ObjetoUtil;
import br.com.swconsultoria.nfe.util.XmlNfeUtil;
import br.com.synki.apidfe.entity.Empresa;
import br.com.synki.apidfe.exception.SistemaException;
import br.com.synki.apidfe.repository.EmpresaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmpresaService {

    private final EmpresaRepository repository;

    public EmpresaService(EmpresaRepository repository) {this.repository = repository;}

    public Empresa salvar(Empresa empresa){
        validar(empresa);
        System.out.println("Salvando Empresa");
        return repository.save(empresa);
    }

    public void deletar(Long idEmpresa){
        repository.deleteById(idEmpresa);
    }

    public List<Empresa> listarTudo(){
        return repository.findAll();
    }

    public Empresa listarPorId(Long idEmpresa){
        return repository.findById(idEmpresa)
                .orElseThrow(() -> new SistemaException("Empresa não encontrada com id: "+idEmpresa));
    }

    private void validar(Empresa empresa) {
        ObjetoUtil.verifica(empresa.getCpfCnpj()).orElseThrow( () ->
            new SistemaException("Campo cpf/cnpj obrigatório.")
        );
        ObjetoUtil.verifica(empresa.getCertificado()).orElseThrow( () ->
            new SistemaException("Campo certificado obrigatório.")
        );
    }
}
