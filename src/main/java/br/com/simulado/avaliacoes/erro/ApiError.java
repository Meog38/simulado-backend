package br.com.simulado.avaliacoes.erro;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, Object details) {
}
