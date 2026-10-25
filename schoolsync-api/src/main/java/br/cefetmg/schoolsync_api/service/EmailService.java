package br.cefetmg.schoolsync_api.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.config.RecuperacaoSenhaProperties;

/**
 * Feito com auxílio do Claude: envio de e-mail transacional pela API da Brevo
 * (POST https://api.brevo.com/v3/smtp/email, autenticado pelo cabeçalho "api-key").
 */
@Service
public class EmailService {

    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final RecuperacaoSenhaProperties properties;
    private final RestClient restClient;

    public EmailService(RecuperacaoSenhaProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public boolean configurado() {
        return properties.configurado();
    }

    public void enviar(String paraEmail, String paraNome, String assunto, String html, String texto) {
        if (!configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O envio de e-mail não está configurado no servidor");
        }

        Map<String, Object> corpo = Map.of(
                "sender", Map.of("email", properties.remetenteEmail(), "name", properties.nomeRemetente()),
                "to", List.of(Map.of("email", paraEmail, "name", paraNome == null || paraNome.isBlank() ? paraEmail : paraNome)),
                "subject", assunto,
                "htmlContent", html,
                "textContent", texto);

        try {
            restClient.post()
                    .uri(URL_BREVO)
                    .header("api-key", properties.brevoApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // n registra o corpo nem o destinatário completo no log
            log.error("Falha ao enviar e-mail pela Brevo: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail agora. Tente de novo em alguns minutos");
        }
    }
}
