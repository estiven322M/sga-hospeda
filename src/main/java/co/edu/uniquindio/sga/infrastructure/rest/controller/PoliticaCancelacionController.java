package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.RegistrarPoliticaCancelacionRequest;
import co.edu.uniquindio.sga.application.dto.response.PoliticaCancelacionResponse;
import co.edu.uniquindio.sga.application.usecase.ConsultarPoliticaPorVersionUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarPoliticaVigenteUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarPoliticaCancelacionUseCase;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.TarifarioMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Adaptador REST de la política de cancelación (única por alojamiento y versionada). Actualizarla crea una
 * versión nueva; las anteriores se conservan porque las reservas las tienen congeladas.
 */
@RestController
@RequestMapping("/api/politicas-cancelacion")
@Tag(name = "Política de cancelación", description = "Retención por tramos de antelación, versionada")
public class PoliticaCancelacionController {

    private final RegistrarPoliticaCancelacionUseCase registrarPolitica;
    private final ConsultarPoliticaVigenteUseCase consultarVigente;
    private final ConsultarPoliticaPorVersionUseCase consultarPorVersion;
    private final TarifarioMapper mapper;
    private final SolicitudMapper solicitud;
    private final Clock reloj;

    public PoliticaCancelacionController(RegistrarPoliticaCancelacionUseCase registrarPolitica,
                                         ConsultarPoliticaVigenteUseCase consultarVigente,
                                         ConsultarPoliticaPorVersionUseCase consultarPorVersion,
                                         TarifarioMapper mapper, SolicitudMapper solicitud, Clock reloj) {
        this.registrarPolitica = registrarPolitica;
        this.consultarVigente = consultarVigente;
        this.consultarPorVersion = consultarPorVersion;
        this.mapper = mapper;
        this.solicitud = solicitud;
        this.reloj = reloj;
    }

    /** 201 con Location hacia la nueva versión. */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Definir una nueva versión de la política de cancelación")
    public ResponseEntity<PoliticaCancelacionResponse> registrar(
            @Valid @RequestBody RegistrarPoliticaCancelacionRequest request) {
        PoliticaCancelacion politica = registrarPolitica.ejecutar(request.vigenteDesde(),
                solicitud.aTramos(request.tramos()));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{version}")
                .buildAndExpand(politica.getVersion().numero()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aPolitica(politica));
    }

    /** La vigente hoy, con la fecha del servidor. */
    @GetMapping("/vigente")
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Consultar la política de cancelación vigente")
    public ResponseEntity<PoliticaCancelacionResponse> vigente() {
        return ResponseEntity.ok(mapper.aPolitica(consultarVigente.ejecutar(LocalDate.now(reloj))));
    }

    @GetMapping("/{version}")
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Consultar una versión de la política de cancelación")
    public ResponseEntity<PoliticaCancelacionResponse> porVersion(@PathVariable int version) {
        return ResponseEntity.ok(mapper.aPolitica(consultarPorVersion.ejecutar(solicitud.aVersion(version))));
    }
}
