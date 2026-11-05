package co.edu.uniquindio.sga.infrastructure.config.setup;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Carga un usuario por rol al iniciar (Guía 12, 12.3), con la contraseña cifrada por el PasswordEncoder.
 * Es idempotente: si ya hay usuarios no hace nada.
 */
@Component
public class DefaultUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final DefaultUserProperties props;
    private final UsuarioJpaDataRepository repository;
    private final PasswordEncoder encoder;

    public DefaultUserInitializer(DefaultUserProperties props, UsuarioJpaDataRepository repository,
                                  PasswordEncoder encoder) {
        this.props = props;
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        props.usuarios().forEach(inicial -> repository.save(new UsuarioEntity(
                inicial.nombre(),
                inicial.correo(),
                encoder.encode(inicial.contrasena()),
                inicial.telefono(),
                inicial.documento(),
                LocalDate.parse(inicial.fechaNacimiento()),
                inicial.rol())));

        log.info("Usuarios iniciales cargados en H2: {}", repository.count());
    }
}
