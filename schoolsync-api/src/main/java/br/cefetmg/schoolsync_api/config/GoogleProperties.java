package br.cefetmg.schoolsync_api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
