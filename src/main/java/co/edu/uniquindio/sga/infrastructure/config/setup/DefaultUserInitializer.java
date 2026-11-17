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
 * Es idempotente por usuario: si ya existe uno con ese correo no lo toca ni lo duplica. En prod la base H2 se borra cuando Render
 * duerme el servicio, así que se recrean en cada arranque ({@code sga.usuarios-iniciales.habilitado=true}). Si a un usuario le
 * falta un dato, falla el arranque con un mensaje que dice cuál.
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
        if (!props.habilitado()) {
            log.info("Usuarios iniciales desactivados (sga.usuarios-iniciales.habilitado=false).");
            return;
        }

        int creados = 0;
        for (int i = 0; i < props.usuarios().size(); i++) {
            DefaultUserProperties.DefaultUser inicial = props.usuarios().get(i);
            validar(inicial, i);
            if (repository.existsByCorreo(inicial.correo())) {
                continue;
            }
            repository.save(new UsuarioEntity(
                    inicial.nombre(),
                    inicial.correo(),
                    encoder.encode(inicial.contrasena()),
                    inicial.telefono(),
                    inicial.documento(),
                    LocalDate.parse(inicial.fechaNacimiento()),
                    inicial.rol()));
            creados++;
        }

        log.info("Usuarios iniciales creados: {} (total en la base: {})", creados, repository.count());
    }

    private static void validar(DefaultUserProperties.DefaultUser u, int indice) {
        exigir(u.nombre(), "nombre", indice);
        exigir(u.correo(), "correo", indice);
        exigir(u.contrasena(), "contrasena", indice);
        exigir(u.documento(), "documento", indice);
        exigir(u.fechaNacimiento(), "fecha-nacimiento", indice);
        if (u.rol() == null) {
            throw new IllegalStateException(mensaje("rol", indice));
        }
    }

    private static void exigir(String valor, String dato, int indice) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(mensaje(dato, indice));
        }
        // Al enlazar listas Spring deja "${VARIABLE}" sin resolver si la variable no existe: se detecta aquí y se dice cuál falta
        int inicio = valor.indexOf("${");
        int fin = valor.indexOf('}', inicio + 2);
        if (inicio >= 0 && fin > inicio) {
            throw new IllegalStateException("Falta la variable de entorno " + valor.substring(inicio + 2, fin)
                    + " (usuarios-iniciales[" + indice + "]." + dato + "); ver docs/despliegue.md");
        }
    }

    private static String mensaje(String dato, int indice) {
        return "Usuario inicial sin '" + dato + "': defina sga.usuarios-iniciales.usuarios[" + indice + "]." + dato
                + " (en prod, la variable de entorno correspondiente; ver docs/despliegue.md)";
    }
}
