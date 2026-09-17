package br.cefetmg.schoolsync_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {

    public boolean configurado() {
        return naoVazio(cloudName) && naoVazio(apiKey) && naoVazio(apiSecret);
    }

    private boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }
}
