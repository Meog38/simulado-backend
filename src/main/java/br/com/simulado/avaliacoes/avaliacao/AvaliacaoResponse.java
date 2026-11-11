package br.com.simulado.avaliacoes.avaliacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AvaliacaoResponse(
        UUID id,
        String emailAvaliador,
        String emailAvaliado,
        String comentariosAvaliacao,
        BigDecimal nota,
        Instant dataAvaliacao) {

    public static AvaliacaoResponse from(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getEmailAvaliador(),
                avaliacao.getEmailAvaliado(),
                avaliacao.getComentariosAvaliacao(),
                avaliacao.getNota(),
                avaliacao.getDataAvaliacao());
    }
}
