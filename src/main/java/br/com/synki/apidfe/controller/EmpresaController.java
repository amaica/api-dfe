package br.com.synki.apidfe.controller;

import br.com.synki.apidfe.entity.Empresa;
import br.com.synki.apidfe.service.EmpresaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empresa")
@Slf4j
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
