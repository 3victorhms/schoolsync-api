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
import br.cefetmg.schoolsync_api.entity.Membros;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtividadeService {

    private static final double VALOR_MAXIMO_POR_ATIVIDADE = 15d;

    private final AtividadeRepository atividadeRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CadernoRepository cadernoRepository;
    private final MembrosRepository membrosRepository;
    private final NotificacaoService notificacaoService;

    @Transactional
    public AtividadeResponseDTO criar(AtividadeRequestDTO dto) {
        Sala sala = salaRepository.findByIdForUpdate(dto.getIdSala())
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Usuario criador = usuarioRepository.findById(dto.getIdCriador())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        validarPontuacao(sala.getId(), dto.getDisciplina(), dto.getValor(), null);

        Atividade atividade = new Atividade();
        atividade.setTitulo(dto.getTitulo());
        atividade.setDescricao(dto.getDescricao());
        atividade.setDisciplina(dto.getDisciplina().trim());
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

        notificarNovaAtividade(atividadeSalva, criador);

        return new AtividadeResponseDTO(atividadeSalva, caderno.getStatus());
    }

    private void notificarNovaAtividade(Atividade atividade, Usuario criador) {
        List<Membros> membros = membrosRepository.findBySala_Id(atividade.getSala().getId());

        for (Membros membro : membros) {
            Usuario usuario = membro.getUsuario();
            if (!usuario.getId().equals(criador.getId()) && usuario.isAtivo()) {
                notificacaoService.criarParaUsuario(
                        usuario.getId(),
                        "ATIVIDADE",
                        "Nova atividade",
                        criador.getNome() + " criou a atividade " + atividade.getTitulo(),
                        atividade.getId()
                );
            }
        }
    }

    @Transactional
    public AtividadeResponseDTO atualizar(String id, AtividadeRequestDTO dto) {
        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        Sala sala = salaRepository.findByIdForUpdate(atividade.getSala().getId())
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));
        validarPontuacao(sala.getId(), dto.getDisciplina(), dto.getValor(), id);

        atividade.setTitulo(dto.getTitulo());
        atividade.setDescricao(dto.getDescricao());
        atividade.setDisciplina(dto.getDisciplina().trim());
        atividade.setDataEntrega(dto.getDataEntrega());
        atividade.setValor(dto.getValor());

        Atividade atividadeAtualizada = atividadeRepository.save(atividade);

        notificarAtividadeAtualizada(atividadeAtualizada);

        return new AtividadeResponseDTO(atividadeAtualizada);
    }

    private void validarPontuacao(String idSala, String disciplina, Double novoValor, String idIgnorado) {
        if (novoValor == null || novoValor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor da atividade não pode ser negativo");
        }

        if (novoValor > VALOR_MAXIMO_POR_ATIVIDADE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O valor máximo de uma atividade é 15 pontos");
        }

        String disciplinaNormalizada = disciplina == null ? "" : disciplina.trim();
        if (disciplinaNormalizada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A disciplina é obrigatória");
        }

        double utilizado = atividadeRepository.findBySala_IdAndDisciplinaIgnoreCase(idSala, disciplinaNormalizada).stream()
                .filter(atividade -> idIgnorado == null || !atividade.getId().equals(idIgnorado))
                .mapToDouble(Atividade::getValor)
                .sum();
        double disponivel = Math.max(0, 100 - utilizado);

        if (utilizado + novoValor > 100 + 0.000001d) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Pontuação máxima de 100 pontos excedida para %s. Já utilizados: %.2f; disponíveis: %.2f; valor informado: %.2f.",
                            disciplinaNormalizada, utilizado, disponivel, novoValor));
        }
    }

    private void notificarAtividadeAtualizada(Atividade atividade) {
        List<Membros> membros = membrosRepository.findBySala_Id(atividade.getSala().getId());
        for (Membros membro : membros) {
            Usuario usuario = membro.getUsuario();
            if (!usuario.getId().equals(atividade.getCriadaPor().getId()) && usuario.isAtivo()) {
                notificacaoService.criarParaUsuario(
                        usuario.getId(),
                        "ATIVIDADE",
                        "Atividade atualizada",
                        atividade.getTitulo() + " foi atualizada. Novo prazo: " + atividade.getDataEntrega(),
                        atividade.getId());
            }
        }
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

        notificacaoService.removerPorAlvo(id);
        atividadeRepository.delete(atividade);
    }

    @Transactional
    public void excluirAtividadesDaSala(String idSala) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        List<Atividade> atividades = new ArrayList<>(sala.getAtividades());
        atividades.forEach(atividade -> notificacaoService.removerPorAlvo(atividade.getId()));
        atividadeRepository.deleteAll(atividades);
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

    /** Editar ou excluir: só quem criou a atividade ou o líder da sala. */
    @Transactional(readOnly = true)
    public void validarPodeGerenciar(String idAtividade, String idUsuario) {
        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        boolean criador = atividade.getCriadaPor() != null && atividade.getCriadaPor().getId().equals(idUsuario);
        boolean liderDaSala = atividade.getSala().getLider().getId().equals(idUsuario);
        if (!criador && !liderDaSala) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas quem criou a atividade ou o líder da sala pode alterá-la");
        }
    }

    @Transactional(readOnly = true)
    public void validarLiderDaSala(String idSala, String idUsuario) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        if (!sala.getLider().getId().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o líder da sala pode fazer isso");
        }
    }
}
