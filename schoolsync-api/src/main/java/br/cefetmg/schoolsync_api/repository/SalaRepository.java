package br.cefetmg.schoolsync_api.repository;

import br.cefetmg.schoolsync_api.entity.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, String> {
    Optional<Sala> findByCodigoConvite(String codigoConvite);

    boolean existsByCodigoConvite(String codigoConvite);

    List<Sala> findByMembros_Usuario_Id(String idUsuario);

    boolean existsByLider_Id(String idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sala from Sala sala where sala.id = :id")
    Optional<Sala> findByIdForUpdate(@Param("id") String id);
}
