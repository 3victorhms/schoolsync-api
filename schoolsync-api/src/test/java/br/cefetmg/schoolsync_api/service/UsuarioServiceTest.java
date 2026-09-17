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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.GrupoRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import br.cefetmg.schoolsync_api.repository.DispositivoPushRepository;
import br.cefetmg.schoolsync_api.security.JwtService;
import br.cefetmg.schoolsync_api.security.SenhaEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SenhaEncoder senhaEncoder;
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
        assertTrue(erro.getReason().contains("atividades/tarefas atribuídas"));
        assertTrue(erro.getReason().contains("Transfira as lideranças e reatribua as atividades"));
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
