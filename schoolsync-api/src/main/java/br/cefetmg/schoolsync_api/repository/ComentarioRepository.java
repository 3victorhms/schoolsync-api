package br.cefetmg.schoolsync_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, String> {

    List<Comentario> findByAtividade_IdAndComentarioPaiIsNullOrderByDataCriacaoAsc(String idAtividade);

    // paginação por cursor (ver Paginacao): a ordem vem do Pageable
    List<Comentario> findByAtividade_IdAndComentarioPaiIsNull(String idAtividade, Pageable pageable);

    List<Comentario> findByAtividade_IdAndComentarioPaiIsNullAndDataCriacaoBefore(
            String idAtividade, LocalDateTime antesDe, Pageable pageable);
}
