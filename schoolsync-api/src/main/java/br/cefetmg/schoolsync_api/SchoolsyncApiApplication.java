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
		SpringApplication.run(SchoolsyncApiApplication.class, args);
	}

}
