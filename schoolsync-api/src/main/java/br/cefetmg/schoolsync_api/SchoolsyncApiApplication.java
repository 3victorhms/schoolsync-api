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
		// O servidor (Render) roda em UTC. Sem isso, LocalDateTime.now() grava
		// horários 3h adiantados e as contas com "hoje" viram o dia às 21h.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(SchoolsyncApiApplication.class, args);
	}

}
