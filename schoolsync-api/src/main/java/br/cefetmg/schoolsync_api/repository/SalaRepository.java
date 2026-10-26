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

    // trava a linha da sala no banco (select ... for update) até a transação terminar
    // sem isso, se dois alunos entrarem ao mesmo tempo numa sala com 49, os dois passam e ela fica com 51
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sala from Sala sala where sala.codigoConvite = :codigoConvite")
    Optional<Sala> findByCodigoConviteForUpdate(@Param("codigoConvite") String codigoConvite);

    boolean existsByCodigoConvite(String codigoConvite);

    List<Sala> findByMembros_Usuario_Id(String idUsuario);

    boolean existsByLider_Id(String idUsuario);

    // mesma ideia do de cima, mas pelo id. é usado ao criar/editar atividade e ao editar a sala,
    // pra duas requisições ao mesmo tempo n furarem o limite de pontos do período
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sala from Sala sala where sala.id = :id")
    Optional<Sala> findByIdForUpdate(@Param("id") String id);
}
