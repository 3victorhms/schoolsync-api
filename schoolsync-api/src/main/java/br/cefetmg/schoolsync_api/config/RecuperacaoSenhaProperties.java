package br.cefetmg.schoolsync_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recuperacao-senha")
public record RecuperacaoSenhaProperties(
        String brevoApiKey,
        String remetenteEmail,
        String remetenteNome,
        Integer validadeMinutos) {

    public boolean configurado() {
        return naoVazio(brevoApiKey) && naoVazio(remetenteEmail);
    }

    public int validade() {
        return validadeMinutos == null || validadeMinutos <= 0 ? 15 : validadeMinutos;
    }

    public String nomeRemetente() {
        return naoVazio(remetenteNome) ? remetenteNome : "SchoolSync";
    }

    private boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }
}
