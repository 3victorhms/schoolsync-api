package br.cefetmg.schoolsync_api.dto.tarefa;

import java.time.LocalDateTime;

import br.cefetmg.schoolsync_api.entity.Tarefa;
import lombok.Getter;

@Getter
public class TarefaResponseDTO {

    private String id;
    private String titulo;
    private String status;
    private LocalDateTime dataCriacao;
    private String idGrupo;
    private String idAtividade;
    private String tituloAtividade;
    private String disciplinaAtividade;
    private String idUsuarioAtribuido;
    private String nomeUsuarioAtribuido;
    private String idCriador;
    private String nomeCriador;

    public TarefaResponseDTO(Tarefa tarefa) {
        this.id = tarefa.getId();
        this.titulo = tarefa.getTitulo();
        this.status = tarefa.getStatus();
        this.dataCriacao = tarefa.getDataCriacao();
        this.idGrupo = tarefa.getGrupo().getId();
        this.idAtividade = tarefa.getAtividade().getId();
        this.tituloAtividade = tarefa.getAtividade().getTitulo();
        this.disciplinaAtividade = tarefa.getAtividade().getDisciplina();
        this.idUsuarioAtribuido = tarefa.getAtribuidoPara().getId();
        this.nomeUsuarioAtribuido = tarefa.getAtribuidoPara().getNome();
        this.idCriador = tarefa.getCriadaPor().getId();
        this.nomeCriador = tarefa.getCriadaPor().getNome();
    }
}
