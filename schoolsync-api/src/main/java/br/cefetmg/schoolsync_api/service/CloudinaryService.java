package br.cefetmg.schoolsync_api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

import br.cefetmg.schoolsync_api.config.CloudinaryProperties;

@Service
public class CloudinaryService {

    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    private static final String PASTA_PERFIS = "schoolsync/perfis";

    // Ex.: "data:image/png;base64,iVBORw0KGgo..."
    private static final Pattern DATA_URI = Pattern.compile("^data:(image/[a-zA-Z+.-]+);base64,(.+)$", Pattern.DOTALL);

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    public CloudinaryService(CloudinaryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /**
     * Envia a foto de perfil ao Cloudinary a partir de uma Data URI em Base64
     * (ex.: "data:image/jpeg;base64,....."). Enviamos a própria string para o
     * Cloudinary (ele aceita Data URI diretamente no campo "file"), então essa
     * chamada nunca precisa montar um corpo multipart/binário.
     */
    public String enviarFotoDePerfil(String idUsuario, String imagemDataUri) {
        if (!properties.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O envio de imagens ainda não foi configurado no servidor");
        }
        validarImagem(imagemDataUri);

        long timestamp = Instant.now().getEpochSecond();
        String publicId = "usuario-" + idUsuario;
        String parametrosAssinados = "folder=" + PASTA_PERFIS
                + "&overwrite=true"
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp;

        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("file", imagemDataUri);
        corpo.add("api_key", properties.apiKey());
        corpo.add("timestamp", String.valueOf(timestamp));
        corpo.add("folder", PASTA_PERFIS);
        corpo.add("public_id", publicId);
        corpo.add("overwrite", "true");
        corpo.add("signature", sha1(parametrosAssinados + properties.apiSecret()));

        try {
            JsonNode resposta = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", properties.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);

            String urlSegura = resposta == null ? null : resposta.path("secure_url").asText();
            if (urlSegura == null || urlSegura.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "O Cloudinary não retornou a URL da imagem");
            }
            return urlSegura;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível enviar a imagem ao Cloudinary", ex);
        }
    }

    private void validarImagem(String imagemDataUri) {
        if (imagemDataUri == null || imagemDataUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem para o perfil");
        }

        Matcher matcher = DATA_URI.matcher(imagemDataUri.trim());
        if (!matcher.matches()) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Envie uma imagem JPG, PNG ou WebP");
        }

        String tipo = matcher.group(1);
        if (!MediaType.IMAGE_JPEG_VALUE.equals(tipo)
                && !MediaType.IMAGE_PNG_VALUE.equals(tipo)
                && !"image/webp".equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Envie uma imagem JPG, PNG ou WebP");
        }

        String base64 = matcher.group(2);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem", ex);
        }

        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem para o perfil");
        }
        if (bytes.length > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve ter no máximo 5 MB");
        }
    }

    private String sha1(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte caractere : hash) {
                hexadecimal.append(String.format("%02x", caractere));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 não está disponível na JVM", ex);
        }
    }
}
