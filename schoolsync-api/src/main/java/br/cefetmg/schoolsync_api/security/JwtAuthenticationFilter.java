package br.cefetmg.schoolsync_api.security;

import java.io.IOException;
import java.util.List;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.cefetmg.schoolsync_api.entity.Perfil;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String token = null;

        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else if (ehStreamDeNotificacoes(request)) {
            token = request.getParameter("token");
        }

        if (token != null && !token.isBlank()) {

            if (jwtService.tokenValido(token)) {
                String idUsuario = jwtService.extrairIdUsuario(token);
                String tokenAtual = token;
                usuarioRepository.findById(idUsuario)
                        .filter(usuario -> !emitidoAntesDaTrocaDeCredenciais(tokenAtual, usuario))
                        .ifPresent(this::autenticar);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean ehStreamDeNotificacoes(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && request.getRequestURI().endsWith("/notificacoes/stream");
    }

    private boolean emitidoAntesDaTrocaDeCredenciais(String token, Usuario usuario) {
        if (usuario.getCredenciaisAlteradasEm() == null) {
            return false;
        }
        Date emitidoEm = jwtService.extrairEmitidoEm(token);
        if (emitidoEm == null) {
            return true;
        }
        long alteradoEm = usuario.getCredenciaisAlteradasEm().atZone(ZoneId.systemDefault()).toEpochSecond();
        return emitidoEm.getTime() / 1000 < alteradoEm;
    }

    private void autenticar(Usuario usuario) {
        if (!usuario.isAtivo()) {
            return;
        }

        Perfil perfil = usuario.getPerfil() == null ? Perfil.USUARIO : usuario.getPerfil();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
