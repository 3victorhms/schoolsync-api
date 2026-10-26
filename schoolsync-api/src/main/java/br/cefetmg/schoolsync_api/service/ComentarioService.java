package br.cefetmg.schoolsync_api.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.comentario.ComentarioRequestDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioResponseDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioUpdateDTO;
import br.cefetmg.schoolsync_api.dto.pagina.PaginaDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Comentario;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.ComentarioRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
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
    private final MembrosRepository membrosRepository;

    @Transactional
    public ComentarioResponseDTO criar(String idAtividade, ComentarioRequestDTO dto, String idUsuario) {
        Atividade atividade = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new EntityNotFoundException("Atividade nao encontrada"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
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
                throw new IllegalArgumentException("O comentário respondido não pertence a esta atividade");
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

    /**
     * Feito com auxílio do Claude: os comentários principais mais recentes (cada um com as
     * respostas). A página vem em ordem cronológica, igual antes; {@code antesDe} busca os anteriores.
     */
    @Transactional(readOnly = true)
    public PaginaDTO<ComentarioResponseDTO> listarPaginaPorAtividade(String idAtividade, int tamanho, LocalDateTime antesDe) {
        if (!atividadeRepository.existsById(idAtividade)) {
            throw new EntityNotFoundException("Atividade nao encontrada");
        }

        Pageable pagina = PageRequest.of(0, tamanho + 1, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        List<Comentario> encontrados = antesDe == null
                ? comentarioRepository.findByAtividade_IdAndComentarioPaiIsNull(idAtividade, pagina)
                : comentarioRepository.findByAtividade_IdAndComentarioPaiIsNullAndDataCriacaoBefore(idAtividade, antesDe, pagina);

        List<ComentarioResponseDTO> itens = new ArrayList<>(encontrados.stream()
                .limit(tamanho)
                .map(ComentarioResponseDTO::new)
                .collect(Collectors.toList()));
        // buscou do mais novo para o mais antigo; a tela mostra do mais antigo para o mais novo
        Collections.reverse(itens);
        return new PaginaDTO<>(itens, encontrados.size() > tamanho);
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
    public ComentarioResponseDTO atualizar(String idComentario, ComentarioUpdateDTO dto, String idUsuario) {
        Comentario comentario = comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new EntityNotFoundException("Comentario nao encontrado"));

        validarAutor(comentario, idUsuario);

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o autor pode alterar este comentário");
        }
        // o autor tbm precisa continuar na sala da atividade
        String idSala = comentario.getAtividade().getSala().getId();
        if (!membrosRepository.existsBySala_IdAndUsuario_Id(idSala, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta sala");
        }
    }
}
