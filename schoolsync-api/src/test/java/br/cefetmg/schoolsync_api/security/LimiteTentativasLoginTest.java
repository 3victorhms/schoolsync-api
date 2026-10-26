package br.cefetmg.schoolsync_api.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

// Feito com auxílio do Claude
class LimiteTentativasLoginTest {

    /** Relógio que o teste avança na mão. */
    private static final class RelogioFalso extends Clock {
        private Instant agora = Instant.parse("2026-10-26T12:00:00Z");

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private RelogioFalso relogio;
    private LimiteTentativasLogin limite;

    @BeforeEach
    void preparar() {
        relogio = new RelogioFalso();
        limite = new LimiteTentativasLogin(relogio);
    }

    private void errar(String email, String ip, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.registrarFalha(email, ip);
        }
    }

    @Test
    void quatroErrosAindaDeixamTentar() {
        errar("aluno@exemplo.com", "1.1.1.1", LimiteTentativasLogin.MAX_FALHAS_POR_EMAIL - 1);
        assertDoesNotThrow(() -> limite.verificar("aluno@exemplo.com", "1.1.1.1"));
    }

    @Test
    void cincoErrosBloqueiamOEmailCom429() {
        errar("aluno@exemplo.com", "1.1.1.1", LimiteTentativasLogin.MAX_FALHAS_POR_EMAIL);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> limite.verificar("aluno@exemplo.com", "2.2.2.2"));
        assertEquals(429, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("15 minutos"));
        // o bloqueio é do e-mail, não de quem está no mesmo IP com outra conta
        assertDoesNotThrow(() -> limite.verificar("outro@exemplo.com", "1.1.1.1"));
    }

    @Test
    void oEmailNaoDiferenciaMaiusculas() {
        errar("Aluno@Exemplo.com", "1.1.1.1", LimiteTentativasLogin.MAX_FALHAS_POR_EMAIL);
        assertThrows(ResponseStatusException.class, () -> limite.verificar("aluno@exemplo.com", "3.3.3.3"));
    }

    @Test
    void oBloqueioAcabaDepoisDe15Minutos() {
        errar("aluno@exemplo.com", "1.1.1.1", LimiteTentativasLogin.MAX_FALHAS_POR_EMAIL);
        relogio.avancar(Duration.ofMinutes(14));
        assertThrows(ResponseStatusException.class, () -> limite.verificar("aluno@exemplo.com", "1.1.1.1"));

        relogio.avancar(Duration.ofMinutes(2));
        assertDoesNotThrow(() -> limite.verificar("aluno@exemplo.com", "1.1.1.1"));
    }

    @Test
    void errosEspalhadosNaoSomamDepoisDaJanela() {
        errar("aluno@exemplo.com", "1.1.1.1", 3);
        relogio.avancar(Duration.ofMinutes(16));
        errar("aluno@exemplo.com", "1.1.1.1", 3);
        assertDoesNotThrow(() -> limite.verificar("aluno@exemplo.com", "1.1.1.1"));
    }

    @Test
    void loginCertoZeraOContadorDoEmail() {
        errar("aluno@exemplo.com", "1.1.1.1", LimiteTentativasLogin.MAX_FALHAS_POR_EMAIL - 1);
        limite.registrarSucesso("aluno@exemplo.com");
        errar("aluno@exemplo.com", "1.1.1.1", LimiteTentativasLogin.MAX_FALHAS_POR_EMAIL - 1);
        assertDoesNotThrow(() -> limite.verificar("aluno@exemplo.com", "1.1.1.1"));
    }

    @Test
    void muitosEmailsDiferentesDoMesmoIpBloqueiamOIp() {
        for (int i = 0; i < LimiteTentativasLogin.MAX_FALHAS_POR_IP; i++) {
            limite.registrarFalha("conta" + i + "@exemplo.com", "9.9.9.9");
        }
        assertThrows(ResponseStatusException.class, () -> limite.verificar("nova@exemplo.com", "9.9.9.9"));
        assertDoesNotThrow(() -> limite.verificar("nova@exemplo.com", "8.8.8.8"));
    }

    @Test
    void usaOUltimoIpDoXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 200.10.20.30");
        assertEquals("200.10.20.30", limite.ipDoCliente(request));

        MockHttpServletRequest semProxy = new MockHttpServletRequest();
        semProxy.setRemoteAddr("10.0.0.1");
        assertEquals("10.0.0.1", limite.ipDoCliente(semProxy));
    }
}
