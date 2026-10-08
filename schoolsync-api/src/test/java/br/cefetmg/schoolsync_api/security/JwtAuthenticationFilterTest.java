package br.cefetmg.schoolsync_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.cefetmg.schoolsync_api.entity.Perfil;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtAuthenticationFilter filter;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void naoAutenticaTokenDeUsuarioInativo() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");
        usuario.setAtivo(false);

        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.tokenValido("token-valido")).thenReturn(true);
        when(jwtService.extrairIdUsuario("token-valido")).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    private String roleAutenticada(Usuario usuario) throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.tokenValido("token-valido")).thenReturn(true);
        when(jwtService.extrairIdUsuario("token-valido")).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        filter.doFilterInternal(request, response, filterChain);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
    }

    @Test
    void usuarioNovoEntraComPerfilUsuario() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");

        assertEquals("ROLE_USUARIO", roleAutenticada(usuario));
    }

    @Test
    void administradorEntraComPerfilAdmin() throws Exception {
        Usuario admin = new Usuario();
        admin.setId("admin-1");
        admin.setPerfil(Perfil.ADMIN);

        assertEquals("ROLE_ADMIN", roleAutenticada(admin));
    }
}
