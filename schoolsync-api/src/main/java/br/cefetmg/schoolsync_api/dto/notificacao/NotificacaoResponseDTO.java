package br.cefetmg.schoolsync_api.dto.notificacao;

import br.cefetmg.schoolsync_api.entity.Notificacao;
import lombok.Getter;

@Getter
public class NotificacaoResponseDTO {

    private String id;
    private String tipo;
    private String titulo;
    private String mensagem;
    private String horario;
    private boolean lido;
    private String targetId;

    public NotificacaoResponseDTO(Notificacao notificacao) {
        this.id = notificacao.getId();
        this.tipo = notificacao.getTipo();
        this.titulo = notificacao.getTitulo();
        this.mensagem = notificacao.getMensagem();
        this.horario = notificacao.getHorario().toString();
        this.lido = notificacao.isLido();
        this.targetId = notificacao.getTargetId();
    }
}
