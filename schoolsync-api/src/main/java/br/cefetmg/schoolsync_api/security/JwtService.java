package br.cefetmg.schoolsync_api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.cefetmg.schoolsync_api.entity.Usuario;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String gerarToken(Usuario usuario) {
        long now = Instant.now().toEpochMilli();
        long exp = now + expirationMs;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"nome\":\"%s\",\"iat\":%d,\"exp\":%d}",
                escape(usuario.getId()),
                escape(usuario.getEmail()),
                escape(usuario.getNome()),
                now / 1000,
                exp / 1000
        );

        String unsignedToken = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(payload.getBytes(StandardCharsets.UTF_8));

        return unsignedToken + "." + assinar(unsignedToken);
    }

    public boolean tokenValido(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return false;
            }

            String assinaturaEsperada = assinar(partes[0] + "." + partes[1]);
            if (!constantTimeEquals(assinaturaEsperada, partes[2])) {
                return false;
            }

            Long exp = extrairLong(token, "exp");
            return exp != null && exp > Instant.now().getEpochSecond();
        } catch (Exception ex) {
            return false;
        }
    }

    public String extrairIdUsuario(String token) {
        return extrairString(token, "sub");
    }

    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel assinar o token", ex);
        }
    }

    private String extrairString(String token, String campo) {
        String payload = payload(token);
        String marcador = "\"" + campo + "\":\"";
        int inicio = payload.indexOf(marcador);
        if (inicio < 0) {
            return null;
        }
        inicio += marcador.length();
        int fim = payload.indexOf("\"", inicio);
        return fim < 0 ? null : payload.substring(inicio, fim);
    }

    private Long extrairLong(String token, String campo) {
        String payload = payload(token);
        String marcador = "\"" + campo + "\":";
        int inicio = payload.indexOf(marcador);
        if (inicio < 0) {
            return null;
        }
        inicio += marcador.length();
        int fim = inicio;
        while (fim < payload.length() && Character.isDigit(payload.charAt(fim))) {
            fim++;
        }
        return Long.parseLong(payload.substring(inicio, fim));
    }

    private String payload(String token) {
        String[] partes = token.split("\\.");
        return new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigestTiming.equals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
