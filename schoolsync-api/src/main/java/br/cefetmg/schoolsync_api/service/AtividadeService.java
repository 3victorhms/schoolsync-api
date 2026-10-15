package br.cefetmg.schoolsync_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.atividade.AtividadeRequestDTO;
import br.cefetmg.schoolsync_api.dto.atividade.AtividadeResponseDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.Materia;
import br.cefetmg.schoolsync_api.entity.Membros;
import br.cefetmg.schoolsync_api.entity.Periodo;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtividadeService {

    private static final double VALOR_MAXIMO_POR_ATIVIDADE = 15d;
    private static final Set<String> STATUS_CADERNO = Set.of("nao_iniciada", "em_andamento", "concluido");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AtividadeRepository atividadeRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CadernoRepository cadernoRepository;
    private final MembrosRepository membrosRepository;
    private final NotificacaoService notificacaoService;
    private final NotaService notaService;
    private final TarefaRepository tarefaRepository;

    @Transactional
    public AtividadeResponseDTO criar(AtividadeRequestDTO dto, String idCriador) {
        Sala sala = salaRepository.findByIdForUpdate(dto.getIdSala())
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Usuario criador = usuarioRepository.findById(idCriador)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        Materia materia = buscarMateriaDaSala(sala, dto.getIdMateria());
        Periodo periodo = buscarPeriodoDaData(sala, dto.getDataEntrega());
        validarPontuacao(materia, periodo, dto.getValor(), null);

        Atividade atividade = new Atividade();
        atividade.setTitulo(dto.getTitulo());
        atividade.setDescricao(dto.getDescricao());
        atividade.setMateria(materia);
        atividade.setPeriodo(periodo);
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
    public AtividadeResponseDTO atualizar(String id, AtividadeRequestDTO dto, String idEditor) {
        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        Sala sala = salaRepository.findByIdForUpdate(atividade.getSala().getId())
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));
        Materia materia = buscarMateriaDaSala(sala, dto.getIdMateria());
        Periodo periodo = buscarPeriodoDaData(sala, dto.getDataEntrega());
        validarPontuacao(materia, periodo, dto.getValor(), id);

        atividade.setTitulo(dto.getTitulo());
        atividade.setDescricao(dto.getDescricao());
        atividade.setMateria(materia);
        atividade.setPeriodo(periodo);
        atividade.setDataEntrega(dto.getDataEntrega());
        atividade.setValor(dto.getValor());

        Atividade atividadeAtualizada = atividadeRepository.save(atividade);

        notaService.sincronizarComAtividade(atividadeAtualizada);

        notificarAtividadeAtualizada(atividadeAtualizada, idEditor);

        return new AtividadeResponseDTO(atividadeAtualizada);
    }

    private Materia buscarMateriaDaSala(Sala sala, String idMateria) {
        return sala.getMaterias().stream()
                .filter(materia -> materia.getId().equals(idMateria))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Escolha uma das matérias cadastradas nesta sala"));
    }

    private Periodo buscarPeriodoDaData(Sala sala, LocalDate dataEntrega) {
        return sala.periodoDaData(dataEntrega)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "A data de entrega " + (dataEntrega == null ? "" : dataEntrega.format(DATA_BR) + " ")
                                + "está fora dos períodos letivos da sala"));
    }

    // soma o valor das atividades dessa matéria no período (na edição ignora a própria atividade)
    // e n deixa passar da pontuação máxima. a margem de 0.000001 é pq o double n guarda decimal exato
    private void validarPontuacao(Materia materia, Periodo periodo, Double novoValor, String idIgnorado) {
        if (novoValor == null || novoValor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor da atividade não pode ser negativo");
        }

        if (novoValor > VALOR_MAXIMO_POR_ATIVIDADE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O valor máximo de uma atividade é 15 pontos");
        }

        double utilizado = atividadeRepository.findByMateria_IdAndPeriodo_Id(materia.getId(), periodo.getId()).stream()
                .filter(atividade -> idIgnorado == null || !atividade.getId().equals(idIgnorado))
                .mapToDouble(Atividade::getValor)
                .sum();
        double maximo = periodo.getPontuacaoMaxima();
        double disponivel = Math.max(0, maximo - utilizado);

        if (utilizado + novoValor > maximo + 0.000001d) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Pontuação máxima de %.2f pontos excedida para %s no %s. Já utilizados: %.2f; disponíveis: %.2f; valor informado: %.2f.",
                            maximo, materia.getNome(), periodo.getNome(), utilizado, disponivel, novoValor));
        }
    }

    // avisa todo mundo da sala menos quem fez a edição (pode ser o criador ou o líder)
    private void notificarAtividadeAtualizada(Atividade atividade, String idEditor) {
        List<Membros> membros = membrosRepository.findBySala_Id(atividade.getSala().getId());
        for (Membros membro : membros) {
            Usuario usuario = membro.getUsuario();
            if (!usuario.getId().equals(idEditor) && usuario.isAtivo()) {
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
        notaService.desvincularDaAtividade(atividade);
        excluirTarefasDaAtividade(id);
        atividadeRepository.delete(atividade);
    }

    // as tarefas de grupo apontam pra atividade (id_atividade é obrigatório), ent precisam
    // sair antes dela, senão o banco recusa a exclusão. o flush manda os deletes na hora
    private void excluirTarefasDaAtividade(String idAtividade) {
        List<Tarefa> tarefas = tarefaRepository.findByAtividade_Id(idAtividade);
        tarefas.forEach(tarefa -> notificacaoService.removerPorAlvo(tarefa.getId()));
        tarefaRepository.deleteAll(tarefas);
        tarefaRepository.flush();
    }

    @Transactional
    public void excluirAtividadesDaSala(String idSala) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        List<Atividade> atividades = new ArrayList<>(sala.getAtividades());
        atividades.forEach(atividade -> {
            notificacaoService.removerPorAlvo(atividade.getId());
            notaService.desvincularDaAtividade(atividade);
            excluirTarefasDaAtividade(atividade.getId());
        });
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
        if (!STATUS_CADERNO.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status inválido: use nao_iniciada, em_andamento ou concluido");
        }

        Caderno caderno = cadernoRepository
                .findByAtividade_IdAndUsuario_Id(idAtividade, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada no caderno"));

        caderno.setStatus(status);
        cadernoRepository.save(caderno);
    }

    @Transactional(readOnly = true)
    public void validarPodeGerenciar(String idAtividade, String idUsuario) {
        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        // quem criou mas saiu ou foi removido da sala n pode mais mexer na atividade
        validarMembroDaSala(atividade.getSala().getId(), idUsuario);

        boolean criador = atividade.getCriadaPor() != null && atividade.getCriadaPor().getId().equals(idUsuario);
        boolean liderDaSala = atividade.getSala().getLider().getId().equals(idUsuario);
        if (!criador && !liderDaSala) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas quem criou a atividade ou o líder da sala pode alterá-la");
        }
    }

    @Transactional(readOnly = true)
    public void validarAcessoAtividade(String idAtividade, String idUsuario) {
        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));
        validarMembroDaSala(atividade.getSala().getId(), idUsuario);
    }

    public void validarMembroDaSala(String idSala, String idUsuario) {
        if (!membrosRepository.existsBySala_IdAndUsuario_Id(idSala, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta sala");
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
