package br.cefetmg.schoolsync_api.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.usuario.LoginResponseDTO;
import br.cefetmg.schoolsync_api.dto.usuario.UsuarioResponseDTO;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import br.cefetmg.schoolsync_api.security.JwtService;
import br.cefetmg.schoolsync_api.service.GoogleTokenVerifier.ContaGoogle;
import lombok.RequiredArgsConstructor;

// login com google: o app manda o ID token, a gente confere com o google e devolve o mesmo
// JWT do login normal. se o e-mail já tem conta, liga a conta ao google; se n tem, cria
@Service
@RequiredArgsConstructor
public class LoginGoogleService {

    private static final int TAMANHO_MAXIMO_NOME = 150;

    private final GoogleTokenVerifier verificador;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponseDTO entrar(String idToken) {
        ContaGoogle conta = verificador.verificar(idToken);

        Usuario usuario = usuarioRepository.findByGoogleId(conta.id())
                .or(() -> usuarioRepository.findByEmailIgnoreCase(conta.email()))
                .orElseGet(() -> novoUsuario(conta));

        if (usuario.getGoogleId() != null && !usuario.getGoogleId().equals(conta.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse e-mail já está ligado a outra conta Google");
        }
        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Essa conta foi desativada");
        }

        usuario.setGoogleId(conta.id());
        if ((usuario.getFoto() == null || usuario.getFoto().isBlank()) && conta.foto() != null) {
            usuario.setFoto(conta.foto());
        }
        Usuario salvo = usuarioRepository.save(usuario);

        return new LoginResponseDTO(jwtService.gerarToken(salvo), new UsuarioResponseDTO(salvo));
    }

    private Usuario novoUsuario(ContaGoogle conta) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome(conta));
        usuario.setEmail(conta.email());
        usuario.setFoto(conta.foto());
        // quem entra pelo google n tem senha. fica um hash aleatório que ninguém sabe;
        // se a pessoa quiser entrar com senha também, usa o "esqueci minha senha" pra criar uma
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID() + "-" + UUID.randomUUID()));
        return usuario;
    }

    private static String nome(ContaGoogle conta) {
        String nome = conta.nome() == null || conta.nome().isBlank()
                ? conta.email().substring(0, conta.email().indexOf('@'))
                : conta.nome().trim();
        return nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
    }
}
