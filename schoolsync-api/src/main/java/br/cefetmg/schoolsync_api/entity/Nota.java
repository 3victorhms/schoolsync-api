package br.cefetmg.schoolsync_api.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nota pessoal do boletim (v2). Só o próprio aluno vê e lança.
 *
 * Dois formatos:
 *  - ligada a uma atividade da sala: matéria, período, valor máximo e data vêm da atividade;
 *  - avulsa (atividade = null): o aluno informa descrição, matéria, valor máximo e data,
 *    para registrar notas antigas ou que não foram cadastradas como atividade.
 *
 * Matéria, período e valor máximo ficam gravados nos dois casos (nas ligadas, são copiados
 * da atividade e atualizados quando ela muda) para o boletim sair de uma consulta simples.
 */
@Entity
@Table(
        name = "tb_nota",
        uniqueConstraints = @UniqueConstraint(columnNames = { "id_usuario", "id_atividade" })
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sala", nullable = false)
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_materia", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_periodo", nullable = false)
    private Periodo periodo;

    /** Null quando a nota é avulsa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_atividade")
    private Atividade atividade;

    /** Obrigatória na avulsa (ex.: "Prova de recuperação de março"). */
    @Column(length = 150)
    private String descricao;

    @Column(name = "valor_obtido", nullable = false)
    private Double valorObtido;

    @Column(name = "valor_maximo", nullable = false)
    private Double valorMaximo;

    /** Data da avaliação: a entrega da atividade, ou a informada na avulsa. */
    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    public boolean isAvulsa() {
        return atividade == null;
    }
}
