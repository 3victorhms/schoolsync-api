package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.sistema.ManutencaoDTO;
import br.cefetmg.schoolsync_api.dto.sistema.ManutencaoRequestDTO;
import br.cefetmg.schoolsync_api.dto.usuario.PerfilUsuarioDTO;
import br.cefetmg.schoolsync_api.dto.usuario.UsuarioResponseDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import br.cefetmg.schoolsync_api.service.ManutencaoService;
import br.cefetmg.schoolsync_api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioService usuarioService;
    private final ManutencaoService manutencaoService;
    private final UsuarioAtual usuarioAtual;

    @GetMapping("/usuarios")
    @Operation(summary = "Listar todos os usuários (somente ADMIN)")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.findAllList());
    }

    // promove um usuario a admin ou tira o cargo (o hasRole("ADMIN") do /admin/** já barra quem n é admin)
    @PutMapping("/usuarios/{id}/perfil")
    @Operation(summary = "Alterar o perfil de um usuário para ADMIN ou USUARIO (somente ADMIN)")
    public ResponseEntity<UsuarioResponseDTO> alterarPerfil(@PathVariable String id,
            @Valid @RequestBody PerfilUsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.alterarPerfil(id, dto.getPerfil()));
    }

    @GetMapping("/manutencao")
    @Operation(summary = "Ver o modo manutenção, com quem ligou e quando (somente ADMIN)")
    public ResponseEntity<ManutencaoDTO> verManutencao() {
        return ResponseEntity.ok(manutencaoService.estado());
    }

    // ligado, só admin usa o app; o resto recebe 503 e vê a tela de manutenção
    @PutMapping("/manutencao")
    @Operation(summary = "Ligar ou desligar o modo manutenção (somente ADMIN)")
    public ResponseEntity<ManutencaoDTO> alterarManutencao(@Valid @RequestBody ManutencaoRequestDTO dto) {
        return ResponseEntity.ok(manutencaoService.atualizar(dto, usuarioAtual.usuario().getNome()));
    }
}
