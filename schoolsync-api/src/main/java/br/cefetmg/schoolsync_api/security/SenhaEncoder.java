package br.cefetmg.schoolsync_api.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

// Essa classe irá criptografar a senha do usuário em hash, o que vai garantir uma melhor segurança
// Como não aprendi isso no curso técnico do CEFET, tirei as informações desses artigos:
// https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
// https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html

@Component
public class SenhaEncoder {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String criptografar(String senha) {
        return encoder.encode(senha);
    }

    public boolean verificar(String senhaDigitada, String senhaHash) {
        return encoder.matches(senhaDigitada, senhaHash);
    }
}
