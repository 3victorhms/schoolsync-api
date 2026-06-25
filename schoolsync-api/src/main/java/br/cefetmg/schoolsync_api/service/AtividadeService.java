package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.atividade.AtividadeRequestDTO;
import br.cefetmg.schoolsync_api.dto.atividade.AtividadeResponseDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CadernoRepository cadernoRepository;

    @Transactional
    public AtividadeResponseDTO criar(AtividadeRequestDTO dto) {
        Sala sala = salaRepository.findById(dto.getIdSala())
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Usuario criador = usuarioRepository.findById(dto.getIdCriador())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        Atividade atividade = new Atividade();
        atividade.setTitulo(dto.getTitulo());
        atividade.setDescricao(dto.getDescricao());
        atividade.setDisciplina(dto.getDisciplina());
        atividade.setDataEntrega(dto.getDataEntrega());
        atividade.setValor(dto.getValor());
        atividade.setSala(sala);
        atividade.setCriadaPor(criador);

        Atividade atividadeSalva = atividadeRepository.save(atividade);

        Caderno caderno = new Caderno();
        caderno.setAtividade(atividadeSalva);
        caderno.setUsuario(criador);
        caderno.setStatus("nao_iniciada");
        caderno.setDataAdicionado(LocalDateTime.now());

        cadernoRepository.save(caderno);

        return new AtividadeResponseDTO(atividadeSalva, caderno.getStatus());
    }

    @Transactional
    public AtividadeResponseDTO atualizar(String id, AtividadeRequestDTO dto) {
        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        atividade.setTitulo(dto.getTitulo());
        atividade.setDescricao(dto.getDescricao());
        atividade.setDisciplina(dto.getDisciplina());
        atividade.setDataEntrega(dto.getDataEntrega());
        atividade.setValor(dto.getValor());

        Atividade atividadeAtualizada = atividadeRepository.save(atividade);

        return new AtividadeResponseDTO(atividadeAtualizada);
    }

    public AtividadeResponseDTO buscarPorId(String idAtividade, String idUsuarioLogado) {
        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        String status = cadernoRepository
                .findByAtividade_IdAndUsuario_Id(idAtividade, idUsuarioLogado)
                .map(Caderno::getStatus)
                .orElse(null);

        return new AtividadeResponseDTO(atividade, status);
    }

    public List<AtividadeResponseDTO> listarPorSala(String idSala) {
        return atividadeRepository.findBySala_Id(idSala)
                .stream()
                .map(AtividadeResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<AtividadeResponseDTO> listarPorUsuarioNoCaderno(String idUsuario) {
        return cadernoRepository.findByUsuario_Id(idUsuario)
                .stream()
                .map(caderno -> new AtividadeResponseDTO(caderno.getAtividade(), caderno.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void excluir(String id) {
        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        atividadeRepository.delete(atividade);
    }

    @Transactional
    public void excluirAtividadesDaSala(String idSala) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        atividadeRepository.deleteAll(new ArrayList<>(sala.getAtividades()));
        sala.getAtividades().clear();
    }

    @Transactional
    public void adicionarNoCaderno(String idAtividade, String idUsuario) {
        if (cadernoRepository.findByAtividade_IdAndUsuario_Id(idAtividade, idUsuario).isPresent()) {
            return;
        }

        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        Caderno caderno = new Caderno();
        caderno.setAtividade(atividade);
        caderno.setUsuario(usuario);
        caderno.setStatus("nao_iniciada");
        caderno.setDataAdicionado(LocalDateTime.now());

        cadernoRepository.save(caderno);
    }

    @Transactional
    public void removerDoCaderno(String idAtividade, String idUsuario) {
        cadernoRepository
                .findByAtividade_IdAndUsuario_Id(idAtividade, idUsuario)
                .ifPresent(cadernoRepository::delete);
    }

    @Transactional
    public void alterarStatus(String idAtividade, String idUsuario, String status) {
        Caderno caderno = cadernoRepository
                .findByAtividade_IdAndUsuario_Id(idAtividade, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada no caderno"));

        caderno.setStatus(status);
        cadernoRepository.save(caderno);
    }
}
