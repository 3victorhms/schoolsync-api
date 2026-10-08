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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // As rotas de alteração usam "/me": a conta alterada é sempre a do token.

    @PutMapping("/me")
    @Operation(summary = "Atualizar os dados do usuário logado")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@Valid @RequestBody UsuarioRequestDTO usuarioRequestDTO) {
        UsuarioResponseDTO usuarioResponseDTO = usuarioService.update(usuarioAtual.id(), usuarioRequestDTO);
        return ResponseEntity.ok(usuarioResponseDTO);
    }

    @PatchMapping("/me/imagem")
    @Operation(summary = "Enviar foto de perfil do usuário logado (Data URI em Base64)")
    public ResponseEntity<UsuarioResponseDTO> atualizarImagem(@Valid @RequestBody ImagemUsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarImagem(usuarioAtual.id(), dto.getImagemBase64()));
    }

    /**
     * Upload de arquivo de verdade (multipart/form-data, campo "arquivo").
     * Faz o mesmo que a rota PATCH /me/imagem, que recebe a imagem em Base64 e
     * continua sendo a usada pelo app.
     */
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar foto de perfil como arquivo (multipart/form-data, campo 'arquivo')")
    public ResponseEntity<UsuarioResponseDTO> enviarFoto(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(usuarioService.atualizarFotoPorArquivo(usuarioAtual.id(), arquivo));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Desativar a conta do usuário logado")
    public ResponseEntity<Void> excluir() {
        usuarioService.delete(usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }
}
