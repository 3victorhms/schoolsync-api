package br.cefetmg.schoolsync_api.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um bimestre/trimestre/semestre da sala. A soma dos valores das atividades de
 * uma matéria dentro do período não pode passar de pontuacaoMaxima.
 */
@Entity
@Table(name = "tb_periodo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Periodo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** 1, 2, 3... na ordem do ano letivo. */
    @Column(nullable = false)
    private Integer ordem;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "pontuacao_maxima", nullable = false)
    private Double pontuacaoMaxima;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sala", nullable = false)
    private Sala sala;

    /** A data cai dentro do período (início e fim inclusos). */
    public boolean contem(LocalDate data) {
        return data != null && !data.isBefore(dataInicio) && !data.isAfter(dataFim);
    }

    /** "2º Bimestre", "1º Semestre"... depende do tipo escolhido na sala. */
    public String getNome() {
        return sala.getTipoPeriodo().nomeDoPeriodo(ordem);
    }
}
