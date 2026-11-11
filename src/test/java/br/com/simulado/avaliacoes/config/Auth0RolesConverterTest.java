package br.com.simulado.avaliacoes.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class Auth0RolesConverterTest {

    private final Auth0RolesConverter converter =
            new Auth0RolesConverter("https://simulado-backend.example.com/roles");

    @Test
    void converteRolesDoAuth0EmAuthoritiesSpring() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("https://simulado-backend.example.com/roles", List.of("ADMIN", "USER"))
                .build();

        assertThat(authorities(jwt)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void convertePermissoesDaApiEmRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("permissions", List.of("avaliacoes:write", "avaliacoes:read"))
                .build();

        assertThat(authorities(jwt)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void ignoraRolesDesconhecidas() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("https://simulado-backend.example.com/roles", List.of("SUPERVISOR"))
                .build();

        assertThat(authorities(jwt)).isEmpty();
    }

    private Set<String> authorities(Jwt jwt) {
        return converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
