package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.sala.SalaRequestDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResponseDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResumoDTO;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.Membros;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalaService {

    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembrosRepository membrosRepository;
    private final CadernoRepository cadernoRepository;

    public SalaResponseDTO criar(SalaRequestDTO dto, String idLider) {
        Usuario lider = usuarioRepository.findById(idLider)
                .orElseThrow(() -> new EntityNotFoundException("Usuario lider nao encontrado"));

        Sala sala = new Sala();
        sala.setNome(dto.getNome());
        sala.setCodigoConvite(gerarCodigoConvite());
        sala.setLider(lider);

        Membros membroLider = new Membros();
        membroLider.setSala(sala);
        membroLider.setUsuario(lider);
        membroLider.setDataEntrada(LocalDateTime.now());

        sala.getMembros().add(membroLider);

        Sala salaSalva = salaRepository.save(sala);

        return new SalaResponseDTO(salaSalva, Map.of());
    }

    public SalaResponseDTO atualizar(String id, SalaRequestDTO dto) {
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        sala.setNome(dto.getNome());

        Sala salaAtualizada = salaRepository.save(sala);

        return new SalaResponseDTO(salaAtualizada, Map.of());
    }

    public SalaResponseDTO entrar(String codigoConvite, String idUsuario) {
        Sala sala = salaRepository.findByCodigoConvite(codigoConvite)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        boolean jaMembro = membrosRepository.existsBySala_IdAndUsuario_Id(
                sala.getId(),
                usuario.getId()
        );

        if (jaMembro) {
            throw new IllegalArgumentException("Voce ja esta nesta sala");
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

        salaRepository.delete(sala);
    }

    @Transactional
    public void excluirMembro(String idSala, String idUsuarioRemover, String idUsuarioLogado) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala nao encontrada"));

        if (!sala.getLider().getId().equals(idUsuarioLogado)) {
            throw new IllegalArgumentException("Apenas o lider da sala pode remover membros");
        }

        if (sala.getLider().getId().equals(idUsuarioRemover)) {
            throw new IllegalArgumentException("O lider da sala nao pode ser removido");
        }

        removerVinculoDoUsuario(sala, idUsuarioRemover);
    }

    @Transactional
    public void sair(String idSala, String idUsuario) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        if (sala.getLider().getId().equals(idUsuario)) {
            throw new IllegalArgumentException("O lider da sala não pode sair da sala");
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
}
