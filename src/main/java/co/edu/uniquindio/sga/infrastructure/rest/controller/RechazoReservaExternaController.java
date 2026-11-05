package co.edu.uniquindio.sga.infrastructure.rest.controller;

import org.springframework.security.access.prepost.PreAuthorize;
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
    private final RechazoReservaExternaMapper mapper;

    public RechazoReservaExternaController(ListarRechazosReservasExternasUseCase listarRechazos,
                                           RechazoReservaExternaMapper mapper) {
        this.listarRechazos = listarRechazos;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar las reservas externas rechazadas (RN-18)")
    public ResponseEntity<PaginaResponse<RechazoReservaExternaResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(listarRechazos.ejecutar(new SolicitudPagina(pagina)),
                mapper::aResponse));
    }
}
