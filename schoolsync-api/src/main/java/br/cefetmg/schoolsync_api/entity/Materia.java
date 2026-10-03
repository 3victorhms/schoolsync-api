package br.cefetmg.schoolsync_api.entity;

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
 * Matéria da sala (v2). Antes a disciplina era um texto livre em cada atividade;
 * agora o líder cadastra as matérias e as atividades escolhem uma delas.
 *
 * O nome não tem unique no banco de propósito: ao renomear/remover matérias na
 * mesma requisição o Hibernate faz os INSERTs antes dos DELETEs e a constraint
 * quebraria. A unicidade (sem diferenciar maiúsculas) é garantida no SalaService.
 */
@Entity
@Table(name = "tb_materia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sala", nullable = false)
    private Sala sala;
}
