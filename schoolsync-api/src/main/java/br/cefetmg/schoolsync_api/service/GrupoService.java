package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.grupo.GrupoRequestDTO;
import br.cefetmg.schoolsync_api.dto.grupo.GrupoResponseDTO;
import br.cefetmg.schoolsync_api.dto.grupo.GrupoResumoDTO;
import br.cefetmg.schoolsync_api.entity.Grupo;
import br.cefetmg.schoolsync_api.entity.GrupoMembro;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.GrupoMembroRepository;
import br.cefetmg.schoolsync_api.repository.GrupoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoMembroRepository grupoMembroRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembrosRepository membrosRepository;
    private final NotificacaoService notificacaoService;
    private final TarefaRepository tarefaRepository;

    @Transactional
    public GrupoResponseDTO criar(GrupoRequestDTO dto, String idCriador) {
        Sala sala = salaRepository.findById(dto.getIdSala())
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        Usuario criador = usuarioRepository.findById(idCriador)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        validarMembroDaSala(sala.getId(), criador.getId());

        Grupo grupo = new Grupo();
        grupo.setNome(dto.getNome());
        grupo.setSala(sala);
        grupo.setCriador(criador);
        grupo.setCodigoConvite(gerarCodigoConvite());

        GrupoMembro membroCriador = new GrupoMembro();
        membroCriador.setGrupo(grupo);
        membroCriador.setUsuario(criador);
        membroCriador.setDataEntrada(LocalDateTime.now());

        grupo.getMembros().add(membroCriador);

        Grupo grupoSalvo = grupoRepository.save(grupo);

        return new GrupoResponseDTO(grupoSalvo, criador.getId());
    }

    @Transactional
    public GrupoResponseDTO entrar(String codigoConvite, String idUsuario) {
        // os códigos são gerados em maiúsculas, ent aceita o código digitado em minúsculas tbm
        Grupo grupo = grupoRepository.findByCodigoConvite(codigoConvite.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("Grupo não encontrado"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        validarMembroDaSala(grupo.getSala().getId(), usuario.getId());

        if (grupoMembroRepository.existsByGrupo_IdAndUsuario_Id(grupo.getId(), usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já está neste grupo");
        }

        GrupoMembro membro = new GrupoMembro();
        membro.setGrupo(grupo);
        membro.setUsuario(usuario);
        membro.setDataEntrada(LocalDateTime.now());

        grupo.getMembros().add(membro);

        Grupo grupoSalvo = grupoRepository.save(grupo);

        if (!grupo.getCriador().getId().equals(usuario.getId())) {
            notificacaoService.criarParaUsuario(
                    grupo.getCriador().getId(),
                    "GRUPO",
                    "Novo membro no grupo",
                    usuario.getNome() + " entrou no grupo " + grupo.getNome(),
                    grupo.getId()
            );
        }

        return new GrupoResponseDTO(grupoSalvo, usuario.getId());
    }

    @Transactional(readOnly = true)
    public GrupoResponseDTO buscarPorId(String idGrupo, String idUsuarioLogado) {
        Grupo grupo = buscarGrupo(idGrupo);
        validarMembroDoGrupo(grupo.getId(), idUsuarioLogado);

        return new GrupoResponseDTO(grupo, idUsuarioLogado);
    }

    @Transactional(readOnly = true)
    public List<GrupoResumoDTO> listarPorSalaEUsuario(String idSala, String idUsuario) {
        return grupoRepository.findBySala_IdAndMembros_Usuario_Id(idSala, idUsuario)
                .stream()
                .map(grupo -> new GrupoResumoDTO(grupo, idUsuario))
                .collect(Collectors.toList());
    }

    @Transactional
    public GrupoResponseDTO atualizar(String idGrupo, GrupoRequestDTO dto, String idUsuarioLogado) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (!grupo.getCriador().getId().equals(idUsuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o criador do grupo pode editar o grupo");
        }

        grupo.setNome(dto.getNome());

        return new GrupoResponseDTO(grupoRepository.save(grupo), idUsuarioLogado);
    }

    @Transactional
    public void excluir(String idGrupo, String idUsuarioLogado) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (!grupo.getCriador().getId().equals(idUsuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o criador do grupo pode excluir o grupo");
        }

        // tira as notificações do grupo e das tarefas dele, pra ninguém abrir um aviso de algo q n existe mais
        grupo.getTarefas().forEach(tarefa -> notificacaoService.removerPorAlvo(tarefa.getId()));
        notificacaoService.removerPorAlvo(grupo.getId());
        grupoRepository.delete(grupo);
    }

    // o líder do grupo passa a liderança pra outro membro. quem recebe passa a poder criar,
    // atribuir e excluir tarefas, e o antigo líder vira um membro comum
    @Transactional
    public GrupoResponseDTO transferirLideranca(String idGrupo, String idNovoLider, String idUsuarioLogado) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (!grupo.getCriador().getId().equals(idUsuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o líder do grupo pode transferir a liderança");
        }
        if (grupo.getCriador().getId().equals(idNovoLider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você já é o líder deste grupo");
        }

        GrupoMembro novoLider = grupoMembroRepository.findByGrupo_IdAndUsuario_Id(idGrupo, idNovoLider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O novo líder precisa ser membro do grupo"));
        if (!novoLider.getUsuario().isAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível passar a liderança para uma conta desativada");
        }

        grupo.setCriador(novoLider.getUsuario());
        Grupo grupoAtualizado = grupoRepository.save(grupo);

        notificacaoService.criarParaUsuario(
                idNovoLider,
                "GRUPO",
                "Você é o novo líder",
                "Agora você é o líder do grupo " + grupo.getNome(),
                grupo.getId());

        return new GrupoResponseDTO(grupoAtualizado, idUsuarioLogado);
    }

    @Transactional
    public void sair(String idGrupo, String idUsuario) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (grupo.getCriador().getId().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O criador do grupo não pode sair do grupo");
        }

        removerDoGrupo(grupo, idUsuario);
    }

    @Transactional
    public void removerMembro(String idGrupo, String idUsuarioRemover, String idUsuarioLogado) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (!grupo.getCriador().getId().equals(idUsuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o líder do grupo pode remover membros");
        }

        if (grupo.getCriador().getId().equals(idUsuarioRemover)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O líder do grupo não pode ser removido");
        }

        removerDoGrupo(grupo, idUsuarioRemover);
    }

    // quando alguém sai ou é removido da sala, sai tbm de todos os grupos dela
    // se era líder de algum grupo, o membro mais antigo vira o líder. se o grupo ficar vazio ele é excluído
    @Transactional
    public void removerUsuarioDosGruposDaSala(String idSala, String idUsuario) {
        List<Grupo> grupos = grupoRepository.findBySala_IdAndMembros_Usuario_Id(idSala, idUsuario);

        for (Grupo grupo : grupos) {
            if (grupo.getCriador().getId().equals(idUsuario)) {
                Optional<GrupoMembro> sucessor = grupo.getMembros().stream()
                        .filter(m -> !m.getUsuario().getId().equals(idUsuario))
                        .findFirst(); // membros já vêm ordenados por data de entrada

                if (sucessor.isEmpty()) {
                    grupoRepository.delete(grupo);
                    continue;
                }

                grupo.setCriador(sucessor.get().getUsuario());
                grupoRepository.save(grupo);
            }

            removerDoGrupo(grupo, idUsuario);
        }
    }

    private void removerDoGrupo(Grupo grupo, String idUsuario) {
        GrupoMembro membro = grupoMembroRepository
                .findByGrupo_IdAndUsuario_Id(grupo.getId(), idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado neste grupo"));

        List<Tarefa> tarefasDoMembro = tarefaRepository.findByGrupo_IdOrderByDataCriacaoAsc(grupo.getId())
                .stream()
                .filter(tarefa -> tarefa.getAtribuidoPara() != null
                        && tarefa.getAtribuidoPara().getId().equals(idUsuario))
                .collect(Collectors.toList());
        tarefasDoMembro.forEach(tarefa -> tarefa.setAtribuidoPara(grupo.getCriador()));
        tarefaRepository.saveAll(tarefasDoMembro);

        grupo.getMembros().removeIf(m -> m.getId().equals(membro.getId()));
        grupoMembroRepository.delete(membro);
    }

    Grupo buscarGrupo(String idGrupo) {
        return grupoRepository.findById(idGrupo)
                .orElseThrow(() -> new EntityNotFoundException("Grupo não encontrado"));
    }

    void validarCriadorDoGrupo(Grupo grupo, String idUsuario) {
        if (!grupo.getCriador().getId().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o criador do grupo pode atribuir tarefas");
        }
    }

    void validarMembroDoGrupo(String idGrupo, String idUsuario) {
        if (!grupoMembroRepository.existsByGrupo_IdAndUsuario_Id(idGrupo, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa deste grupo");
        }
    }

    // aqui é 400 e n 403 pq o problema é o usuário escolhido pra tarefa, n a permissão de quem tá logado
    void validarAtribuidoNoGrupo(String idGrupo, String idUsuario) {
        if (!grupoMembroRepository.existsByGrupo_IdAndUsuario_Id(idGrupo, idUsuario)) {
            throw new IllegalArgumentException("O usuário escolhido não pertence a este grupo");
        }
    }

    private void validarMembroDaSala(String idSala, String idUsuario) {
        if (!membrosRepository.existsBySala_IdAndUsuario_Id(idSala, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta sala");
        }
    }

    private String gerarCodigoConvite() {
        String codigo;

        do {
            codigo = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
        } while (grupoRepository.existsByCodigoConvite(codigo));

        return codigo;
    }
}
