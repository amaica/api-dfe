package br.com.synki.apidfe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.synki.apidfe.entity.NotaEntrada;

import java.util.Optional;

@Repository
public interface NotaEntradaRepository extends JpaRepository<NotaEntrada, Long> {

    Optional<NotaEntrada> findFirstByChave(String chave);
    Optional<NotaEntrada> findFirstByCnpjEmitente(String emitente);

}
