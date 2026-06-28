package br.cefetmg.schoolsync_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Membros;

public interface MembrosRepository extends JpaRepository<Membros, String> {
    boolean existsBySala_IdAndUsuario_Id(String idSala, String idUsuario);
    List<Membros> findByUsuario_Id(String idUsuario);
    Optional<Membros> findBySala_IdAndUsuario_Id(String idSala, String idUsuario);
}
