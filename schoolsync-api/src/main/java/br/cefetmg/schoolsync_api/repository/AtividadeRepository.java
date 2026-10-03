package br.cefetmg.schoolsync_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Atividade;

public interface AtividadeRepository extends JpaRepository<Atividade, String> {

    List<Atividade> findBySala_Id(String idSala);

    /** Atividades de uma matéria dentro de um período: base do limite de pontuação. */
    List<Atividade> findByMateria_IdAndPeriodo_Id(String idMateria, String idPeriodo);

    boolean existsByMateria_Id(String idMateria);
}
