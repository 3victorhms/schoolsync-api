package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.atividade.AtividadeRequestDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    @Mock private AtividadeRepository atividadeRepository;
    @Mock private SalaRepository salaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CadernoRepository cadernoRepository;
    @Mock private MembrosRepository membrosRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private AtividadeService atividadeService;

    @Test
    void impedeCriacaoQuandoPontuacaoDaDisciplinaUltrapassaCem() {
        Sala sala = new Sala();
        sala.setId("sala-1");
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");
        Atividade existente = new Atividade();
        existente.setId("atividade-existente");
        existente.setValor(95d);

        AtividadeRequestDTO dto = new AtividadeRequestDTO();
        dto.setIdSala("sala-1");
        dto.setIdCriador("usuario-1");
        dto.setDisciplina("Matemática");
        dto.setTitulo("Prova");
        dto.setDataEntrega(LocalDate.now().plusDays(1));
        dto.setValor(10d);

        when(salaRepository.findByIdForUpdate("sala-1")).thenReturn(java.util.Optional.of(sala));
        when(usuarioRepository.findById("usuario-1")).thenReturn(java.util.Optional.of(usuario));
        when(atividadeRepository.findBySala_IdAndDisciplinaIgnoreCase("sala-1", "Matemática"))
                .thenReturn(List.of(existente));

        assertThrows(ResponseStatusException.class, () -> atividadeService.criar(dto));
        verify(atividadeRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
