package br.cefetmg.schoolsync_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, String> {

    List<Comentario> findByAtividade_IdAndComentarioPaiIsNullOrderByDataCriacaoAsc(String idAtividade);
}
