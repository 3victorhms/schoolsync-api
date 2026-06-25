package br.cefetmg.schoolsync_api.repository;

import br.cefetmg.schoolsync_api.entity.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, String> {
    Optional<Sala> findByCodigoConvite(String codigoConvite);

    boolean existsByCodigoConvite(String codigoConvite);

    List<Sala> findByMembros_Usuario_Id(String idUsuario);
}
