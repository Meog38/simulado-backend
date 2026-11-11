package br.com.simulado.avaliacoes.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private final AudienceValidator validator =
            new AudienceValidator("https://api.example.com");

    @Test
    void aceitaTokenComAudienceEsperada() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .audience(List.of("https://api.example.com"))
                .build();

        assertThat(validator.validate(jwt)).isEqualTo(OAuth2TokenValidatorResult.success());
    }

    @Test
    void rejeitaTokenSemAudienceEsperada() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .audience(List.of("https://other.example.com"))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }
}
