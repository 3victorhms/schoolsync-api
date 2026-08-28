package br.cefetmg.schoolsync_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.cefetmg.schoolsync_api.entity.DispositivoPush;

@Repository
public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, String> {

    Optional<DispositivoPush> findByToken(String token);

    List<DispositivoPush> findAllByUsuario_Id(String idUsuario);

    void deleteByUsuario_IdAndToken(String idUsuario, String token);

    void deleteAllByUsuario_Id(String idUsuario);
}
