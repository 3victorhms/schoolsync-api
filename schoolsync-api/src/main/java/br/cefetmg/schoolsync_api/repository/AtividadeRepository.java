package br.cefetmg.schoolsync_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Atividade;

public interface AtividadeRepository extends JpaRepository<Atividade, String> {

    List<Atividade> findBySala_Id(String idSala);
}