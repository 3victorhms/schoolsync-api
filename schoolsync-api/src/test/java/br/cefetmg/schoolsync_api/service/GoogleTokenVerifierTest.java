package br.cefetmg.schoolsync_api.service;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.config.GoogleProperties;
import br.cefetmg.schoolsync_api.service.GoogleTokenVerifier.ContaGoogle;

// o google é simulado pelo MockRestServiceServer: nenhum teste chama a internet
class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "123-web.apps.googleusercontent.com";
    private static final Instant AGORA = Instant.parse("2026-10-26T12:00:00Z");

    private MockRestServiceServer google;
    private GoogleTokenVerifier verificador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        google = MockRestServiceServer.bindTo(builder).build();
        verificador = new GoogleTokenVerifier(new GoogleProperties(List.of(CLIENT_ID)), builder.build(),
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private void googleResponde(String aud, String iss, long exp, String emailVerificado) {
        String json = """
                {"sub": "g-42", "email": "Marcos@Gmail.com", "email_verified": "%s", "name": "Marcos Daniel",
                 "picture": "https://foto", "aud": "%s", "iss": "%s", "exp": "%d"}
                """.formatted(emailVerificado, aud, iss, exp);
        google.expect(requestTo(startsWith(GoogleTokenVerifier.URL_TOKENINFO)))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void tokenValidoDevolveAContaComEmailMinusculo() {
        googleResponde(CLIENT_ID, "https://accounts.google.com", AGORA.getEpochSecond() + 600, "true");

        ContaGoogle conta = verificador.verificar("token");

        assertEquals("g-42", conta.id());
        assertEquals("marcos@gmail.com", conta.email());
        assertEquals("Marcos Daniel", conta.nome());
        google.verify();
    }

    @Test
    void tokenDeOutroAppEhRecusado() {
        googleResponde("outro-app.apps.googleusercontent.com", "accounts.google.com", AGORA.getEpochSecond() + 600, "true");

        assertEquals(401, assertThrows(ResponseStatusException.class, () -> verificador.verificar("token")).getStatusCode().value());
    }

    @Test
    void tokenExpiradoEhRecusado() {
        googleResponde(CLIENT_ID, "accounts.google.com", AGORA.getEpochSecond() - 1, "true");

        assertEquals(401, assertThrows(ResponseStatusException.class, () -> verificador.verificar("token")).getStatusCode().value());
    }

    @Test
    void emailNaoConfirmadoEhRecusado() {
        googleResponde(CLIENT_ID, "accounts.google.com", AGORA.getEpochSecond() + 600, "false");

        assertEquals(401, assertThrows(ResponseStatusException.class, () -> verificador.verificar("token")).getStatusCode().value());
    }

    @Test
    void tokenQueOGoogleNaoReconheceEhRecusado() {
        google.expect(requestTo(startsWith(GoogleTokenVerifier.URL_TOKENINFO))).andRespond(withBadRequest());

        assertEquals(401, assertThrows(ResponseStatusException.class, () -> verificador.verificar("token")).getStatusCode().value());
    }

    @Test
    void semClientIdConfiguradoResponde503() {
        GoogleTokenVerifier semConfiguracao = new GoogleTokenVerifier(new GoogleProperties(List.of()),
                RestClient.create(), Clock.fixed(AGORA, ZoneOffset.UTC));

        assertEquals(503, assertThrows(ResponseStatusException.class, () -> semConfiguracao.verificar("token")).getStatusCode().value());
    }
}
