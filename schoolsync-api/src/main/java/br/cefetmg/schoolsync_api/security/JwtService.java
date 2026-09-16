package br.cefetmg.schoolsync_api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import br.cefetmg.schoolsync_api.entity.Usuario;

@Service
public class JwtService {

    private final String secret; // chave usada para assinar e validar o token
    private final long expirationMs; // tempo de expiração do token em milissegundos

    // construtor que recebe as configurações direto do arquivo yaml
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * método para um token JWT contendo os dados do usuário autenticado.
     */
    public String gerarToken(Usuario usuario) {
        long now = Instant.now().toEpochMilli(); // recebe o timestamp atual em milissegundos (valor númerico para algum
                                                 // momento no tempo)
        long exp = now + expirationMs; // timestamp de quando o token vai expirar

        // em vez de salvar textos como "15/09/2026 às 00:00", o sistema salva apenas um
        // número grande (por exemplo, 1789441200).
        // https://hkotsubo.github.io/blog/2019-05-02/o-que-e-timestamp

        // o trecho abaixo foi feito com auxílio da AI Codex e comentado por
        // victorhmsdev
        // 1. Define o cabeçalho do padrão JWT (Algoritmo HMAC-SHA256 e tipo do arquivo)
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

        // 2. Monta o corpo (payload) com as informações do usuário (sub = ID, iat =
        // criado em, exp = expira em)
        String payload = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"nome\":\"%s\",\"iat\":%d,\"exp\":%d}",
                escape(usuario.getId()),
                escape(usuario.getEmail()),
                escape(usuario.getNome()),
                now / 1000, // Converte milissegundos para segundos (padrão JWT)
                exp / 1000 // Converte milissegundos para segundos (padrão JWT)
        );

        // 3. Transforma o cabeçalho e o corpo em texto Base64 sem preenchimento (=)
        // separados por um ponto
        String unsignedToken = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(payload.getBytes(StandardCharsets.UTF_8));

        // 4. Junta tudo: [Header codificado].[Payload codificado].[Assinatura digital]
        return unsignedToken + "." + assinar(unsignedToken);
    }

    /**
     * Verifica se um token é legítimo, se foi alterado ou se já expirou.
     */
    public boolean tokenValido(String token) {
        try {
            // Divide o token recebido nos 3 pedaços separados por ponto
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return false; // Se não tiver 3 partes, é um formato inválido
            }

            // Recria a assinatura original usando o Header e o Payload enviados
            String assinaturaEsperada = assinar(partes[0] + "." + partes[1]);

            // Compara de forma segura a assinatura recriada com a assinatura que veio no
            // token (partes[2])
            if (!constantTimeEquals(assinaturaEsperada, partes[2])) {
                return false; // Se forem diferentes, o token foi adulterado!
            }

            // Extrai a data de expiração (exp) do token e valida contra o horário atual
            Long exp = extrairLong(token, "exp");
            return exp != null && exp > Instant.now().getEpochSecond(); // Retorna true se ainda for válido
        } catch (Exception ex) {
            return false; // Qualquer erro ou falha invalida o token automaticamente
        }
    }

    /**
     * Atalho para extrair o ID do usuário (campo 'sub' do payload).
     */
    public String extrairIdUsuario(String token) {
        return extrairString(token, "sub");
    }

    /**
     * Aplica criptografia HmacSHA256 usando a palavra secreta para criar a
     * assinatura.
     */
    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel assinar o token", ex);
        }
    }

    /**
     * Captura um texto de dentro do JSON do Payload sem usar leitor de JSON externo
     * (via manipulação de string).
     */
    private String extrairString(String token, String campo) {
        String payload = payload(token);
        String marcador = "\"" + campo + "\":\""; // Busca por exemplo: "sub":"
        int inicio = payload.indexOf(marcador);
        if (inicio < 0) {
            return null;
        }
        inicio += marcador.length();
        int fim = payload.indexOf("\"", inicio); // Acha onde fecham as aspas do valor
        return fim < 0 ? null : payload.substring(inicio, fim); // Corta o pedaço exato do texto
    }

    /**
     * Captura um número de dentro do JSON do Payload de forma puramente manual.
     */
    private Long extrairLong(String token, String campo) {
        String payload = payload(token);
        String marcador = "\"" + campo + "\":"; // Busca por exemplo: "exp":
        int inicio = payload.indexOf(marcador);
        if (inicio < 0) {
            return null;
        }
        inicio += marcador.length();
        int fim = inicio;
        // Percorre os caracteres enquanto eles forem números para descobrir o fim do
        // valor numérico
        while (fim < payload.length() && Character.isDigit(payload.charAt(fim))) {
            fim++;
        }
        return Long.parseLong(payload.substring(inicio, fim));
    }

    /**
     * Pega a segunda parte do token (índice 1) e decodifica do Base64Url para ler o
     * JSON original.
     */
    private String payload(String token) {
        String[] partes = token.split("\\.");
        return new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
    }

    /**
     * Converte bytes em uma String Base64 segura para URLs (sem caracteres
     * especiais como '+' ou '/').
     */
    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Escapa aspas e barras invertidas caso o nome ou email do usuário possua esses
     * caracteres,
     * evitando quebrar a estrutura manual do JSON.
     */
    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Compara duas assinaturas caractere por caractere levando o mesmo tempo
     * independente de onde está o erro.
     * Isso impede ataques hacker do tipo 'Timing Attack'.
     */
    private boolean constantTimeEquals(String a, String b) {

        return MessageDigestTiming.equals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
