package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.sala.SalaRequestDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResponseDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResumoDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import br.cefetmg.schoolsync_api.service.SalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** v2: o usuário vem sempre do token (UsuarioAtual), nunca de parâmetro da requisição. */
@RestController
@RequestMapping("/salas")
@Tag(name = "Sala")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService salaService;
    private final UsuarioAtual usuarioAtual;

    @PostMapping
    @Operation(summary = "Criar sala (o usuário logado vira o líder)")
    public ResponseEntity<SalaResponseDTO> criar(@Valid @RequestBody SalaRequestDTO salaRequestDTO) {
        SalaResponseDTO sala = salaService.criar(salaRequestDTO, usuarioAtual.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(sala);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar sala, matérias e períodos")
    public ResponseEntity<SalaResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody SalaRequestDTO salaRequestDTO
    ) {
        salaService.validarLider(id, usuarioAtual.id());
        return ResponseEntity.ok(salaService.atualizar(id, salaRequestDTO));
    }

    @PostMapping("/entrar")
    @Operation(summary = "Entrar em uma sala via código de convite")
    public ResponseEntity<SalaResponseDTO> entrar(@RequestParam String codigoConvite) {
        return ResponseEntity.ok(salaService.entrar(codigoConvite, usuarioAtual.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sala por ID")
    public ResponseEntity<SalaResponseDTO> buscarPorId(@PathVariable String id) {
        String idUsuario = usuarioAtual.id();
        salaService.validarMembro(id, idUsuario);
        return ResponseEntity.ok(salaService.buscarPorId(id, idUsuario));
    }

    @GetMapping
    @Operation(summary = "Listar as salas do usuário logado")
    public ResponseEntity<List<SalaResumoDTO>> listarMinhas() {
        return ResponseEntity.ok(salaService.listarPorUsuario(usuarioAtual.id()));
    }

    @DeleteMapping("/{idSala}/membros/{idUsuarioRemover}")
    @Operation(summary = "Remover membro da sala (só o líder)")
    public ResponseEntity<Void> excluirMembro(
            @PathVariable String idSala,
            @PathVariable String idUsuarioRemover
    ) {
        salaService.excluirMembro(idSala, idUsuarioRemover, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idSala}/sair")
    @Operation(summary = "Sair da sala")
    public ResponseEntity<Void> sair(@PathVariable String idSala) {
        salaService.sair(idSala, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir sala")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        salaService.validarLider(id, usuarioAtual.id());
        salaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
