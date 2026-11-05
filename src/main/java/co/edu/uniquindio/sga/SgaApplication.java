package co.edu.uniquindio.sga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import co.edu.uniquindio.sga.infrastructure.config.setup.DefaultUserProperties;

/** Clase principal del proyecto Spring Boot (Guía 03) */
@SpringBootApplication
@EnableConfigurationProperties(DefaultUserProperties.class)   // Usuarios iniciales (Guía 12)
public class SgaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SgaApplication.class, args);
	}

}
