package br.cefetmg.schoolsync_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {

    List<Notificacao> findByUsuario_IdOrderByHorarioDesc(String idUsuario);

    List<Notificacao> findByUsuario_IdAndLidoFalse(String idUsuario);
}
