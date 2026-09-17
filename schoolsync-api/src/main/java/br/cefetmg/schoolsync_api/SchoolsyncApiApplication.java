package br.cefetmg.schoolsync_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import br.cefetmg.schoolsync_api.config.CloudinaryProperties;

@SpringBootApplication
@EnableConfigurationProperties(CloudinaryProperties.class)
public class SchoolsyncApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchoolsyncApiApplication.class, args);
	}

}
