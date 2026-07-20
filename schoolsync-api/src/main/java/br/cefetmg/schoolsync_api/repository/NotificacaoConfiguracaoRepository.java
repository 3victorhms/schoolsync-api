package br.cefetmg.schoolsync_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.cefetmg.schoolsync_api.entity.NotificacaoConfiguracao;

public interface NotificacaoConfiguracaoRepository extends JpaRepository<NotificacaoConfiguracao, String> {

    Optional<NotificacaoConfiguracao> findByUsuario_Id(String idUsuario);
}
