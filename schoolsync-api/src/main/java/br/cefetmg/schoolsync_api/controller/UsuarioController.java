package br.cefetmg.schoolsync_api.controller;

import br.cefetmg.schoolsync_api.dto.usuario.LoginDTO;
import br.cefetmg.schoolsync_api.dto.usuario.LoginResponseDTO;
import br.cefetmg.schoolsync_api.dto.usuario.UsuarioRequestDTO;
import br.cefetmg.schoolsync_api.dto.usuario.UsuarioResponseDTO;
import br.cefetmg.schoolsync_api.dto.usuario.ImagemUsuarioDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import br.cefetmg.schoolsync_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioAtual usuarioAtual;

    // GET /usuarios (listar todos) foi removido: expunha nome e e-mail de todos
    // os usuários para qualquer pessoa logada, e o app não usa essa rota.

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable String id) {
        Optional<UsuarioResponseDTO> usuarioResponseDTO = usuarioService.findOne(id);

        if (usuarioResponseDTO.isPresent()) {
            return ResponseEntity.ok(usuarioResponseDTO.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastrar usuário")
    public ResponseEntity<UsuarioResponseDTO> inserir(@Valid @RequestBody UsuarioRequestDTO usuarioRequestDTO) {
        UsuarioResponseDTO usuarioResponseDTO = usuarioService.save(usuarioRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioResponseDTO);
    }

    @PostMapping("/autenticar")
    public ResponseEntity<LoginResponseDTO> autenticar(
            @Valid @RequestBody LoginDTO loginDTO
    ) {
        Optional<LoginResponseDTO> login = usuarioService.autenticar(
                loginDTO.getEmail(),
                loginDTO.getSenha()
        );

        if (login.isPresent()) {
            return ResponseEntity.ok(login.get());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/verificar-login")
    public ResponseEntity<Boolean> verificarLogin(@RequestParam String email) {
        return ResponseEntity.ok(usuarioService.verificarLogin(email));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable String id, @Valid @RequestBody UsuarioRequestDTO usuarioRequestDTO) {
        usuarioAtual.validar(id);
        UsuarioResponseDTO usuarioResponseDTO = usuarioService.update(id, usuarioRequestDTO);
        return ResponseEntity.ok(usuarioResponseDTO);
    }

    @PatchMapping("/{id}/imagem")
    @Operation(summary = "Enviar foto de perfil (Data URI em Base64)")
    public ResponseEntity<UsuarioResponseDTO> atualizarImagem(
            @PathVariable String id,
            @Valid @RequestBody ImagemUsuarioDTO dto) {
        usuarioAtual.validar(id);
        return ResponseEntity.ok(usuarioService.atualizarImagem(id, dto.getImagemBase64()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        usuarioAtual.validar(id);
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
