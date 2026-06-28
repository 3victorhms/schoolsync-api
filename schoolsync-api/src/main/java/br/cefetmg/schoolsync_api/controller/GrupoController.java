package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import br.cefetmg.schoolsync_api.service.GrupoService;
import br.cefetmg.schoolsync_api.service.TarefaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "Grupo")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;
    private final TarefaService tarefaService;

    @PostMapping("/grupos")
    public ResponseEntity<GrupoResponseDTO> criar(@Valid @RequestBody GrupoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grupoService.criar(dto));
    }

    @PostMapping("/grupos/entrar")
    public ResponseEntity<GrupoResponseDTO> entrar(
            @RequestParam String codigoConvite,
            @RequestParam String idUsuario
    ) {
        return ResponseEntity.ok(grupoService.entrar(codigoConvite, idUsuario));
    }

    @GetMapping("/grupos/{idGrupo}")
    public ResponseEntity<GrupoResponseDTO> buscarPorId(
            @PathVariable String idGrupo,
            @RequestParam String idUsuarioLogado
    ) {
        return ResponseEntity.ok(grupoService.buscarPorId(idGrupo, idUsuarioLogado));
    }

    @GetMapping("/salas/{idSala}/grupos/usuario/{idUsuario}")
    public ResponseEntity<List<GrupoResumoDTO>> listarPorSalaEUsuario(
            @PathVariable String idSala,
            @PathVariable String idUsuario
    ) {
        return ResponseEntity.ok(grupoService.listarPorSalaEUsuario(idSala, idUsuario));
    }

    @PutMapping("/grupos/{idGrupo}")
    public ResponseEntity<GrupoResponseDTO> atualizar(
            @PathVariable String idGrupo,
            @Valid @RequestBody GrupoRequestDTO dto
    ) {
        return ResponseEntity.ok(grupoService.atualizar(idGrupo, dto));
    }

    @DeleteMapping("/grupos/{idGrupo}")
    public ResponseEntity<Void> excluir(
            @PathVariable String idGrupo,
            @RequestParam String idUsuarioLogado
    ) {
        grupoService.excluir(idGrupo, idUsuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/grupos/{idGrupo}/sair")
    public ResponseEntity<Void> sair(
            @PathVariable String idGrupo,
            @RequestParam String idUsuario
    ) {
        grupoService.sair(idGrupo, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupos/{idGrupo}/tarefas")
    public ResponseEntity<List<TarefaResponseDTO>> listarTarefas(
            @PathVariable String idGrupo,
            @RequestParam String idUsuarioLogado
    ) {
        return ResponseEntity.ok(tarefaService.listarPorGrupo(idGrupo, idUsuarioLogado));
    }

    @GetMapping("/tarefas/usuario/{idUsuario}")
    public ResponseEntity<List<TarefaResponseDTO>> listarTarefasPorUsuario(
            @PathVariable String idUsuario
    ) {
        return ResponseEntity.ok(tarefaService.listarPorUsuario(idUsuario));
    }

    @PostMapping("/grupos/{idGrupo}/tarefas")
    public ResponseEntity<TarefaResponseDTO> criarTarefa(
            @PathVariable String idGrupo,
            @Valid @RequestBody TarefaRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarefaService.criar(idGrupo, dto));
    }

    @PutMapping("/tarefas/{idTarefa}/status")
    public ResponseEntity<TarefaResponseDTO> alterarStatusTarefa(
            @PathVariable String idTarefa,
            @Valid @RequestBody TarefaStatusDTO dto
    ) {
        return ResponseEntity.ok(tarefaService.alterarStatus(idTarefa, dto));
    }

    @DeleteMapping("/tarefas/{idTarefa}")
    public ResponseEntity<Void> excluirTarefa(
            @PathVariable String idTarefa,
            @RequestParam String idUsuarioLogado
    ) {
        tarefaService.excluir(idTarefa, idUsuarioLogado);
        return ResponseEntity.noContent().build();
    }
}
