package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.grupo.GrupoRequestDTO;
import br.cefetmg.schoolsync_api.dto.grupo.GrupoResponseDTO;
import br.cefetmg.schoolsync_api.dto.grupo.GrupoResumoDTO;
import br.cefetmg.schoolsync_api.entity.Grupo;
import br.cefetmg.schoolsync_api.entity.GrupoMembro;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.GrupoMembroRepository;
import br.cefetmg.schoolsync_api.repository.GrupoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
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

    @Transactional
    public GrupoResponseDTO criar(GrupoRequestDTO dto) {
        Sala sala = salaRepository.findById(dto.getIdSala())
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Usuario criador = usuarioRepository.findById(dto.getIdCriador())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

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
        Grupo grupo = grupoRepository.findByCodigoConvite(codigoConvite)
                .orElseThrow(() -> new EntityNotFoundException("Grupo nao encontrado"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        validarMembroDaSala(grupo.getSala().getId(), usuario.getId());

        if (grupoMembroRepository.existsByGrupo_IdAndUsuario_Id(grupo.getId(), usuario.getId())) {
            throw new IllegalArgumentException("Voce ja esta neste grupo");
        }

        GrupoMembro membro = new GrupoMembro();
        membro.setGrupo(grupo);
        membro.setUsuario(usuario);
        membro.setDataEntrada(LocalDateTime.now());

        grupo.getMembros().add(membro);

        Grupo grupoSalvo = grupoRepository.save(grupo);

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
    public GrupoResponseDTO atualizar(String idGrupo, GrupoRequestDTO dto) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (!grupo.getCriador().getId().equals(dto.getIdCriador())) {
            throw new IllegalArgumentException("Apenas o criador do grupo pode atualizar o grupo");
        }

        grupo.setNome(dto.getNome());

        return new GrupoResponseDTO(grupoRepository.save(grupo), dto.getIdCriador());
    }

    @Transactional
    public void excluir(String idGrupo, String idUsuarioLogado) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (!grupo.getCriador().getId().equals(idUsuarioLogado)) {
            throw new IllegalArgumentException("Apenas o criador do grupo pode excluir o grupo");
        }

        grupoRepository.delete(grupo);
    }

    @Transactional
    public void sair(String idGrupo, String idUsuario) {
        Grupo grupo = buscarGrupo(idGrupo);

        if (grupo.getCriador().getId().equals(idUsuario)) {
            throw new IllegalArgumentException("O criador do grupo nao pode sair por enquanto");
        }

        GrupoMembro membro = grupoMembroRepository
                .findByGrupo_IdAndUsuario_Id(idGrupo, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado neste grupo"));

        grupo.getMembros().removeIf(m -> m.getId().equals(membro.getId()));
        grupoMembroRepository.delete(membro);
    }

    Grupo buscarGrupo(String idGrupo) {
        return grupoRepository.findById(idGrupo)
                .orElseThrow(() -> new EntityNotFoundException("Grupo nao encontrado"));
    }

    void validarCriadorDoGrupo(Grupo grupo, String idUsuario) {
        if (!grupo.getCriador().getId().equals(idUsuario)) {
            throw new IllegalArgumentException("Apenas o criador do grupo pode atribuir tarefas");
        }
    }

    void validarMembroDoGrupo(String idGrupo, String idUsuario) {
        if (!grupoMembroRepository.existsByGrupo_IdAndUsuario_Id(idGrupo, idUsuario)) {
            throw new IllegalArgumentException("Usuario nao pertence a este grupo");
        }
    }

    private void validarMembroDaSala(String idSala, String idUsuario) {
        if (!membrosRepository.existsBySala_IdAndUsuario_Id(idSala, idUsuario)) {
            throw new IllegalArgumentException("Usuario nao pertence a esta sala");
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
