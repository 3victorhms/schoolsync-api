package br.cefetmg.schoolsync_api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {

    List<Notificacao> findByUsuario_IdOrderByHorarioDesc(String idUsuario);

    List<Notificacao> findByUsuario_IdAndLidoFalse(String idUsuario);

    // paginação por cursor (ver Paginacao): a ordem vem do Pageable
    List<Notificacao> findByUsuario_Id(String idUsuario, Pageable pageable);

    List<Notificacao> findByUsuario_IdAndHorarioBefore(String idUsuario, LocalDateTime antesDe, Pageable pageable);

    long countByUsuario_IdAndLidoFalse(String idUsuario);

    Optional<Notificacao> findFirstByUsuario_IdAndTipoAndTargetIdAndLidoFalseOrderByHorarioDesc(
            String idUsuario, String tipo, String targetId);

    boolean existsByUsuario_IdAndTipoAndTargetId(String idUsuario, String tipo, String targetId);

    void deleteByTargetId(String targetId);

    void deleteByTargetIdStartingWith(String prefixo);

    // limpeza: notificações já lidas e mais antigas que a data
    long deleteByLidoTrueAndHorarioBefore(LocalDateTime limite);
}
