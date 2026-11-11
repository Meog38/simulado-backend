package br.com.simulado.avaliacoes.avaliacao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AvaliacaoResponse criar(String emailAvaliador, CriarAvaliacaoRequest request) {
        Avaliacao avaliacao = new Avaliacao(
                emailAvaliador,
                request.emailAvaliado(),
                request.comentariosAvaliacao(),
                request.nota());
        return AvaliacaoResponse.from(repository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listar() {
        return repository.findAllByOrderByDataAvaliacaoDesc()
                .stream()
                .map(AvaliacaoResponse::from)
                .toList();
    }
}
