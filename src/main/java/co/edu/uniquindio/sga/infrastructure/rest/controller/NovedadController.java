package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.RegistrarNovedadRequest;
import co.edu.uniquindio.sga.application.dto.response.NovedadResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.ListarNovedadesUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarNovedadUseCase;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.NovedadMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import co.edu.uniquindio.sga.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Adaptador REST de las novedades de un apartamento (7.6), anidadas bajo su apartamento. Una novedad es una constancia: no hay
 * PUT ni DELETE. El autor NO viaja en el cuerpo: es el nombre de quien inició sesión (claim del JWT).
 */
@RestController
@RequestMapping("/api/apartamentos/{identificacion}/novedades")
@Tag(name = "Novedades", description = "Novedades que el personal reporta sobre un apartamento y su historial")
public class NovedadController {

    private final RegistrarNovedadUseCase registrarNovedad;
    private final ListarNovedadesUseCase listarNovedades;
    private final NovedadMapper mapper;
    private final SolicitudMapper solicitud;

    public NovedadController(RegistrarNovedadUseCase registrarNovedad, ListarNovedadesUseCase listarNovedades,
                             NovedadMapper mapper, SolicitudMapper solicitud) {
        this.registrarNovedad = registrarNovedad;
        this.listarNovedades = listarNovedades;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    /** 201; la novedad no tiene identificador propio, así que Location apunta al historial del apartamento. */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PERSONAL_SERVICIO', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar una novedad sobre un apartamento (el autor sale del token)")
    public ResponseEntity<NovedadResponse> registrar(@PathVariable String identificacion,
                                                     @Valid @RequestBody RegistrarNovedadRequest request,
                                                     Authentication authentication) {
        String autor = authentication instanceof JwtAuthenticationToken jwt
                && jwt.getToken().getClaimAsString(JwtTokenProvider.CLAIM_NOMBRE) != null
                ? jwt.getToken().getClaimAsString(JwtTokenProvider.CLAIM_NOMBRE) : authentication.getName();
        Novedad novedad = registrarNovedad.ejecutar(solicitud.aApartamento(identificacion), autor,
                request.descripcion(), request.gravedad());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aResponse(novedad));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERSONAL_SERVICIO', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Historial de novedades de un apartamento, de 10 en 10, de la más reciente a la más antigua")
    public ResponseEntity<PaginaResponse<NovedadResponse>> listar(
            @PathVariable String identificacion,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        var resultado = listarNovedades.ejecutar(solicitud.aApartamento(identificacion), new SolicitudPagina(pagina));
        return ResponseEntity.ok(Paginas.aResponse(resultado, mapper::aResponse));
    }
}
