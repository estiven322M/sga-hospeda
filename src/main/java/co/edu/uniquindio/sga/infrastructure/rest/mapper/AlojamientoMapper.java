package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.AlojamientoResponse;
import co.edu.uniquindio.sga.application.dto.response.ParametrosResponse;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;

/** Dominio → respuestas del alojamiento y de sus parámetros. No calcula nada: solo presenta. */
@Component
public class AlojamientoMapper {

    static final String NOTA_PARAMETROS = "Estos parámetros son configuración del sistema (application.properties o variables "
            + "de entorno): se muestran en solo lectura y cambiarlos exige modificar la configuración y reiniciar.";

    public AlojamientoResponse aResponse(Alojamiento alojamiento, ConfiguracionAlojamiento configuracion) {
        return new AlojamientoResponse(alojamiento.getNombre(), alojamiento.getDescripcion(), alojamiento.getCiudad(),
                alojamiento.getDireccion(), alojamiento.getUbicacion().latitud(), alojamiento.getUbicacion().longitud(),
                alojamiento.getHoraEntrada(), alojamiento.getHoraSalida(), alojamiento.getNormasConvivencia(),
                alojamiento.getPoliticaMascotas(), configuracion.mediosPagoAceptados());
    }

    public ParametrosResponse aParametros(ConfiguracionAlojamiento configuracion) {
        return new ParametrosResponse(
                configuracion.umbralEdadFacturable().anios(),
                configuracion.tiempoPreparacion().horas(),
                configuracion.plazoConfirmacion().horas(),
                configuracion.horaLimiteNoShow(),
                configuracion.anticipoPorcentaje(),
                configuracion.estanciaMinimaPorTemporada(),
                configuracion.nochesDescuentoEstanciaLarga(),
                configuracion.porcentajeDescuentoEstanciaLarga(),
                configuracion.edadAdultoResponsableAnios(),
                configuracion.mediosPagoAceptados(),
                configuracion.calendarioMaximoNoches(),
                configuracion.zonaHoraria().getId(),
                NOTA_PARAMETROS);
    }
}
