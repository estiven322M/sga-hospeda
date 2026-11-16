package co.edu.uniquindio.sga.infrastructure.rest.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.time.Clock;
import java.time.LocalDateTime;
import co.edu.uniquindio.sga.application.dto.request.ResolverRechazoRequest;
import co.edu.uniquindio.sga.application.usecase.ResolverRechazoReservaExternaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.RechazoReservaExternaResponse;
import co.edu.uniquindio.sga.application.usecase.ListarRechazosReservasExternasUseCase;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.RechazoReservaExternaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

/**
 * Adaptador REST de los rechazos de reservas externas (RN-18). Solo lectura: los rechazos los produce la creación
 * de reservas y no se editan ni se borran.
 */
@RestController
@RequestMapping("/api/reservas-externas/rechazos")
@Tag(name = "Canal externo", description = "Reservas que llegan por un canal externo y fueron rechazadas por conflicto")
public class RechazoReservaExternaController {

    private final ListarRechazosReservasExternasUseCase listarRechazos;
    private final ResolverRechazoReservaExternaUseCase resolver;
    private final RechazoReservaExternaMapper mapper;
    private final Clock reloj;

    public RechazoReservaExternaController(ListarRechazosReservasExternasUseCase listarRechazos,
                                           ResolverRechazoReservaExternaUseCase resolver,
                                           RechazoReservaExternaMapper mapper, Clock reloj) {
        this.listarRechazos = listarRechazos;
        this.resolver = resolver;
        this.mapper = mapper;
        this.reloj = reloj;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar las reservas externas rechazadas (RN-18), con filtro por revisión")
    public ResponseEntity<PaginaResponse<RechazoReservaExternaResponse>> listar(
            @RequestParam(required = false) Boolean revisado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(listarRechazos.ejecutar(revisado, new SolicitudPagina(pagina)),
                mapper::aResponse));
    }

    /**
     * 7.8: marca el conflicto como revisado, con una nota. Es una operación de negocio (200). Solo escribe la revisión del rechazo:
     * NUNCA toca la reserva vigente con la que chocó. 409 si ya estaba revisado; 404 si no existe.
     */
    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Resolver un conflicto del canal externo (marcarlo como revisado, con una nota)")
    public ResponseEntity<RechazoReservaExternaResponse> resolver(@PathVariable long id,
                                                                  @Valid @RequestBody ResolverRechazoRequest request,
                                                                  Authentication authentication) {
        String revisor = authentication instanceof JwtAuthenticationToken jwt && jwt.getToken().getClaimAsString("nombre") != null
                ? jwt.getToken().getClaimAsString("nombre") : authentication.getName();
        return ResponseEntity.ok(mapper.aResponse(resolver.ejecutar(id, request.nota(), revisor, LocalDateTime.now(reloj))));
    }
}
