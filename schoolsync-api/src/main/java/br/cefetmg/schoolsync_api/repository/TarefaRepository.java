package br.cefetmg.schoolsync_api.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Tarefa;

public interface TarefaRepository extends JpaRepository<Tarefa, String> {

    List<Tarefa> findByGrupo_IdOrderByDataCriacaoAsc(String idGrupo);

    List<Tarefa> findByAtribuidoPara_IdOrderByDataCriacaoAsc(String idUsuario);

    List<Tarefa> findByAtividade_Id(String idAtividade);

    List<Tarefa> findByGrupo_Sala_Id(String idSala);

    // lembrete: tarefas q vencem numa data e ainda n foram concluídas
    List<Tarefa> findByPrazoAndStatusNot(LocalDate prazo, String status);
}
