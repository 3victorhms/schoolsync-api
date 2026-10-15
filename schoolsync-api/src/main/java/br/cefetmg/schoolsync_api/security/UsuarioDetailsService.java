package br.cefetmg.schoolsync_api.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.cefetmg.schoolsync_api.entity.Perfil;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

// usado pelo AuthenticationManager no login: busca o usuário pelo e-mail e devolve
// o e-mail, o hash da senha e a role pro spring security comparar a senha com o BCrypt
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        Perfil perfil = usuario.getPerfil() == null ? Perfil.USUARIO : usuario.getPerfil();

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(perfil.name()) // vira ROLE_USUARIO ou ROLE_ADMIN
                .disabled(!usuario.isAtivo()) // conta desativada n consegue fazer login
                .build();
    }
}
