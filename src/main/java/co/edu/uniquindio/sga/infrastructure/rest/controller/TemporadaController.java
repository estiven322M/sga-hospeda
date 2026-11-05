package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.RegistrarTemporadaRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.TemporadaResponse;
import co.edu.uniquindio.sga.application.usecase.ListarTemporadasUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarTemporadaUseCase;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.TarifarioMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/** Adaptador REST del recurso Temporada. Sin DELETE: no se borra nada físicamente. */
@RestController
@RequestMapping("/api/temporadas")
@Tag(name = "Temporadas", description = "Calendario de temporadas con una temporada base obligatoria")
public class TemporadaController {

    private final RegistrarTemporadaUseCase registrarTemporada;
    private final ListarTemporadasUseCase listarTemporadas;
    private final TarifarioMapper mapper;
    private final SolicitudMapper solicitud;

    public TemporadaController(RegistrarTemporadaUseCase registrarTemporada, ListarTemporadasUseCase listarTemporadas,
                               TarifarioMapper mapper, SolicitudMapper solicitud) {
        this.registrarTemporada = registrarTemporada;
        this.listarTemporadas = listarTemporadas;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    /** 201 con Location hacia el listado (la temporada se identifica por su nombre, que no va en una URL propia). */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Registrar una temporada")
    public ResponseEntity<TemporadaResponse> registrar(@Valid @RequestBody RegistrarTemporadaRequest request) {
        Temporada temporada = registrarTemporada.ejecutar(solicitud.aTemporada(request));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aTemporada(temporada));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Listar las temporadas")
    public ResponseEntity<PaginaResponse<TemporadaResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(
                listarTemporadas.ejecutar(new SolicitudPagina(pagina)), mapper::aTemporada));
    }
}
