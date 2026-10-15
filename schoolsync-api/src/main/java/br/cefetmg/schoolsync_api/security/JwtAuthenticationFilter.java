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
            // o EventSource do navegador n manda cabeçalho, ent no stream de notificações o token vem pela url (?token=)
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

    // se o e-mail ou a senha mudaram depois que o token foi gerado, ele n vale mais.
    // a comparação é em segundos pq o iat do token só guarda segundos
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

        // o perfil vem do banco a cada requisição, ent se o perfil de alguém mudar já vale sem gerar outro token.
        // ele vira a role do spring security (ROLE_USUARIO ou ROLE_ADMIN)
        Perfil perfil = usuario.getPerfil() == null ? Perfil.USUARIO : usuario.getPerfil();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
