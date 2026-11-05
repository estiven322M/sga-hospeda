package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.RegistrarBloqueoRequest;
import co.edu.uniquindio.sga.application.dto.response.BloqueoResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.ListarBloqueosUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.BloqueoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/** Adaptador REST del recurso Bloqueo, anidado bajo su apartamento. No hay DELETE: no se borra nada físicamente. */
@RestController
@RequestMapping("/api/apartamentos/{identificacion}/bloqueos")
@Tag(name = "Bloqueos", description = "Decisiones administrativas que impiden vender un apartamento en un rango de fechas")
public class BloqueoController {

    private final RegistrarBloqueoUseCase registrarBloqueo;
    private final ListarBloqueosUseCase listarBloqueos;
    private final BloqueoMapper mapper;
    private final SolicitudMapper solicitud;

    public BloqueoController(RegistrarBloqueoUseCase registrarBloqueo, ListarBloqueosUseCase listarBloqueos,
                             BloqueoMapper mapper, SolicitudMapper solicitud) {
        this.registrarBloqueo = registrarBloqueo;
        this.listarBloqueos = listarBloqueos;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    /** CU-09. 201; el bloqueo no tiene identificador propio, así que Location apunta al listado del apartamento. */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Registrar un bloqueo (CU-09)")
    public ResponseEntity<BloqueoResponse> registrar(@PathVariable String identificacion,
                                                     @Valid @RequestBody RegistrarBloqueoRequest request) {
        Bloqueo bloqueo = registrarBloqueo.ejecutar(
                solicitud.aApartamento(identificacion),
                solicitud.aEstancia(request.fechaInicio(), request.fechaFin()),
                request.motivo());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aResponse(bloqueo));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Listar los bloqueos de un apartamento")
    public ResponseEntity<PaginaResponse<BloqueoResponse>> listar(
            @PathVariable String identificacion,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        var resultado = listarBloqueos.ejecutar(solicitud.aApartamento(identificacion), new SolicitudPagina(pagina));
        return ResponseEntity.ok(Paginas.aResponse(resultado, mapper::aResponse));
    }
}
