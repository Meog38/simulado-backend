package br.com.simulado.avaliacoes.avaliacao;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService service;
    private final String emailClaim;

    public AvaliacaoController(
            AvaliacaoService service,
            @Value("${app.security.auth0.email-claim}") String emailClaim) {
        this.service = service;
        this.emailClaim = emailClaim;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse criar(
            @Valid @RequestBody CriarAvaliacaoRequest request,
            JwtAuthenticationToken authentication) {
        String emailAvaliador = authentication.getToken().getClaimAsString(emailClaim);
        if (emailAvaliador == null || emailAvaliador.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "O token autenticado precisa conter a claim de e-mail configurada.");
        }
        return service.criar(emailAvaliador, request);
    }

    @GetMapping
    public List<AvaliacaoResponse> listar() {
        return service.listar();
    }
}
