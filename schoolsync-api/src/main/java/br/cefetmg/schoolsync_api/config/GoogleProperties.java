package br.cefetmg.schoolsync_api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

// client ids do Google que podem pedir login (o web, que o app usa no android e no navegador).
// vem do GOOGLE_CLIENT_IDS, separados por vírgula. vazio = login com google desligado
@ConfigurationProperties(prefix = "google")
public record GoogleProperties(List<String> clientIds) {

    public GoogleProperties {
        clientIds = clientIds == null ? List.of()
                : clientIds.stream().map(String::trim).filter(id -> !id.isEmpty()).toList();
    }

    public boolean configurado() {
        return !clientIds.isEmpty();
    }
}
