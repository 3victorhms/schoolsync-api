package br.cefetmg.schoolsync_api.service;

import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.dto.usuario.*;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<UsuarioResponseDTO> findOne(String id) {
        log.debug("Request to get Usuario : {}", id);

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return Optional.of(new UsuarioResponseDTO(usuario));
        } else {
            return Optional.empty();
        }
    }

    public List<UsuarioResponseDTO> findAllList() {
        log.debug("Request to get All Usuarios");

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<UsuarioResponseDTO> resposta = new ArrayList<>();

        for (Usuario usuario : usuarios) {
            resposta.add(new UsuarioResponseDTO(usuario));
        }

        return resposta;
    }

    public void delete(String id) {
        log.debug("Request to delete Usuario : {}", id);
        usuarioRepository.deleteById(id);
    }

    public UsuarioResponseDTO save(UsuarioRequestDTO dto) {
        log.debug("Request to save Usuario : {}", dto);

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha é obrigatória");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(dto.getSenha());
        usuario.setFoto(dto.getFoto());

        usuario = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(usuario);
    }

    public UsuarioResponseDTO update(String id, UsuarioRequestDTO dto) {
        log.debug("Request to update Usuario : {}", dto);

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        Usuario usuario = usuarioOpt.get();

        Optional<Usuario> usuarioComMesmoEmail = usuarioRepository.findByEmail(dto.getEmail());
        if (usuarioComMesmoEmail.isPresent() && !usuarioComMesmoEmail.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email já cadastrado");
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());

        // só atualiza a senha se vier um valor novo e não vazio; senão mantém a senha atual no banco
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(dto.getSenha());
        }

        usuario.setFoto(dto.getFoto());

        usuario = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(usuario);
    }

    public Optional<UsuarioResponseDTO> autenticar(String email, String senha) {
        log.debug("Request to autenticar Usuario : {}", email);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndSenha(email, senha);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return Optional.of(new UsuarioResponseDTO(usuario));
        } else {
            return Optional.empty();
        }
    }

    public boolean verificarLogin(String email) {
        log.debug("Request to verificarLogin : {}", email);
        return usuarioRepository.existsByEmail(email);
    }
}