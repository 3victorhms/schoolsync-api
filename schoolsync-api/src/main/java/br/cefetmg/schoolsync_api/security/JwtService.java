package br.cefetmg.schoolsync_api.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.cefetmg.schoolsync_api.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey chave; // chave usada para assinar e validar o token
    private final long expirationMs; // tempo de expiração do token em milissegundos

    // construtor que recebe as configurações direto do arquivo yaml
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        // o jjwt exige uma chave de pelo menos 32 caracteres pro HS256, senão a aplicação nem sobe
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // gera o token com o id do usuário no subject e o e-mail e o nome como claims (o app lê esses dados)
    // o exp é um timestamp: o sistema salva só um número grande em vez de uma data em texto
    // https://hkotsubo.github.io/blog/2019-05-02/o-que-e-timestamp
    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(usuario.getId())
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    // o parser confere a assinatura e a expiração. se o token foi alterado ou já expirou ele lança exceção
    public boolean tokenValido(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extrairIdUsuario(String token) {
        return getClaims(token).getSubject();
    }

    public Date extrairEmitidoEm(String token) {
        return getClaims(token).getIssuedAt();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
