package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;

/**
 * Consultar los parámetros de la sección 6 (umbral de edad, plazos, anticipo, reglas propias...). Son configuración del
 * sistema: solo se leen. Devuelve el puerto de configuración del dominio; el controlador lo presenta.
 */
@Service
public class ConsultarParametrosUseCase {

    private final ConfiguracionAlojamiento configuracion;

    public ConsultarParametrosUseCase(ConfiguracionAlojamiento configuracion) {
        this.configuracion = configuracion;
    }

    public ConfiguracionAlojamiento ejecutar() {
        return configuracion;
    }
}
