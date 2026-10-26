package br.cefetmg.schoolsync_api.service;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.config.GoogleProperties;

// confere o ID token que o app recebeu do google. quem valida a assinatura é o próprio google
// (endpoint tokeninfo); aqui a gente confere se o token é pro nosso app (aud), quem emitiu (iss),
// se ainda vale (exp) e se o e-mail foi confirmado pelo google
@Service
public class GoogleTokenVerifier {

    static final String URL_TOKENINFO = "https://oauth2.googleapis.com/tokeninfo";
    private static final Set<String> EMISSORES = Set.of("accounts.google.com", "https://accounts.google.com");

    private final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private final GoogleProperties properties;
    private final RestClient restClient;
    private final Clock relogio;

    public record ContaGoogle(String id, String email, String nome, String foto) {}

    @Autowired
    public GoogleTokenVerifier(GoogleProperties properties) {
        this(properties, RestClient.create(), Clock.systemUTC());
    }

    // pros testes trocarem o RestClient e o relógio
    GoogleTokenVerifier(GoogleProperties properties, RestClient restClient, Clock relogio) {
        this.properties = properties;
        this.restClient = restClient;
        this.relogio = relogio;
    }

    public ContaGoogle verificar(String idToken) {
        if (!properties.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O login com Google não está configurado no servidor");
        }

        Map<String, Object> dados;
        try {
            dados = restClient.get()
                    .uri(URL_TOKENINFO + "?id_token={token}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException e) {
            // o google responde 400 pra token inválido, alterado ou expirado
            throw tokenInvalido();
        } catch (RestClientException e) {
            log.error("Falha ao consultar o Google: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível falar com o Google agora. Tente de novo em instantes");
        }
        if (dados == null) {
            throw tokenInvalido();
        }

        String id = texto(dados, "sub");
        String email = texto(dados, "email");
        if (id == null || email == null
                || !properties.clientIds().contains(texto(dados, "aud"))
                || !EMISSORES.contains(texto(dados, "iss"))
                || expirado(texto(dados, "exp"))) {
            throw tokenInvalido();
        }
        if (!"true".equalsIgnoreCase(texto(dados, "email_verified"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "O e-mail dessa conta Google ainda não foi confirmado");
        }

        return new ContaGoogle(id, email.trim().toLowerCase(Locale.ROOT), texto(dados, "name"), texto(dados, "picture"));
    }

    private boolean expirado(String exp) {
        try {
            return exp == null || Long.parseLong(exp) <= relogio.instant().getEpochSecond();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static String texto(Map<String, Object> dados, String chave) {
        Object valor = dados.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }

    private static ResponseStatusException tokenInvalido() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível confirmar sua conta Google. Tente de novo");
    }
}
