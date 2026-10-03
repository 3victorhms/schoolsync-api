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

import br.cefetmg.schoolsync_api.dto.atividade.AtividadeRequestDTO;
import br.cefetmg.schoolsync_api.dto.atividade.AtividadeResponseDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Materia;
import br.cefetmg.schoolsync_api.entity.Periodo;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.TipoPeriodo;
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
    @Mock private NotaService notaService;
    @InjectMocks private AtividadeService atividadeService;

    private Sala sala;
    private Usuario usuario;
    private Materia matematica;
    private Periodo primeiroBimestre;

    /** Sala por bimestre: 1º bimestre de 01/02 a 30/04 valendo 25 pontos. */
    @BeforeEach
    void montarSala() {
        usuario = new Usuario();
        usuario.setId("usuario-1");

        sala = new Sala();
        sala.setId("sala-1");
        sala.setLider(usuario);
        sala.setTipoPeriodo(TipoPeriodo.BIMESTRE);

        matematica = new Materia();
        matematica.setId("materia-1");
        matematica.setNome("Matemática");
        matematica.setSala(sala);
        sala.getMaterias().add(matematica);

        primeiroBimestre = new Periodo();
        primeiroBimestre.setId("periodo-1");
        primeiroBimestre.setOrdem(1);
        primeiroBimestre.setDataInicio(LocalDate.of(2027, 2, 1));
        primeiroBimestre.setDataFim(LocalDate.of(2027, 4, 30));
        primeiroBimestre.setPontuacaoMaxima(25d);
        primeiroBimestre.setSala(sala);
        sala.getPeriodos().add(primeiroBimestre);
    }

    private AtividadeRequestDTO novaAtividade(String idMateria, LocalDate dataEntrega, double valor) {
        AtividadeRequestDTO dto = new AtividadeRequestDTO();
        dto.setIdSala("sala-1");
        dto.setIdMateria(idMateria);
        dto.setTitulo("Prova");
        dto.setDataEntrega(dataEntrega);
        dto.setValor(valor);
        return dto;
    }

    private Atividade atividadeExistente(double valor) {
        Atividade existente = new Atividade();
        existente.setId("atividade-existente");
        existente.setValor(valor);
        return existente;
    }

    private void prepararSalaEUsuario() {
        when(salaRepository.findByIdForUpdate("sala-1")).thenReturn(Optional.of(sala));
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario));
    }

    @Test
    void impedeCriacaoQuandoPontuacaoDaMateriaNoPeriodoUltrapassaOMaximo() {
        prepararSalaEUsuario();
        when(atividadeRepository.findByMateria_IdAndPeriodo_Id("materia-1", "periodo-1"))
                .thenReturn(List.of(atividadeExistente(20d)));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> atividadeService.criar(novaAtividade("materia-1", LocalDate.of(2027, 3, 10), 10d), "usuario-1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, erro.getStatusCode());
        verify(atividadeRepository, never()).save(any());
    }

    @Test
    void criaAtividadeNoPeriodoDaDataDeEntregaQuandoCabeNoLimite() {
        prepararSalaEUsuario();
        when(atividadeRepository.findByMateria_IdAndPeriodo_Id("materia-1", "periodo-1"))
                .thenReturn(List.of(atividadeExistente(15d)));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(membrosRepository.findBySala_Id("sala-1")).thenReturn(List.of());

        AtividadeResponseDTO criada = atividadeService.criar(novaAtividade("materia-1", LocalDate.of(2027, 3, 10), 10d), "usuario-1");

        assertEquals("materia-1", criada.getIdMateria());
        assertEquals("periodo-1", criada.getIdPeriodo());
        assertEquals("1º Bimestre", criada.getNomePeriodo());
    }

    @Test
    void recusaDataDeEntregaForaDosPeriodosDaSala() {
        prepararSalaEUsuario();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> atividadeService.criar(novaAtividade("materia-1", LocalDate.of(2027, 7, 15), 5d), "usuario-1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, erro.getStatusCode());
        verify(atividadeRepository, never()).save(any());
    }

    @Test
    void recusaMateriaQueNaoEDaSala() {
        prepararSalaEUsuario();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> atividadeService.criar(novaAtividade("materia-de-outra-sala", LocalDate.of(2027, 3, 10), 5d), "usuario-1"));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(atividadeRepository, never()).save(any());
    }
}
