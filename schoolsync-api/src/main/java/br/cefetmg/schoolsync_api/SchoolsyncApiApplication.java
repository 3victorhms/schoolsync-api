package br.cefetmg.schoolsync_api;

import java.util.TimeZone;

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
		// o servidor do render roda em UTC, sem isso o LocalDateTime.now() grava horário 3h adiantado
		// e o "hoje" vira às 21h
		// (comentário feito com auxílio do Claude)
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(SchoolsyncApiApplication.class, args);
	}

}
