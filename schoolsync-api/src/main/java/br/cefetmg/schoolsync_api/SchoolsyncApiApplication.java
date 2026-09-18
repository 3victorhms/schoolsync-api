package br.cefetmg.schoolsync_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import br.cefetmg.schoolsync_api.config.CloudinaryProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CloudinaryProperties.class)
public class SchoolsyncApiApplication {

	public static void main(String[] args) {
		String dbUser = System.getenv("DATABASE_USER");
		String dbPass = System.getenv("DATABASE_PASSWORD");
		String dbUrl = System.getenv("DATABASE_URL");
		System.out.println("[DIAG] DATABASE_USER present=" + (dbUser != null) + " length=" + (dbUser == null ? -1 : dbUser.length()) + " trimmedLength=" + (dbUser == null ? -1 : dbUser.trim().length()));
		System.out.println("[DIAG] DATABASE_PASSWORD present=" + (dbPass != null) + " length=" + (dbPass == null ? -1 : dbPass.length()) + " trimmedLength=" + (dbPass == null ? -1 : dbPass.trim().length()) + " sha256=" + sha256Hex(dbPass));
		System.out.println("[DIAG] DATABASE_URL=" + dbUrl);

		SpringApplication.run(SchoolsyncApiApplication.class, args);
	}

	private static String sha256Hex(String value) {
		if (value == null) {
			return "null";
		}
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			return "error";
		}
	}

}
