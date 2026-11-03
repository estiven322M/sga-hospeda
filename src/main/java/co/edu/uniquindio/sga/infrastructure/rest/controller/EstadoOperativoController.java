package co.edu.uniquindio.sga.infrastructure.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.usecase.DeclararFueraDeServicioUseCase;
import co.edu.uniquindio.sga.application.usecase.IniciarPreparacionUseCase;
import co.edu.uniquindio.sga.application.usecase.MarcarPreparadoUseCase;
import co.edu.uniquindio.sga.application.usecase.ReincorporarApartamentoUseCase;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Estado operativo de un apartamento (3.3): su condición física presente. Cada transición es una operación de
 * negocio con nombre propio; no existe un endpoint genérico de cambio de estado. El registro de la llegada y la
 * salida (OCUPADO y PENDIENTE_PREPARACION) cambian el estado desde las operaciones de la reserva.
 */
@RestController
@RequestMapping("/api/apartamentos/{identificacion}/estado-operativo")
@Tag(name = "Estado operativo", description = "Preparación y disponibilidad física de un apartamento")
public class EstadoOperativoController {

    private final IniciarPreparacionUseCase iniciarPreparacion;
    private final MarcarPreparadoUseCase marcarPreparado;
    private final DeclararFueraDeServicioUseCase declararFueraDeServicio;
    private final ReincorporarApartamentoUseCase reincorporar;
    private final ApartamentoMapper mapper;
    private final SolicitudMapper solicitud;

    public EstadoOperativoController(IniciarPreparacionUseCase iniciarPreparacion,
                                     MarcarPreparadoUseCase marcarPreparado,
                                     DeclararFueraDeServicioUseCase declararFueraDeServicio,
                                     ReincorporarApartamentoUseCase reincorporar,
                                     ApartamentoMapper mapper, SolicitudMapper solicitud) {
        this.iniciarPreparacion = iniciarPreparacion;
        this.marcarPreparado = marcarPreparado;
        this.declararFueraDeServicio = declararFueraDeServicio;
        this.reincorporar = reincorporar;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    /** CU-10: PENDIENTE_PREPARACION → EN_PREPARACION. */
    @PutMapping("/iniciar-preparacion")
    @Operation(summary = "Iniciar la preparación de un apartamento (CU-10)")
    public ResponseEntity<ApartamentoResponse> iniciarPreparacion(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                iniciarPreparacion.ejecutar(solicitud.aApartamento(identificacion))));
    }

    /** CU-10: EN_PREPARACION → PREPARADO. */
    @PutMapping("/marcar-preparado")
    @Operation(summary = "Marcar un apartamento como preparado (CU-10)")
    public ResponseEntity<ApartamentoResponse> marcarPreparado(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                marcarPreparado.ejecutar(solicitud.aApartamento(identificacion))));
    }

    /** Decisión administrativa; no equivale a un bloqueo (3.3). */
    @PutMapping("/declarar-fuera-de-servicio")
    @Operation(summary = "Declarar un apartamento fuera de servicio")
    public ResponseEntity<ApartamentoResponse> declararFueraDeServicio(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                declararFueraDeServicio.ejecutar(solicitud.aApartamento(identificacion))));
    }

    /** FUERA_DE_SERVICIO → PENDIENTE_PREPARACION. */
    @PutMapping("/reincorporar")
    @Operation(summary = "Reincorporar al servicio un apartamento fuera de servicio")
    public ResponseEntity<ApartamentoResponse> reincorporar(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                reincorporar.ejecutar(solicitud.aApartamento(identificacion))));
    }
}
