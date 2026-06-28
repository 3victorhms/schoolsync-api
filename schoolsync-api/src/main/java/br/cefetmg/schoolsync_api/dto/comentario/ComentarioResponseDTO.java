package br.cefetmg.schoolsync_api.dto.comentario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import br.cefetmg.schoolsync_api.entity.Comentario;
import lombok.Getter;

@Getter
public class ComentarioResponseDTO {

    private String id;
    private String texto;
    private LocalDateTime dataCriacao;
    private String idAtividade;
    private String idUsuario;
    private String nomeUsuario;
    private String fotoUsuario;
    private String idComentarioPai;
    private List<ComentarioResponseDTO> respostas;

    public ComentarioResponseDTO(Comentario comentario) {
        this.id = comentario.getId();
        this.texto = comentario.getTexto();
        this.dataCriacao = comentario.getDataCriacao();
        this.idAtividade = comentario.getAtividade().getId();
        this.idUsuario = comentario.getUsuario().getId();
        this.nomeUsuario = comentario.getUsuario().getNome();
        this.fotoUsuario = comentario.getUsuario().getFoto();
        this.idComentarioPai = comentario.getComentarioPai() == null
                ? null
                : comentario.getComentarioPai().getId();
        this.respostas = comentario.getRespostas().stream()
                .map(ComentarioResponseDTO::new)
                .collect(Collectors.toList());
    }
}
