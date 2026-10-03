package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.nota.NotaRequestDTO;
import br.cefetmg.schoolsync_api.dto.nota.NotaResponseDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Materia;
import br.cefetmg.schoolsync_api.entity.Nota;
import br.cefetmg.schoolsync_api.entity.Periodo;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.TipoPeriodo;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.NotaRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock private NotaRepository notaRepository;
    @Mock private SalaRepository salaRepository;
    @Mock private AtividadeRepository atividadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MembrosRepository membrosRepository;
    @InjectMocks private NotaService notaService;

    private Sala sala;
    private Usuario aluno;
    private Materia matematica;
    private Periodo primeiroBimestre;

    /** 1º bimestre do ano passado (datas já ocorridas), valendo 25 pontos por matéria. */
    @BeforeEach
    void montarSala() {
        int anoPassado = LocalDate.now().getYear() - 1;

        aluno = new Usuario();
        aluno.setId("aluno-1");

        sala = new Sala();
        sala.setId("sala-1");
        sala.setLider(aluno);
        sala.setTipoPeriodo(TipoPeriodo.BIMESTRE);

        matematica = new Materia();
        matematica.setId("materia-1");
        matematica.setNome("Matemática");
        matematica.setSala(sala);
        sala.getMaterias().add(matematica);

        primeiroBimestre = new Periodo();
        primeiroBimestre.setId("periodo-1");
        primeiroBimestre.setOrdem(1);
        primeiroBimestre.setDataInicio(LocalDate.of(anoPassado, 2, 1));
        primeiroBimestre.setDataFim(LocalDate.of(anoPassado, 4, 30));
        primeiroBimestre.setPontuacaoMaxima(25d);
        primeiroBimestre.setSala(sala);
        sala.getPeriodos().add(primeiroBimestre);

        when(membrosRepository.existsBySala_IdAndUsuario_Id("sala-1", "aluno-1")).thenReturn(true);
        when(salaRepository.findById("sala-1")).thenReturn(Optional.of(sala));
        when(usuarioRepository.findById("aluno-1")).thenReturn(Optional.of(aluno));
    }

    private NotaRequestDTO avulsa(double valorMaximo, double valorObtido) {
        NotaRequestDTO dto = new NotaRequestDTO();
        dto.setDescricao("Prova de recuperação");
        dto.setIdMateria("materia-1");
        dto.setValorMaximo(valorMaximo);
        dto.setValorObtido(valorObtido);
        dto.setData(primeiroBimestre.getDataInicio().plusDays(10));
        return dto;
    }

    private Atividade atividadeDaSala(double valor) {
        Atividade atividade = new Atividade();
        atividade.setId("atividade-1");
        atividade.setTitulo("Trabalho");
        atividade.setValor(valor);
        atividade.setSala(sala);
        atividade.setMateria(matematica);
        atividade.setPeriodo(primeiroBimestre);
        atividade.setDataEntrega(primeiroBimestre.getDataInicio().plusDays(5));
        return atividade;
    }

    @Test
    void notaAvulsaEntraNoLimiteDoPeriodoJuntoComAsAtividadesDaSala() {
        // A sala já distribuiu 20 dos 25 pontos: uma avulsa de 10 não cabe
        when(atividadeRepository.findByMateria_IdAndPeriodo_Id("materia-1", "periodo-1"))
                .thenReturn(List.of(atividadeDaSala(20d)));
        when(notaRepository.findByUsuario_IdAndMateria_IdAndPeriodo_IdAndAtividadeIsNull("aluno-1", "materia-1", "periodo-1"))
                .thenReturn(List.of());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> notaService.lancar("sala-1", avulsa(10d, 8d), "aluno-1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, erro.getStatusCode());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void lancaNotaAvulsaNoPeriodoDaData() {
        when(atividadeRepository.findByMateria_IdAndPeriodo_Id("materia-1", "periodo-1"))
                .thenReturn(List.of(atividadeDaSala(15d)));
        when(notaRepository.findByUsuario_IdAndMateria_IdAndPeriodo_IdAndAtividadeIsNull("aluno-1", "materia-1", "periodo-1"))
                .thenReturn(List.of());
        when(notaRepository.save(any(Nota.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        NotaResponseDTO nota = notaService.lancar("sala-1", avulsa(10d, 8d), "aluno-1");

        assertEquals("periodo-1", nota.getIdPeriodo());
        assertEquals("Prova de recuperação", nota.getDescricao());
        assertEquals(8d, nota.getValorObtido().doubleValue());
    }

    @Test
    void notaDeAtividadeNaoPodePassarDoValorDaAtividade() {
        Atividade atividade = atividadeDaSala(10d);
        when(atividadeRepository.findById("atividade-1")).thenReturn(Optional.of(atividade));
        when(notaRepository.findByAtividade_IdAndUsuario_Id("atividade-1", "aluno-1")).thenReturn(Optional.empty());

        NotaRequestDTO dto = new NotaRequestDTO();
        dto.setIdAtividade("atividade-1");
        dto.setValorObtido(12d);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> notaService.lancar("sala-1", dto, "aluno-1"));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(notaRepository, never()).save(any());
    }
}
