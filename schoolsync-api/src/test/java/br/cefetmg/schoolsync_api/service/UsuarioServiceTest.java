package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.usuario.UsuarioResponseDTO;
import br.cefetmg.schoolsync_api.entity.Perfil;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.GrupoRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import br.cefetmg.schoolsync_api.repository.DispositivoPushRepository;
import br.cefetmg.schoolsync_api.security.JwtService;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private SalaRepository salaRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private TarefaRepository tarefaRepository;
    @Mock private DispositivoPushRepository dispositivoPushRepository;
    @Mock private CloudinaryService cloudinaryService;
    @InjectMocks private UsuarioService usuarioService;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void informaTodosOsVinculosQueImpedemAInativacao() {
        Usuario usuario = usuarioAutenticado();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(salaRepository.existsByLider_Id(usuario.getId())).thenReturn(true);
        when(grupoRepository.existsByCriador_Id(usuario.getId())).thenReturn(true);
        when(tarefaRepository.findByAtribuidoPara_IdOrderByDataCriacaoAsc(usuario.getId()))
                .thenReturn(List.of(new Tarefa()));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.delete(usuario.getId()));

        assertEquals(409, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("liderança de sala"));
        assertTrue(erro.getReason().contains("liderança de grupo"));
        assertTrue(erro.getReason().contains("tarefas pendentes atribuídas"));
        assertTrue(erro.getReason().contains("Transfira a liderança na tela da sala ou do grupo"));
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void inativaUsuarioSemApagarSeuRegistro() {
        Usuario usuario = usuarioAutenticado();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(tarefaRepository.findByAtribuidoPara_IdOrderByDataCriacaoAsc(usuario.getId()))
                .thenReturn(List.of());

        usuarioService.delete(usuario.getId());

        assertFalse(usuario.isAtivo());
        assertEquals("Usuário inativo", usuario.getNome());
        verify(usuarioRepository).save(usuario);
        verify(dispositivoPushRepository).deleteAllByUsuario_Id(usuario.getId());
        verify(usuarioRepository, never()).delete(usuario);
    }

    @Test
    void adminPromoveOutroUsuarioAAdmin() {
        usuarioAutenticado();
        Usuario outro = new Usuario();
        outro.setId("usuario-2");
        outro.setAtivo(true);
        outro.setPerfil(Perfil.USUARIO);
        when(usuarioRepository.findById("usuario-2")).thenReturn(Optional.of(outro));
        when(usuarioRepository.save(outro)).thenReturn(outro);

        UsuarioResponseDTO resposta = usuarioService.alterarPerfil("usuario-2", Perfil.ADMIN);

        assertEquals(Perfil.ADMIN, resposta.getPerfil());
        verify(usuarioRepository).save(outro);
    }

    @Test
    void adminNaoPodeAlterarOProprioPerfil() {
        Usuario admin = usuarioAutenticado();
        admin.setPerfil(Perfil.ADMIN);
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPerfil(admin.getId(), Perfil.USUARIO));

        assertEquals(400, erro.getStatusCode().value());
        assertEquals(Perfil.ADMIN, admin.getPerfil());
        verify(usuarioRepository, never()).save(admin);
    }

    @Test
    void naoAlteraPerfilDeContaDesativada() {
        usuarioAutenticado();
        Usuario inativo = new Usuario();
        inativo.setId("usuario-3");
        inativo.setAtivo(false);
        when(usuarioRepository.findById("usuario-3")).thenReturn(Optional.of(inativo));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPerfil("usuario-3", Perfil.ADMIN));

        assertEquals(409, erro.getStatusCode().value());
        verify(usuarioRepository, never()).save(inativo);
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");
        usuario.setNome("Usuário de teste");
        usuario.setAtivo(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
        return usuario;
    }
}
