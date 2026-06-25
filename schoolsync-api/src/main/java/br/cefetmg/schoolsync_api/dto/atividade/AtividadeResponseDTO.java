package br.cefetmg.schoolsync_api.dto.atividade;

import java.time.LocalDate;

import br.cefetmg.schoolsync_api.entity.Atividade;
import lombok.Getter;

@Getter
public class AtividadeResponseDTO {

    private String id;
    private String titulo;
    private String descricao;
    private String disciplina;
    private LocalDate dataEntrega;
    private Double valor;
    private String idSala;
    private String idCriador;

    private boolean estaNoCaderno;
    private String status;

    public AtividadeResponseDTO(Atividade atividade) {
        this(atividade, null);
    }

    public AtividadeResponseDTO(Atividade atividade, String status) {
        this.id = atividade.getId();
        this.titulo = atividade.getTitulo();
        this.descricao = atividade.getDescricao();
        this.disciplina = atividade.getDisciplina();
        this.dataEntrega = atividade.getDataEntrega();
        this.valor = atividade.getValor();
        this.idSala = atividade.getSala().getId();
        this.idCriador = atividade.getCriadaPor().getId();

        this.estaNoCaderno = status != null;
        this.status = status;
    }
}
