package br.cefetmg.schoolsync_api.service;

import br.cefetmg.schoolsync_api.security.JwtService;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.dto.usuario.*;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.GrupoRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.DispositivoPushRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SalaRepository salaRepository;
    private final GrupoRepository grupoRepository;
    private final TarefaRepository tarefaRepository;
    private final DispositivoPushRepository dispositivoPushRepository;
    private final CloudinaryService cloudinaryService;

    private final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService,
            SalaRepository salaRepository, GrupoRepository grupoRepository, TarefaRepository tarefaRepository,
            DispositivoPushRepository dispositivoPushRepository, CloudinaryService cloudinaryService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.salaRepository = salaRepository;
        this.grupoRepository = grupoRepository;
        this.tarefaRepository = tarefaRepository;
        this.dispositivoPushRepository = dispositivoPushRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public Optional<UsuarioPublicoDTO> findOne(String id) {
        log.debug("Request to get Usuario : {}", id);

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return Optional.of(new UsuarioPublicoDTO(usuario));
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

    @Transactional
    public void delete(String id) {
        log.debug("Request to delete Usuario : {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        validarSolicitante(id, "inativar");

        List<String> vinculos = new ArrayList<>();
        if (salaRepository.existsByLider_Id(id))
            vinculos.add("liderança de sala");
        if (grupoRepository.existsByCriador_Id(id))
            vinculos.add("liderança de grupo");
        // tarefa já concluída n impede, só as q ainda estão pendentes ou em andamento
        boolean temTarefaAberta = tarefaRepository.findByAtribuidoPara_IdOrderByDataCriacaoAsc(id).stream()
                .anyMatch(tarefa -> !"concluido".equals(tarefa.getStatus()));
        if (temTarefaAberta) {
            vinculos.add("tarefas pendentes atribuídas");
        }

        if (!vinculos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível inativar o usuário: resolva " + String.join(", ", vinculos)
                            + ". Transfira a liderança na tela da sala ou do grupo e conclua as tarefas pendentes antes de tentar novamente.");
        }

        // ainda deixa os comentários, histórico e etc, porém alterando o nome para
        // Usuário inativo
        usuario.setAtivo(false);
        usuario.setNome("Usuário inativo");
        usuario.setFoto(null);
        dispositivoPushRepository.deleteAllByUsuario_Id(id);
        usuarioRepository.save(usuario);
    }

    public UsuarioResponseDTO save(UsuarioRequestDTO dto) {
        log.debug("Request to save Usuario : {}", dto.getEmail());

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha é obrigatória");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setFoto(dto.getFoto());

        usuario = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(usuario);
    }

    public UsuarioResponseDTO update(String id, UsuarioRequestDTO dto) {
        log.debug("Request to update Usuario : {}", id);

        validarSolicitante(id, "alterar");

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário inativo não pode ser alterado");
        }

        Optional<Usuario> usuarioComMesmoEmail = usuarioRepository.findByEmail(dto.getEmail());
        if (usuarioComMesmoEmail.isPresent() && !usuarioComMesmoEmail.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email já cadastrado");
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());

        // só atualiza a senha se vier um valor novo e não vazio; senão mantém a senha
        // atual no banco
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        if (dto.getFoto() != null) {
            usuario.setFoto(dto.getFoto());
        }

        usuario = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarImagem(String id, String imagemBase64) {
        validarSolicitante(id, "alterar");

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário inativo não pode ser alterado");
        }

        usuario.setFoto(cloudinaryService.enviarFotoDePerfil(id, imagemBase64));
        return new UsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO atualizarFotoPorArquivo(String id, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Envie um arquivo de imagem no campo 'arquivo'");
        }

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado", ex);
        }

        String tipo = arquivo.getContentType() == null ? "" : arquivo.getContentType();
        String dataUri = "data:" + tipo + ";base64," + Base64.getEncoder().encodeToString(bytes);
        return atualizarImagem(id, dataUri);
    }

    private void validarSolicitante(String id, String acao) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof Usuario solicitante) || !solicitante.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode " + acao + " a própria conta");
        }
    }

    public Optional<LoginResponseDTO> autenticar(String email, String senha) {
        log.debug("Request to autenticar Usuario : {}", email);

        // igual na disciplina: o AuthenticationManager busca o usuário pelo UsuarioDetailsService
        // e compara a senha com o BCrypt. e-mail inexistente, senha errada ou conta desativada
        // lançam AuthenticationException, e aí o controller responde 401
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, senha));
        } catch (AuthenticationException ex) {
            return Optional.empty();
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login ou senha inválidos"));

        return Optional.of(new LoginResponseDTO(
                jwtService.gerarToken(usuario),
                new UsuarioResponseDTO(usuario)));
    }

    public boolean verificarLogin(String email) {
        log.debug("Request to verificarLogin : {}", email);
        return usuarioRepository.existsByEmail(email);
    }
}
