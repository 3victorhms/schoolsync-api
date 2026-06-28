package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.tarefa.TarefaRequestDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaResponseDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaStatusDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Grupo;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final AtividadeRepository atividadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final GrupoService grupoService;

    @Transactional
    public TarefaResponseDTO criar(String idGrupo, TarefaRequestDTO dto) {
        Grupo grupo = grupoService.buscarGrupo(idGrupo);
        grupoService.validarCriadorDoGrupo(grupo, dto.getIdUsuarioLogado());
        grupoService.validarMembroDoGrupo(grupo.getId(), dto.getIdUsuarioAtribuido());

        Atividade atividade = atividadeRepository.findById(dto.getIdAtividade())
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        if (!atividade.getSala().getId().equals(grupo.getSala().getId())) {
            throw new IllegalArgumentException("Atividade nao pertence a sala do grupo");
        }

        Usuario usuarioLogado = usuarioRepository.findById(dto.getIdUsuarioLogado())
                .orElseThrow(() -> new EntityNotFoundException("Usuario logado nao encontrado"));

        Usuario usuarioAtribuido = usuarioRepository.findById(dto.getIdUsuarioAtribuido())
                .orElseThrow(() -> new EntityNotFoundException("Usuario atribuido nao encontrado"));

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(dto.getTitulo());
        tarefa.setStatus("pendente");
        tarefa.setDataCriacao(LocalDateTime.now());
        tarefa.setGrupo(grupo);
        tarefa.setAtividade(atividade);
        tarefa.setCriadaPor(usuarioLogado);
        tarefa.setAtribuidoPara(usuarioAtribuido);

        return new TarefaResponseDTO(tarefaRepository.save(tarefa));
    }

    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarPorGrupo(String idGrupo, String idUsuarioLogado) {
        Grupo grupo = grupoService.buscarGrupo(idGrupo);
        grupoService.validarMembroDoGrupo(grupo.getId(), idUsuarioLogado);

        return tarefaRepository.findByGrupo_IdOrderByDataCriacaoAsc(idGrupo)
                .stream()
                .map(TarefaResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarPorUsuario(String idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new EntityNotFoundException("Usuario nao encontrado");
        }

        return tarefaRepository.findByAtribuidoPara_IdOrderByDataCriacaoAsc(idUsuario)
                .stream()
                .map(TarefaResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public TarefaResponseDTO alterarStatus(String idTarefa, TarefaStatusDTO dto) {
        Tarefa tarefa = buscarTarefa(idTarefa);

        boolean usuarioAtribuido = tarefa.getAtribuidoPara().getId().equals(dto.getIdUsuarioLogado());
        boolean criadorGrupo = tarefa.getGrupo().getCriador().getId().equals(dto.getIdUsuarioLogado());

        if (!usuarioAtribuido && !criadorGrupo) {
            throw new IllegalArgumentException("Usuario nao pode alterar esta tarefa");
        }

        tarefa.setStatus(dto.getStatus());

        return new TarefaResponseDTO(tarefaRepository.save(tarefa));
    }

    @Transactional
    public void excluir(String idTarefa, String idUsuarioLogado) {
        Tarefa tarefa = buscarTarefa(idTarefa);
        grupoService.validarCriadorDoGrupo(tarefa.getGrupo(), idUsuarioLogado);

        tarefaRepository.delete(tarefa);
    }

    private Tarefa buscarTarefa(String idTarefa) {
        return tarefaRepository.findById(idTarefa)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa nao encontrada"));
    }
}
