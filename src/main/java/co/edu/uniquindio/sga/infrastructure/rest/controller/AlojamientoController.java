package co.edu.uniquindio.sga.infrastructure.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.request.ActualizarAlojamientoRequest;
import co.edu.uniquindio.sga.application.dto.response.AlojamientoResponse;
import co.edu.uniquindio.sga.application.dto.response.ParametrosResponse;
import co.edu.uniquindio.sga.application.usecase.ActualizarAlojamientoUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarAlojamientoUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarParametrosUseCase;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.AlojamientoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Adaptador REST del alojamiento (7.3). La consulta es PÚBLICA (la usan Inicio, el detalle de un apartamento y el mapa,
 * también para quien aún no inició sesión); editar es del administrador. Los parámetros de la sección 6 se muestran en
 * solo lectura. No hay POST ni DELETE: el alojamiento se siembra desde application.properties y no se elimina.
 */
@RestController
@RequestMapping("/api/alojamiento")
@Tag(name = "Alojamiento", description = "Datos del alojamiento: identidad, ubicación, horarios, normas y mascotas")
public class AlojamientoController {

    private final ConsultarAlojamientoUseCase consultarAlojamiento;
    private final ActualizarAlojamientoUseCase actualizarAlojamiento;
    private final ConsultarParametrosUseCase consultarParametros;
    private final ConfiguracionAlojamiento configuracion;
    private final AlojamientoMapper mapper;

    public AlojamientoController(ConsultarAlojamientoUseCase consultarAlojamiento,
                                 ActualizarAlojamientoUseCase actualizarAlojamiento,
                                 ConsultarParametrosUseCase consultarParametros, ConfiguracionAlojamiento configuracion,
                                 AlojamientoMapper mapper) {
        this.consultarAlojamiento = consultarAlojamiento;
        this.actualizarAlojamiento = actualizarAlojamiento;
        this.consultarParametros = consultarParametros;
        this.configuracion = configuracion;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Consultar los datos del alojamiento (público)")
    public ResponseEntity<AlojamientoResponse> obtener() {
        return ResponseEntity.ok(mapper.aResponse(consultarAlojamiento.ejecutar(), configuracion));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Editar los datos del alojamiento")
    public ResponseEntity<AlojamientoResponse> actualizar(@Valid @RequestBody ActualizarAlojamientoRequest request) {
        var alojamiento = actualizarAlojamiento.ejecutar(request.nombre(), request.descripcion(), request.ciudad(),
                request.direccion(), new Ubicacion(request.latitud(), request.longitud()), request.horaEntrada(),
                request.horaSalida(), request.normasConvivencia(), request.politicaMascotas());
        return ResponseEntity.ok(mapper.aResponse(alojamiento, configuracion));
    }

    @GetMapping("/parametros")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Consultar los parámetros del sistema (solo lectura)")
    public ResponseEntity<ParametrosResponse> parametros() {
        return ResponseEntity.ok(mapper.aParametros(consultarParametros.ejecutar()));
    }
}
