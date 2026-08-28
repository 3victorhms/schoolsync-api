package br.cefetmg.schoolsync_api.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@EnableAsync
public class FirebaseConfiguration {

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(
            @Value("${firebase.project-id:}") String projectId,
            @Value("${firebase.credentials-base64:}") String credentialsBase64
    ) throws IOException {
        GoogleCredentials credentials = credentialsBase64 == null || credentialsBase64.isBlank()
                ? GoogleCredentials.getApplicationDefault()
                : GoogleCredentials.fromStream(new ByteArrayInputStream(
                        Base64.getDecoder().decode(credentialsBase64)));

        FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
        if (projectId != null && !projectId.isBlank()) {
            options.setProjectId(projectId);
        }

        FirebaseApp app = FirebaseApp.getApps().stream()
                .filter(existing -> FirebaseApp.DEFAULT_APP_NAME.equals(existing.getName()))
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(options.build()));
        return FirebaseMessaging.getInstance(app);
    }
}
