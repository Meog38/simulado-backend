package br.com.simulado.avaliacoes.avaliacao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    List<Avaliacao> findAllByOrderByDataAvaliacaoDesc();
}
