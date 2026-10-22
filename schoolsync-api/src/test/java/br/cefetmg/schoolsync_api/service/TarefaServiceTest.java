package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import br.cefetmg.schoolsync_api.dto.tarefa.TarefaRequestDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaStatusDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Grupo;
import br.cefetmg.schoolsync_api.entity.GrupoMembro;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

// feito com auxílio do Claude: prazo das tarefas e avisos ao grupo
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock private TarefaRepository tarefaRepository;
    @Mock private AtividadeRepository atividadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GrupoService grupoService;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private TarefaService tarefaService;

    private Usuario lider;
    private Usuario ana;
    private Usuario bruno;
    private Grupo grupo;
    private Atividade atividade;

    @BeforeEach
    void montarGrupo() {
        lider = usuario("lider", "Victor");
        ana = usuario("ana", "Ana");
        bruno = usuario("bruno", "Bruno");

        Sala sala = new Sala();
        sala.setId("sala-1");

        grupo = new Grupo();
        grupo.setId("grupo-1");
        grupo.setNome("Seminário");
        grupo.setSala(sala);
        grupo.setCriador(lider);
        grupo.setMembros(List.of(membro(lider), membro(ana), membro(bruno)));

        atividade = new Atividade();
        atividade.setId("atividade-1");
        atividade.setSala(sala);
        atividade.setDataEntrega(LocalDate.now().plusDays(10));
    }

    private Usuario usuario(String id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setAtivo(true);
        return usuario;
    }

    private GrupoMembro membro(Usuario usuario) {
        GrupoMembro membro = new GrupoMembro();
        membro.setGrupo(grupo);
        membro.setUsuario(usuario);
        return membro;
    }

    private Tarefa tarefa(String id, Usuario atribuido, String status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(id);
        tarefa.setTitulo("Parte " + id);
        tarefa.setStatus(status);
        tarefa.setGrupo(grupo);
        tarefa.setAtividade(atividade);
        tarefa.setAtribuidoPara(atribuido);
        tarefa.setCriadaPor(lider);
        return tarefa;
    }

    private TarefaStatusDTO status(String valor) {
        TarefaStatusDTO dto = new TarefaStatusDTO();
        dto.setStatus(valor);
        return dto;
    }

    @Test
    void prazoDaTarefaNaoPodePassarDaEntregaDaAtividade() {
        when(grupoService.buscarGrupo("grupo-1")).thenReturn(grupo);
        when(atividadeRepository.findById("atividade-1")).thenReturn(Optional.of(atividade));
        when(usuarioRepository.findById("lider")).thenReturn(Optional.of(lider));
        when(usuarioRepository.findById("ana")).thenReturn(Optional.of(ana));

        TarefaRequestDTO dto = new TarefaRequestDTO();
        dto.setTitulo("Slides");
        dto.setIdAtividade("atividade-1");
        dto.setIdUsuarioAtribuido("ana");
        dto.setPrazo(atividade.getDataEntrega().plusDays(1));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> tarefaService.criar("grupo-1", dto, "lider"));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void concluirTarefaAvisaOsOutrosMembros() {
        Tarefa daAna = tarefa("t1", ana, "em_andamento");
        Tarefa doBruno = tarefa("t2", bruno, "pendente");
        when(tarefaRepository.findById("t1")).thenReturn(Optional.of(daAna));
        when(tarefaRepository.save(daAna)).thenReturn(daAna);
        when(usuarioRepository.findById("ana")).thenReturn(Optional.of(ana));
        when(tarefaRepository.findByGrupo_IdOrderByDataCriacaoAsc("grupo-1")).thenReturn(List.of(daAna, doBruno));

        tarefaService.alterarStatus("t1", status("concluido"), "ana");

        verify(notificacaoService).criarParaUsuario(eq("lider"), eq("GRUPO_TAREFA_CONCLUIDA"), anyString(), anyString(), eq("grupo-1"));
        verify(notificacaoService).criarParaUsuario(eq("bruno"), eq("GRUPO_TAREFA_CONCLUIDA"), anyString(), anyString(), eq("grupo-1"));
        verify(notificacaoService, never()).criarParaUsuario(eq("ana"), anyString(), anyString(), anyString(), anyString());
        verify(notificacaoService, never()).criarParaUsuario(anyString(), eq("GRUPO_CONCLUIDO"), anyString(), anyString(), anyString());
    }

    @Test
    void ultimaTarefaConcluidaAvisaTodoMundoQueOGrupoTerminou() {
        Tarefa daAna = tarefa("t1", ana, "em_andamento");
        Tarefa doBruno = tarefa("t2", bruno, "concluido");
        when(tarefaRepository.findById("t1")).thenReturn(Optional.of(daAna));
        when(tarefaRepository.save(daAna)).thenReturn(daAna);
        when(usuarioRepository.findById("ana")).thenReturn(Optional.of(ana));
        when(tarefaRepository.findByGrupo_IdOrderByDataCriacaoAsc("grupo-1")).thenReturn(List.of(daAna, doBruno));

        tarefaService.alterarStatus("t1", status("concluido"), "ana");

        verify(notificacaoService, times(3)).criarParaUsuario(anyString(), eq("GRUPO_CONCLUIDO"), anyString(), anyString(), eq("grupo-1"));
    }

    @Test
    void mudarParaEmAndamentoNaoAvisaNinguem() {
        Tarefa daAna = tarefa("t1", ana, "pendente");
        when(tarefaRepository.findById("t1")).thenReturn(Optional.of(daAna));
        when(tarefaRepository.save(daAna)).thenReturn(daAna);

        tarefaService.alterarStatus("t1", status("em_andamento"), "ana");

        verify(notificacaoService, never()).criarParaUsuario(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
