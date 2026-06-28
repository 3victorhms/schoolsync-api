package br.cefetmg.schoolsync_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.GrupoMembro;

public interface GrupoMembroRepository extends JpaRepository<GrupoMembro, String> {

    boolean existsByGrupo_IdAndUsuario_Id(String idGrupo, String idUsuario);

    Optional<GrupoMembro> findByGrupo_IdAndUsuario_Id(String idGrupo, String idUsuario);
}
