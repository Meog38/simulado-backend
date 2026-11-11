package br.com.simulado.avaliacoes.erro;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(MethodArgumentNotValidException exception) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                detalhes.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> tratarCorpoInvalido(HttpMessageNotReadableException exception) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                Map.of("mensagem", "O corpo da requisição está ausente ou malformado."));
    }

    private ResponseEntity<ApiError> resposta(HttpStatus status, String erro, Object detalhes) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), erro, detalhes));
    }
}
