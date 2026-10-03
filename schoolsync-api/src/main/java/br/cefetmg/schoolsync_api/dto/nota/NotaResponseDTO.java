package br.cefetmg.schoolsync_api.dto.nota;

import java.time.LocalDate;

import br.cefetmg.schoolsync_api.entity.Nota;
import lombok.Getter;

@Getter
public class NotaResponseDTO {

    private String id;
    private String idSala;
    private String idMateria;
    private String nomeMateria;
    private String idPeriodo;
    private String nomePeriodo;
    /** Null na avulsa. */
    private String idAtividade;
    /** Título da atividade, ou a descrição informada na avulsa. */
    private String descricao;
    private boolean avulsa;
    private Double valorObtido;
    private Double valorMaximo;
    private LocalDate data;

    public NotaResponseDTO(Nota nota) {
        this.id = nota.getId();
        this.idSala = nota.getSala().getId();
        this.idMateria = nota.getMateria().getId();
        this.nomeMateria = nota.getMateria().getNome();
        this.idPeriodo = nota.getPeriodo().getId();
        this.nomePeriodo = nota.getPeriodo().getNome();
        this.avulsa = nota.isAvulsa();
        this.idAtividade = nota.isAvulsa() ? null : nota.getAtividade().getId();
        this.descricao = nota.isAvulsa() ? nota.getDescricao() : nota.getAtividade().getTitulo();
        this.valorObtido = nota.getValorObtido();
        this.valorMaximo = nota.getValorMaximo();
        this.data = nota.getData();
    }
}
