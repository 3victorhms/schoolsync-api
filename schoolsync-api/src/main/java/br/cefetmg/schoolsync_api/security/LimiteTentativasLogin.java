package br.cefetmg.schoolsync_api.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class LimiteTentativasLogin {

    static final int MAX_FALHAS_POR_EMAIL = 5;
    static final int MAX_FALHAS_POR_IP = 20;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private final Clock relogio;
    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();

    @Autowired
    public LimiteTentativasLogin() {
        this(Clock.systemUTC());
    }

    // construtor para os testes controlarem o tempo
    LimiteTentativasLogin(Clock relogio) {
        this.relogio = relogio;
    }

    public void verificar(String email, String ip) {
        Instant agora = relogio.instant();
        Instant bloqueadoAte = maisTarde(bloqueio(chaveEmail(email), agora), bloqueio(chaveIp(ip), agora));
        if (bloqueadoAte != null) {
            long minutos = Math.max(1, (Duration.between(agora, bloqueadoAte).toSeconds() + 59) / 60);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Tente de novo em " + minutos + (minutos == 1 ? " minuto" : " minutos"));
        }
    }

    public void registrarFalha(String email, String ip) {
        Instant agora = relogio.instant();
        contar(chaveEmail(email), MAX_FALHAS_POR_EMAIL, agora);
        contar(chaveIp(ip), MAX_FALHAS_POR_IP, agora);
        limparSePrecisar(agora);
    }

    public void registrarSucesso(String email) {
        contadores.remove(chaveEmail(email));
    }

    public String ipDoCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            String[] partes = encaminhado.split(",");
            return partes[partes.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private void contar(String chave, int limite, Instant agora) {
        contadores.compute(chave, (k, atual) -> {
            Contador contador = atual == null || atual.expirou(agora) ? new Contador(agora) : atual;
            contador.falhas++;
            if (contador.falhas >= limite) {
                contador.bloqueadoAte = agora.plus(BLOQUEIO);
            }
            return contador;
        });
    }

    private Instant bloqueio(String chave, Instant agora) {
        Contador contador = contadores.get(chave);
        if (contador == null || contador.bloqueadoAte == null || !contador.bloqueadoAte.isAfter(agora)) {
            return null;
        }
        return contador.bloqueadoAte;
    }

    private void limparSePrecisar(Instant agora) {
        if (contadores.size() > LIMPAR_ACIMA_DE) {
            contadores.entrySet().removeIf(entrada -> entrada.getValue().expirou(agora));
        }
    }

    private static Instant maisTarde(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static String chaveEmail(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static String chaveIp(String ip) {
        return "ip:" + (ip == null ? "" : ip);
    }

    private static final class Contador {
        private final Instant inicio;
        private int falhas;
        private Instant bloqueadoAte;

        private Contador(Instant inicio) {
            this.inicio = inicio;
        }

        // a janela de contagem passou e não há bloqueio em andamento
        private boolean expirou(Instant agora) {
            boolean janelaPassou = !inicio.plus(JANELA).isAfter(agora);
            boolean semBloqueio = bloqueadoAte == null || !bloqueadoAte.isAfter(agora);
            return janelaPassou && semBloqueio;
        }
    }
}
