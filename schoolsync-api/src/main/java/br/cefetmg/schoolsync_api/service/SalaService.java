package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.sala.MateriaRequestDTO;
import br.cefetmg.schoolsync_api.dto.sala.PeriodoRequestDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaRequestDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResponseDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResumoDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.Materia;
import br.cefetmg.schoolsync_api.entity.Membros;
import br.cefetmg.schoolsync_api.entity.Nota;
import br.cefetmg.schoolsync_api.entity.Periodo;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.TipoPeriodo;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.NotaRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalaService {

    private static final int MAXIMO_MEMBROS_POR_SALA = 50;
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembrosRepository membrosRepository;
    private final CadernoRepository cadernoRepository;
    private final TarefaRepository tarefaRepository;
    private final NotificacaoService notificacaoService;
    private final AtividadeRepository atividadeRepository;
    private final NotaRepository notaRepository;
    private final GrupoService grupoService;

    @Transactional
    public SalaResponseDTO criar(SalaRequestDTO dto, String idLider) {
        Usuario lider = usuarioRepository.findById(idLider)
                .orElseThrow(() -> new EntityNotFoundException("Usuario lider nao encontrado"));

        Sala sala = new Sala();
        sala.setNome(dto.getNome().trim());
        sala.setCodigoConvite(gerarCodigoConvite());
        sala.setLider(lider);
        aplicarMaterias(sala, dto.getMaterias());
        aplicarPeriodos(sala, dto.getTipoPeriodo(), dto.getPeriodos());

        Membros membroLider = new Membros();
        membroLider.setSala(sala);
        membroLider.setUsuario(lider);
        membroLider.setDataEntrada(LocalDateTime.now());

        sala.getMembros().add(membroLider);

        Sala salaSalva = salaRepository.save(sala);

        return new SalaResponseDTO(salaSalva, Map.of());
    }

    @Transactional
    public SalaResponseDTO atualizar(String id, SalaRequestDTO dto) {
        Sala sala = salaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        sala.setNome(dto.getNome().trim());
        aplicarMaterias(sala, dto.getMaterias());
        aplicarPeriodos(sala, dto.getTipoPeriodo(), dto.getPeriodos());
        reencaixarAtividadesNosPeriodos(sala);

        Sala salaAtualizada = salaRepository.save(sala);

        return new SalaResponseDTO(salaAtualizada, Map.of());
    }

    private void aplicarMaterias(Sala sala, List<MateriaRequestDTO> materiasDto) {
        Set<String> nomesUsados = new HashSet<>();
        for (MateriaRequestDTO materiaDto : materiasDto) {
            String nome = materiaDto.getNome().trim();
            if (!nomesUsados.add(nome.toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A matéria \"" + nome + "\" foi informada mais de uma vez");
            }
        }

        Map<String, Materia> existentes = new HashMap<>();
        sala.getMaterias().forEach(materia -> existentes.put(materia.getId(), materia));

        Set<String> idsMantidos = materiasDto.stream()
                .map(MateriaRequestDTO::getId)
                .filter(idMateria -> idMateria != null && !idMateria.isBlank())
                .collect(Collectors.toSet());

        for (String idMantido : idsMantidos) {
            if (!existentes.containsKey(idMantido)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matéria não encontrada nesta sala");
            }
        }

        for (Materia existente : existentes.values()) {
            if (!idsMantidos.contains(existente.getId())) {
                if (atividadeRepository.existsByMateria_Id(existente.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Não é possível remover " + existente.getNome()
                                    + ": já existem atividades cadastradas nessa matéria");
                }
                if (notaRepository.existsByMateria_Id(existente.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Não é possível remover " + existente.getNome()
                                    + ": alunos já lançaram notas nessa matéria no boletim");
                }
                sala.getMaterias().remove(existente);
            }
        }

        for (MateriaRequestDTO materiaDto : materiasDto) {
            String nome = materiaDto.getNome().trim();
            if (materiaDto.getId() != null && !materiaDto.getId().isBlank()) {
                existentes.get(materiaDto.getId()).setNome(nome);
            } else {
                Materia nova = new Materia();
                nova.setNome(nome);
                nova.setSala(sala);
                sala.getMaterias().add(nova);
            }
        }
    }

    private void aplicarPeriodos(Sala sala, TipoPeriodo tipo, List<PeriodoRequestDTO> periodosDto) {
        validarPeriodos(tipo, periodosDto);

        boolean mudouTipo = sala.getTipoPeriodo() != null && sala.getTipoPeriodo() != tipo;
        if (mudouTipo && (!sala.getAtividades().isEmpty() || notaRepository.existsBySala_IdAndAtividadeIsNull(sala.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível trocar de " + sala.getTipoPeriodo().getRotulo().toLowerCase(Locale.ROOT)
                            + " para " + tipo.getRotulo().toLowerCase(Locale.ROOT)
                            + " com atividades ou notas já cadastradas na sala");
        }

        sala.setTipoPeriodo(tipo);

        if (!mudouTipo && sala.getPeriodos().size() == periodosDto.size()) {
            for (int i = 0; i < periodosDto.size(); i++) {
                Periodo periodo = sala.getPeriodos().get(i);
                preencherPeriodo(periodo, i + 1, periodosDto.get(i));
            }
            return;
        }

        sala.getPeriodos().clear();
        for (int i = 0; i < periodosDto.size(); i++) {
            Periodo periodo = new Periodo();
            periodo.setSala(sala);
            preencherPeriodo(periodo, i + 1, periodosDto.get(i));
            sala.getPeriodos().add(periodo);
        }
    }

    private void preencherPeriodo(Periodo periodo, int ordem, PeriodoRequestDTO dto) {
        periodo.setOrdem(ordem);
        periodo.setDataInicio(dto.getDataInicio());
        periodo.setDataFim(dto.getDataFim());
        periodo.setPontuacaoMaxima(dto.getPontuacaoMaxima());
    }

    private void validarPeriodos(TipoPeriodo tipo, List<PeriodoRequestDTO> periodos) {
        if (periodos.size() != tipo.getQuantidade()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Uma sala dividida por " + tipo.getRotulo().toLowerCase(Locale.ROOT)
                            + " precisa de " + tipo.getQuantidade() + " períodos");
        }

        for (int i = 0; i < periodos.size(); i++) {
            PeriodoRequestDTO atual = periodos.get(i);
            String nome = tipo.nomeDoPeriodo(i + 1);

            if (atual.getDataFim().isBefore(atual.getDataInicio())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No " + nome + ", a data de fim deve ser depois da data de início");
            }

            if (i > 0 && !atual.getDataInicio().isAfter(periodos.get(i - 1).getDataFim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O " + nome + " deve começar depois do fim do " + tipo.nomeDoPeriodo(i));
            }
        }
    }

    // depois de editar os períodos, cada atividade é ligada de novo ao período da data de entrega dela
    // e a soma de pontos por matéria/período é conferida de novo. se alguma ficar fora ou passar do máximo
    // dá erro e nada é salvo, pq tá tudo na mesma transação
    private void reencaixarAtividadesNosPeriodos(Sala sala) {
        Map<String, Double> somaPorMateriaEPeriodo = new HashMap<>();
        Map<String, Periodo> periodoPorChave = new HashMap<>();

        for (Atividade atividade : sala.getAtividades()) {
            Periodo periodo = sala.periodoDaData(atividade.getDataEntrega())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "A atividade \"" + atividade.getTitulo() + "\" (entrega em "
                                    + atividade.getDataEntrega().format(DATA_BR)
                                    + ") ficaria fora dos períodos letivos"));
            atividade.setPeriodo(periodo);

            String chave = atividade.getMateria().getId() + "|" + periodo.getOrdem();
            somaPorMateriaEPeriodo.merge(chave, atividade.getValor(), Double::sum);
            periodoPorChave.put(chave, periodo);
        }

        for (Nota nota : notaRepository.findBySala_Id(sala.getId())) {
            if (!nota.isAvulsa()) {
                nota.setPeriodo(nota.getAtividade().getPeriodo());
                continue;
            }
            Periodo periodoDaNota = sala.periodoDaData(nota.getData())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Uma nota avulsa lançada por um aluno (data " + nota.getData().format(DATA_BR)
                                    + ") ficaria fora dos períodos letivos"));
            nota.setPeriodo(periodoDaNota);
        }

        for (Map.Entry<String, Double> soma : somaPorMateriaEPeriodo.entrySet()) {
            Periodo periodo = periodoPorChave.get(soma.getKey());
            if (soma.getValue() > periodo.getPontuacaoMaxima() + 0.000001d) {
                String idMateria = soma.getKey().substring(0, soma.getKey().indexOf('|'));
                String nomeMateria = sala.getMaterias().stream()
                        .filter(materia -> idMateria.equals(materia.getId()))
                        .map(Materia::getNome)
                        .findFirst()
                        .orElse("uma matéria");
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        String.format("%s já tem %.2f pontos em atividades no %s, acima da nova pontuação máxima de %.2f",
                                nomeMateria, soma.getValue(), periodo.getNome(), periodo.getPontuacaoMaxima()));
            }
        }
    }

    @Transactional
    public SalaResponseDTO entrar(String codigoConvite, String idUsuario) {
        // os códigos são gerados em maiúsculas, ent "a3f9c1" tbm tem q achar a sala "A3F9C1"
        Sala sala = salaRepository.findByCodigoConviteForUpdate(codigoConvite.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        boolean jaMembro = membrosRepository.existsBySala_IdAndUsuario_Id(
                sala.getId(),
                usuario.getId()
        );

        if (jaMembro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já está nesta sala");
        }

        if (sala.getMembros().size() >= MAXIMO_MEMBROS_POR_SALA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta sala já atingiu o limite de " + MAXIMO_MEMBROS_POR_SALA + " alunos");
        }

        Membros novoMembro = new Membros();
        novoMembro.setSala(sala);
        novoMembro.setUsuario(usuario);
        novoMembro.setDataEntrada(LocalDateTime.now());

        sala.getMembros().add(novoMembro);

        Sala salaSalva = salaRepository.save(sala);

        Map<String, String> statusPorAtividade = buscarStatusPorAtividade(
                salaSalva.getId(),
                idUsuario
        );

        return new SalaResponseDTO(salaSalva, statusPorAtividade);
    }

    public SalaResponseDTO buscarPorId(String idSala, String idUsuarioLogado) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Map<String, String> statusPorAtividade = buscarStatusPorAtividade(
                idSala,
                idUsuarioLogado
        );

        return new SalaResponseDTO(sala, statusPorAtividade);
    }

    public List<SalaResumoDTO> listarPorUsuario(String idUsuario) {
        return salaRepository.findByMembros_Usuario_Id(idUsuario)
                .stream()
                .map(SalaResumoDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void excluir(String id) {
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        notaRepository.deleteBySala(sala.getId());

        // as tarefas dos grupos apontam pras atividades da sala. sem apagar elas antes,
        // o hibernate tenta excluir as atividades primeiro e o banco recusa
        List<Tarefa> tarefas = tarefaRepository.findByGrupo_Sala_Id(sala.getId());
        tarefas.forEach(tarefa -> notificacaoService.removerPorAlvo(tarefa.getId()));
        tarefaRepository.deleteAll(tarefas);
        tarefaRepository.flush();

        // tira as notificações que apontam pra atividades e grupos q vão deixar de existir
        sala.getAtividades().forEach(atividade -> notificacaoService.removerPorAlvo(atividade.getId()));
        sala.getGrupos().forEach(grupo -> notificacaoService.removerPorAlvo(grupo.getId()));

        salaRepository.delete(sala);
    }

    // o líder passa a liderança pra outro membro da sala. depois disso ele vira um membro comum,
    // e pode sair da sala ou desativar a conta
    @Transactional
    public SalaResponseDTO transferirLideranca(String idSala, String idNovoLider, String idUsuarioLogado) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        if (!sala.getLider().getId().equals(idUsuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o líder da sala pode transferir a liderança");
        }
        if (sala.getLider().getId().equals(idNovoLider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você já é o líder desta sala");
        }

        Membros novoLider = membrosRepository.findBySala_IdAndUsuario_Id(idSala, idNovoLider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O novo líder precisa ser membro da sala"));
        if (!novoLider.getUsuario().isAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível passar a liderança para uma conta desativada");
        }

        sala.setLider(novoLider.getUsuario());
        Sala salaAtualizada = salaRepository.save(sala);

        notificacaoService.criarParaUsuario(
                idNovoLider,
                "SALA",
                "Você é o novo líder",
                "Agora você é o líder da sala " + sala.getNome(),
                sala.getId());

        return new SalaResponseDTO(salaAtualizada, buscarStatusPorAtividade(idSala, idUsuarioLogado));
    }

    @Transactional
    public void excluirMembro(String idSala, String idUsuarioRemover, String idUsuarioLogado) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        if (!sala.getLider().getId().equals(idUsuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o líder da sala pode remover membros");
        }

        if (sala.getLider().getId().equals(idUsuarioRemover)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O líder da sala não pode ser removido");
        }

        removerVinculoDoUsuario(sala, idUsuarioRemover);
    }

    @Transactional
    public void sair(String idSala, String idUsuario) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        if (sala.getLider().getId().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O líder da sala não pode sair da sala");
        }

        removerVinculoDoUsuario(sala, idUsuario);
    }

    private void removerVinculoDoUsuario(Sala sala, String idUsuario) {
        Membros membro = membrosRepository
                .findBySala_IdAndUsuario_Id(sala.getId(), idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado nesta sala"));

        cadernoRepository.deleteAll(
                cadernoRepository.findByAtividade_Sala_IdAndUsuario_Id(sala.getId(), idUsuario)
        );

        notaRepository.deleteBySalaAndUsuario(sala.getId(), idUsuario);

        grupoService.removerUsuarioDosGruposDaSala(sala.getId(), idUsuario);

        sala.getMembros().removeIf(m -> m.getId().equals(membro.getId()));
        membrosRepository.delete(membro);
    }

    private Map<String, String> buscarStatusPorAtividade(String idSala, String idUsuario) {
        return cadernoRepository
                .findByAtividade_Sala_IdAndUsuario_Id(idSala, idUsuario)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getAtividade().getId(),
                        Caderno::getStatus
                ));
    }

    private String gerarCodigoConvite() {
        String codigo;

        do {
            codigo = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
        } while (salaRepository.existsByCodigoConvite(codigo));

        return codigo;
    }

    @Transactional(readOnly = true)
    public void validarMembro(String idSala, String idUsuario) {
        if (!membrosRepository.existsBySala_IdAndUsuario_Id(idSala, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta sala");
        }
    }

    public void validarLider(String idSala, String idUsuario) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        if (!sala.getLider().getId().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o líder da sala pode fazer isso");
        }
    }
}
