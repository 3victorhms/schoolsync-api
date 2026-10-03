package br.cefetmg.schoolsync_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.cefetmg.schoolsync_api.entity.Nota;

public interface NotaRepository extends JpaRepository<Nota, String> {

    List<Nota> findBySala_IdAndUsuario_IdOrderByDataAsc(String idSala, String idUsuario);

    List<Nota> findBySala_Id(String idSala);

    List<Nota> findByAtividade_Id(String idAtividade);

    Optional<Nota> findByAtividade_IdAndUsuario_Id(String idAtividade, String idUsuario);

    /** Notas avulsas do aluno numa matéria/período: entram no limite de pontos do período. */
    List<Nota> findByUsuario_IdAndMateria_IdAndPeriodo_IdAndAtividadeIsNull(String idUsuario, String idMateria, String idPeriodo);

    boolean existsByMateria_Id(String idMateria);

    boolean existsBySala_IdAndAtividadeIsNull(String idSala);

    @Modifying
    @Query("delete from Nota n where n.sala.id = :idSala")
    void deleteBySala(@Param("idSala") String idSala);

    @Modifying
    @Query("delete from Nota n where n.sala.id = :idSala and n.usuario.id = :idUsuario")
    void deleteBySalaAndUsuario(@Param("idSala") String idSala, @Param("idUsuario") String idUsuario);
}
