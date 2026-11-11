package br.com.simulado.avaliacoes.tarefa;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tarefa")
public class TarefaController {

    private final TarefaRepository repository;

    public TarefaController(TarefaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Tarefa> listar() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Tarefa> criar(@RequestBody Tarefa novaTarefa, @AuthenticationPrincipal Jwt jwt) {
        // Se o front não mandar o e-mail no JSON, pega da claim do Auth0 já configurada
        if (novaTarefa.getEmailUsuario() == null || novaTarefa.getEmailUsuario().isBlank()) {
            String emailToken = jwt.getClaimAsString("https://simulado-backend.example.com/email");
            novaTarefa.setEmailUsuario(emailToken != null ? emailToken : "usuario@auth0.com");
        }
        Tarefa salva = repository.save(novaTarefa);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    // Suporta DELETE /tarefa?id=1
    @DeleteMapping
    public ResponseEntity<Void> deletarPorParam(@RequestParam Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Suporta DELETE /tarefa/1 (caso o front chame por path variable)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPorPath(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
