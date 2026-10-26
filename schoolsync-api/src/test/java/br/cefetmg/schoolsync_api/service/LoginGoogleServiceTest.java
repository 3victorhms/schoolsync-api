package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.usuario.LoginResponseDTO;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import br.cefetmg.schoolsync_api.security.JwtService;
import br.cefetmg.schoolsync_api.service.GoogleTokenVerifier.ContaGoogle;

@ExtendWith(MockitoExtension.class)
class LoginGoogleServiceTest {

    @Mock private GoogleTokenVerifier verificador;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtService jwtService;

    private LoginGoogleService service;
    private final ContaGoogle conta = new ContaGoogle("g-42", "marcos@gmail.com", "Marcos Daniel", "https://foto");

    @BeforeEach
    void preparar() {
        service = new LoginGoogleService(verificador, usuarioRepository, new BCryptPasswordEncoder(4), jwtService);
        when(verificador.verificar("token")).thenReturn(conta);
    }

    private void salvarDevolveOMesmo() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(jwtService.gerarToken(any(Usuario.class))).thenReturn("jwt");
    }

    @Test
    void emailSemContaCriaUmaContaNova() {
        when(usuarioRepository.findByGoogleId("g-42")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("marcos@gmail.com")).thenReturn(Optional.empty());
        salvarDevolveOMesmo();

        LoginResponseDTO resposta = service.entrar("token");

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertEquals("Marcos Daniel", salvo.getValue().getNome());
        assertEquals("marcos@gmail.com", salvo.getValue().getEmail());
        assertEquals("g-42", salvo.getValue().getGoogleId());
        assertEquals("https://foto", salvo.getValue().getFoto());
        assertNotNull(salvo.getValue().getSenha());
        assertEquals("jwt", resposta.getToken());
        assertTrue(resposta.getUsuario().isContaGoogle());
    }

    @Test
    void contaQueJaExistiaPeloEmailFicaLigadaAoGoogle() {
        Usuario existente = new Usuario();
        existente.setId("u1");
        existente.setNome("Marcos");
        existente.setEmail("Marcos@gmail.com");
        existente.setFoto("https://foto-antiga");
        when(usuarioRepository.findByGoogleId("g-42")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("marcos@gmail.com")).thenReturn(Optional.of(existente));
        salvarDevolveOMesmo();

        service.entrar("token");

        assertEquals("g-42", existente.getGoogleId());
        // quem já tinha foto continua com a dela
        assertEquals("https://foto-antiga", existente.getFoto());
        assertEquals("Marcos", existente.getNome());
    }

    @Test
    void contaDesativadaNaoEntra() {
        Usuario desativado = new Usuario();
        desativado.setGoogleId("g-42");
        desativado.setAtivo(false);
        when(usuarioRepository.findByGoogleId("g-42")).thenReturn(Optional.of(desativado));

        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.entrar("token")).getStatusCode().value());
        verify(jwtService, never()).gerarToken(any());
    }

    @Test
    void emailLigadoAOutraContaGoogleNaoEntra() {
        Usuario deOutro = new Usuario();
        deOutro.setEmail("marcos@gmail.com");
        deOutro.setGoogleId("g-outro");
        when(usuarioRepository.findByGoogleId("g-42")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("marcos@gmail.com")).thenReturn(Optional.of(deOutro));

        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.entrar("token")).getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }
}
