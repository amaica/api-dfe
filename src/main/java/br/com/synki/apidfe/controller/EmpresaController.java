package br.com.synki.apidfe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import br.com.synki.apidfe.entity.Empresa;
import br.com.synki.apidfe.service.EmpresaService;

@RestController
@RequestMapping("/api/v1/empresa")

public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {this.empresaService = empresaService;}

    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody Empresa empresa) {
        try {
            empresaService.salvar(empresa);
            return ResponseEntity.ok(empresa);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Cadastro via multipart (recomendado para certificado .pfx/.p12).
     *
     * Exemplos:
     * - curl -F "cpfCnpj=123" -F "uf=SP" -F "ambiente=HOMOLOGACAO" -F "senhaCertificado=xxx" -F "certificado=@a1.pfx" http://localhost:9090/api/v1/empresa/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> salvarMultipart(
            @RequestParam("cpfCnpj") String cpfCnpj,
            @RequestParam(value = "razaoSocial", required = false) String razaoSocial,
            @RequestParam(value = "uf", required = false) String uf,
            @RequestParam(value = "ambiente", required = false) String ambiente,
            @RequestParam(value = "senhaCertificado", required = false) String senhaCertificado,
            @RequestParam(value = "tipoPessoa", required = false) String tipoPessoa,
            @RequestParam(value = "certificado") MultipartFile certificado
    ) {
        try {
            Empresa empresa = empresaService.fromMultipart(cpfCnpj, razaoSocial, uf, ambiente, senhaCertificado, tipoPessoa, certificado);
            Empresa saved = empresaService.salvar(empresa);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            return ResponseEntity.ok(empresaService.listarTudo());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(value = "{id}")
    public ResponseEntity<?> deletar(@PathVariable("id") Long idEmpresa) {
        try {
            empresaService.deletar(idEmpresa);
            return ResponseEntity.ok("Empresa deletada com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao deletar Empresas: "+e.getMessage());
        }
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<?> listarTodos(@PathVariable("id") Long idEmpresa) {
        try {
            return ResponseEntity.ok(empresaService.listarPorId(idEmpresa));
        } catch (Exception e) {
           e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
