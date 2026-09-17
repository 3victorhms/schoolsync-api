package br.cefetmg.schoolsync_api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

import br.cefetmg.schoolsync_api.config.CloudinaryProperties;

@Service
public class CloudinaryService {

    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    private static final String PASTA_PERFIS = "schoolsync/perfis";

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    public CloudinaryService(CloudinaryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public String enviarFotoDePerfil(String idUsuario, MultipartFile imagem) {
        if (!properties.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O envio de imagens ainda não foi configurado no servidor");
        }
        validarImagem(imagem);

        long timestamp = Instant.now().getEpochSecond();
        String publicId = "usuario-" + idUsuario;
        String parametrosAssinados = "folder=" + PASTA_PERFIS
                + "&overwrite=true"
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp;

        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("file", recursoDaImagem(imagem));
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

    private void validarImagem(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem para o perfil");
        }
        if (imagem.getSize() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve ter no máximo 5 MB");
        }
        String tipo = imagem.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(tipo)
                && !MediaType.IMAGE_PNG_VALUE.equals(tipo)
                && !"image/webp".equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Envie uma imagem JPG, PNG ou WebP");
        }
    }

    private ByteArrayResource recursoDaImagem(MultipartFile imagem) {
        try {
            return new ByteArrayResource(imagem.getBytes()) {
                @Override
                public String getFilename() {
                    return imagem.getOriginalFilename() == null ? "perfil.jpg" : imagem.getOriginalFilename();
                }
            };
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem", ex);
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
