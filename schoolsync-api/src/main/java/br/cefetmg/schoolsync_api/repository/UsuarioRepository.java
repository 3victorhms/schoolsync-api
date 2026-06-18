package br.cefetmg.schoolsync_api.repository;

import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.dto.usuario.UsuarioResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailAndSenha(String email, String senha);

    boolean existsByEmail(String email);


}