package br.cefetmg.schoolsync_api.dto.atividade;

import java.time.LocalDate;

import br.cefetmg.schoolsync_api.entity.Atividade;
import lombok.Getter;

@Getter
public class AtividadeResponseDTO {

    private String id;
    private String titulo;
    private String descricao;
    private String idMateria;
    private String nomeMateria;
    private String idPeriodo;
    /** Ex.: "2º Bimestre". Calculado pela data de entrega. */
    private String nomePeriodo;
    private LocalDate dataEntrega;
    private Double valor;
    private String idSala;
    private String idCriador;
    /** Líder da sala: também pode editar e excluir a atividade (moderação). */
    private String idLiderSala;

    private boolean estaNoCaderno;
    private String status;

    public AtividadeResponseDTO(Atividade atividade) {
        this(atividade, null);
    }

    public AtividadeResponseDTO(Atividade atividade, String status) {
        this.id = atividade.getId();
        this.titulo = atividade.getTitulo();
        this.descricao = atividade.getDescricao();
        this.idMateria = atividade.getMateria().getId();
        this.nomeMateria = atividade.getMateria().getNome();
        this.idPeriodo = atividade.getPeriodo().getId();
        this.nomePeriodo = atividade.getPeriodo().getNome();
        this.dataEntrega = atividade.getDataEntrega();
        this.valor = atividade.getValor();
        this.idSala = atividade.getSala().getId();
        this.idCriador = atividade.getCriadaPor().getId();
        this.idLiderSala = atividade.getSala().getLider().getId();

        this.estaNoCaderno = status != null;
        this.status = status;
    }
}
