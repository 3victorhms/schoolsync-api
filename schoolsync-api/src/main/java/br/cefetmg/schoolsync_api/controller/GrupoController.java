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

import br.cefetmg.schoolsync_api.dto.grupo.GrupoRequestDTO;
import br.cefetmg.schoolsync_api.dto.grupo.GrupoResponseDTO;
import br.cefetmg.schoolsync_api.dto.grupo.GrupoResumoDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaRequestDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaResponseDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaStatusDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import br.cefetmg.schoolsync_api.service.GrupoService;
import br.cefetmg.schoolsync_api.service.TarefaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@Tag(name = "Grupo")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;
    private final TarefaService tarefaService;
    private final UsuarioAtual usuarioAtual;

    @PostMapping("/grupos")
    public ResponseEntity<GrupoResponseDTO> criar(@Valid @RequestBody GrupoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grupoService.criar(dto, usuarioAtual.id()));
    }

    @PostMapping("/grupos/entrar")
    public ResponseEntity<GrupoResponseDTO> entrar(@RequestParam String codigoConvite) {
        return ResponseEntity.ok(grupoService.entrar(codigoConvite, usuarioAtual.id()));
    }

    @GetMapping("/grupos/{idGrupo}")
    public ResponseEntity<GrupoResponseDTO> buscarPorId(@PathVariable String idGrupo) {
        return ResponseEntity.ok(grupoService.buscarPorId(idGrupo, usuarioAtual.id()));
    }

    /** Grupos do usuário logado dentro da sala. */
    @GetMapping("/salas/{idSala}/grupos")
    public ResponseEntity<List<GrupoResumoDTO>> listarMeusGruposDaSala(@PathVariable String idSala) {
        return ResponseEntity.ok(grupoService.listarPorSalaEUsuario(idSala, usuarioAtual.id()));
    }

    @PutMapping("/grupos/{idGrupo}")
    public ResponseEntity<GrupoResponseDTO> atualizar(
            @PathVariable String idGrupo,
            @Valid @RequestBody GrupoRequestDTO dto
    ) {
        return ResponseEntity.ok(grupoService.atualizar(idGrupo, dto, usuarioAtual.id()));
    }

    @DeleteMapping("/grupos/{idGrupo}")
    public ResponseEntity<Void> excluir(@PathVariable String idGrupo) {
        grupoService.excluir(idGrupo, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/grupos/{idGrupo}/membros/{idUsuarioRemover}")
    public ResponseEntity<Void> removerMembro(
            @PathVariable String idGrupo,
            @PathVariable String idUsuarioRemover
    ) {
        grupoService.removerMembro(idGrupo, idUsuarioRemover, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/grupos/{idGrupo}/sair")
    public ResponseEntity<Void> sair(@PathVariable String idGrupo) {
        grupoService.sair(idGrupo, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupos/{idGrupo}/tarefas")
    public ResponseEntity<List<TarefaResponseDTO>> listarTarefas(@PathVariable String idGrupo) {
        return ResponseEntity.ok(tarefaService.listarPorGrupo(idGrupo, usuarioAtual.id()));
    }

    /** Tarefas atribuídas ao usuário logado em todos os grupos. */
    @GetMapping("/tarefas")
    public ResponseEntity<List<TarefaResponseDTO>> listarMinhasTarefas() {
        return ResponseEntity.ok(tarefaService.listarPorUsuario(usuarioAtual.id()));
    }

    @PostMapping("/grupos/{idGrupo}/tarefas")
    public ResponseEntity<TarefaResponseDTO> criarTarefa(
            @PathVariable String idGrupo,
            @Valid @RequestBody TarefaRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarefaService.criar(idGrupo, dto, usuarioAtual.id()));
    }

    @PutMapping("/tarefas/{idTarefa}/status")
    public ResponseEntity<TarefaResponseDTO> alterarStatusTarefa(
            @PathVariable String idTarefa,
            @Valid @RequestBody TarefaStatusDTO dto
    ) {
        return ResponseEntity.ok(tarefaService.alterarStatus(idTarefa, dto, usuarioAtual.id()));
    }

    @DeleteMapping("/tarefas/{idTarefa}")
    public ResponseEntity<Void> excluirTarefa(@PathVariable String idTarefa) {
        tarefaService.excluir(idTarefa, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }
}
