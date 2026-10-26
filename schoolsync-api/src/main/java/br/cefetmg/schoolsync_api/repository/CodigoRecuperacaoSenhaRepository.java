package br.cefetmg.schoolsync_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.cefetmg.schoolsync_api.entity.CodigoRecuperacaoSenha;
import br.cefetmg.schoolsync_api.entity.Usuario;

@Repository
public interface CodigoRecuperacaoSenhaRepository extends JpaRepository<CodigoRecuperacaoSenha, String> {

    // o código mais recente do usuário (os anteriores são apagados ao gerar um novo)
    Optional<CodigoRecuperacaoSenha> findFirstByUsuarioOrderByCriadoEmDesc(Usuario usuario);

    void deleteByUsuario(Usuario usuario);
}
