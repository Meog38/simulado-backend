package br.com.simulado.avaliacoes.avaliacao;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarAvaliacaoRequest(
        @NotBlank @Email @Size(max = 320) String emailAvaliado,
        @NotBlank @Size(max = 5000) String comentariosAvaliacao,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal nota) {
}
