package br.cefetmg.schoolsync_api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Caderno;

public interface CadernoRepository extends JpaRepository<Caderno, String> {
    List<Caderno> findByAtividade_Sala_IdAndUsuario_Id(String idSala, String idUsuario);

    List<Caderno> findByUsuario_Id(String idUsuario);

    List<Caderno> findByAtividade_DataEntrega(LocalDate dataEntrega);

    @Override
    Optional<Caderno> findById(String s);

    Optional<Caderno> findByAtividade_IdAndUsuario_Id(String idAtividade, String idUsuario);
}
