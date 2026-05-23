package br.com.synki.apidfe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.synki.apidfe.service.DistribuicaoService;
import br.com.synki.apidfe.service.NotaEntradaService;

@RestController
@RequestMapping("/api/v1/notaEntrada")

public class NotaEntradaController {

    private final NotaEntradaService notaEntradaService;
    private final DistribuicaoService distribuicaoService;

    public NotaEntradaController(NotaEntradaService notaEntradaService, DistribuicaoService distribuicaoService) {this.notaEntradaService = notaEntradaService;
        this.distribuicaoService = distribuicaoService;
    }

    @GetMapping(value = "consulta")
    public ResponseEntity<?> consulta() {
        try {
            distribuicaoService.consultaNotas();
            return ResponseEntity.ok(listarTodos());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            return ResponseEntity.ok(notaEntradaService.listarTudo());
        } catch (Exception e) {
        	 e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<?> listarTodos(@PathVariable("id") Long idNotaEntrada) {
        try {
            return ResponseEntity.ok(notaEntradaService.listarPorId(idNotaEntrada));
        } catch (Exception e) {
        	 e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "xml/{id}")
    public ResponseEntity<?> getXml(@PathVariable("id") Long idNotaEntrada) {
        try {
            return ResponseEntity.ok(notaEntradaService.getXml(idNotaEntrada));
        } catch (Exception e) {
        	 e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "chave/{chave}")
    public ResponseEntity<?> getXml(@PathVariable("chave") String chave) {
        try {
            return ResponseEntity.ok(notaEntradaService.getPorChave(chave));
        } catch (Exception e) {
        	 e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
