package br.com.synki.apidfe.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.util.ObjetoUtil;
import br.com.synki.apidfe.entity.Empresa;
import br.com.synki.apidfe.exception.SistemaException;
import br.com.synki.apidfe.repository.EmpresaRepository;

@Service

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

    public Empresa fromMultipart(
            String cpfCnpj,
            String razaoSocial,
            String uf,
            String ambiente,
            String senhaCertificado,
            String tipoPessoa,
            MultipartFile certificado
    ) {
        try {
            Empresa e = new Empresa();
            e.setCpfCnpj(cpfCnpj);
            e.setRazaoSocial(razaoSocial);
            e.setUf(uf);
            if (ambiente != null && !ambiente.isBlank()) {
                // AmbienteEnum (java-nfe): geralmente HOMOLOGACAO/PRODUCAO
                e.setAmbiente(AmbienteEnum.valueOf(ambiente.trim().toUpperCase()));
            }
            e.setSenhaCertificado(senhaCertificado);
            e.setTipoPessoa(tipoPessoa);
            if (certificado == null || certificado.isEmpty()) {
                throw new SistemaException("Campo certificado obrigatório.");
            }
            e.setCertificado(certificado.getBytes());
            return e;
        } catch (SistemaException se) {
            throw se;
        } catch (Exception ex) {
            throw new SistemaException("Falha ao ler certificado: " + ex.getMessage());
        }
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
