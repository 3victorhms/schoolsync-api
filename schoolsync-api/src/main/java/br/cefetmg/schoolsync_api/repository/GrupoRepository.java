package br.cefetmg.schoolsync_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Grupo;

public interface GrupoRepository extends JpaRepository<Grupo, String> {

    Optional<Grupo> findByCodigoConvite(String codigoConvite);

    boolean existsByCodigoConvite(String codigoConvite);

    List<Grupo> findBySala_IdAndMembros_Usuario_Id(String idSala, String idUsuario);
}
