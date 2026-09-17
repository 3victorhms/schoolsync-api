package br.cefetmg.schoolsync_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {

    List<Notificacao> findByUsuario_IdOrderByHorarioDesc(String idUsuario);

    List<Notificacao> findByUsuario_IdAndLidoFalse(String idUsuario);

    Optional<Notificacao> findFirstByUsuario_IdAndTipoAndTargetIdAndLidoFalseOrderByHorarioDesc(
            String idUsuario, String tipo, String targetId);

    boolean existsByUsuario_IdAndTipoAndTargetId(String idUsuario, String tipo, String targetId);

    void deleteByTargetId(String targetId);
}
