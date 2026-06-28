package br.cefetmg.schoolsync_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Tarefa;

public interface TarefaRepository extends JpaRepository<Tarefa, String> {

    List<Tarefa> findByGrupo_IdOrderByDataCriacaoAsc(String idGrupo);

    List<Tarefa> findByAtribuidoPara_IdOrderByDataCriacaoAsc(String idUsuario);
}
