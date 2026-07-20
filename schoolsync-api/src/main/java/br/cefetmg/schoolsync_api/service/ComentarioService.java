package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.comentario.ComentarioRequestDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioResponseDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioUpdateDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Comentario;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.ComentarioRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final AtividadeRepository atividadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    @Transactional
    public ComentarioResponseDTO criar(String idAtividade, ComentarioRequestDTO dto) {
        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        Comentario comentario = new Comentario();
        comentario.setTexto(dto.getTexto());
        comentario.setAtividade(atividade);
        comentario.setUsuario(usuario);
        comentario.setDataCriacao(LocalDateTime.now());

        if (dto.getIdComentarioPai() != null && !dto.getIdComentarioPai().isBlank()) {
            Comentario comentarioPai = comentarioRepository.findById(dto.getIdComentarioPai())
                    .orElseThrow(() -> new EntityNotFoundException("Comentario pai nao encontrado"));

            if (!comentarioPai.getAtividade().getId().equals(idAtividade)) {
                throw new IllegalArgumentException("Comentario pai nao pertence a atividade informada");
            }

            comentario.setComentarioPai(comentarioPai);
        }

        Comentario comentarioSalvo = comentarioRepository.save(comentario);
        notificarComentario(comentarioSalvo);

        return new ComentarioResponseDTO(comentarioSalvo);
    }

    private void notificarComentario(Comentario comentario) {
        String idAutor = comentario.getUsuario().getId();
        String idDonoAtividade = comentario.getAtividade().getCriadaPor().getId();

        if (!idDonoAtividade.equals(idAutor)) {
            notificacaoService.criarParaUsuario(
                    idDonoAtividade,
                    "ATIVIDADE",
                    "Novo comentario",
                    comentario.getUsuario().getNome() + " comentou na atividade " + comentario.getAtividade().getTitulo(),
                    comentario.getAtividade().getId()
            );
        }

        if (comentario.getComentarioPai() != null) {
            String idAutorComentarioPai = comentario.getComentarioPai().getUsuario().getId();
            if (!idAutorComentarioPai.equals(idAutor) && !idAutorComentarioPai.equals(idDonoAtividade)) {
                notificacaoService.criarParaUsuario(
                        idAutorComentarioPai,
                        "ATIVIDADE",
                        "Nova resposta",
                        comentario.getUsuario().getNome() + " respondeu seu comentario",
                        comentario.getAtividade().getId()
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponseDTO> listarPorAtividade(String idAtividade) {
        if (!atividadeRepository.existsById(idAtividade)) {
            throw new EntityNotFoundException("Atividade nao encontrada");
        }

        return comentarioRepository
                .findByAtividade_IdAndComentarioPaiIsNullOrderByDataCriacaoAsc(idAtividade)
                .stream()
                .map(ComentarioResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComentarioResponseDTO atualizar(String idComentario, ComentarioUpdateDTO dto) {
        Comentario comentario = comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new EntityNotFoundException("Comentario nao encontrado"));

        validarAutor(comentario, dto.getIdUsuario());

        comentario.setTexto(dto.getTexto());

        return new ComentarioResponseDTO(comentarioRepository.save(comentario));
    }

    @Transactional
    public void excluir(String idComentario, String idUsuario) {
        Comentario comentario = comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new EntityNotFoundException("Comentario nao encontrado"));

        validarAutor(comentario, idUsuario);

        comentarioRepository.delete(comentario);
    }

    private void validarAutor(Comentario comentario, String idUsuario) {
        if (!comentario.getUsuario().getId().equals(idUsuario)) {
            throw new IllegalArgumentException("Apenas o autor pode alterar este comentario");
        }
    }
}
