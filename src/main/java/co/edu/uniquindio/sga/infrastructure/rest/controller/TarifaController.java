package co.edu.uniquindio.sga.infrastructure.rest.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.request.RegistrarTarifaRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.TarifaResponse;
import co.edu.uniquindio.sga.application.usecase.ListarTarifasUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarTarifaUseCase;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.TarifarioMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/** Adaptador REST de las tarifas de un apartamento: una por temporada, identificada por su nombre. */
@RestController
@RequestMapping("/api/apartamentos/{identificacion}/tarifas")
@Tag(name = "Tarifas", description = "Valor por ocupante facturable y por noche de cada apartamento en cada temporada")
public class TarifaController {

    private final RegistrarTarifaUseCase registrarTarifa;
    private final ListarTarifasUseCase listarTarifas;
    private final TarifarioMapper mapper;
    private final SolicitudMapper solicitud;

    public TarifaController(RegistrarTarifaUseCase registrarTarifa, ListarTarifasUseCase listarTarifas,
                            TarifarioMapper mapper, SolicitudMapper solicitud) {
        this.registrarTarifa = registrarTarifa;
        this.listarTarifas = listarTarifas;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    /** Crea o reemplaza la tarifa de esa temporada (PUT idempotente): 200. */
    @PutMapping("/{temporada}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Definir o reemplazar la tarifa de un apartamento en una temporada")
    public ResponseEntity<TarifaResponse> definir(@PathVariable String identificacion,
                                                  @PathVariable String temporada,
                                                  @Valid @RequestBody RegistrarTarifaRequest request) {
        var tarifa = registrarTarifa.ejecutar(solicitud.aApartamento(identificacion), temporada,
                Dinero.de(request.valorPorOcupanteNoche()));
        return ResponseEntity.ok(mapper.aTarifa(tarifa));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Listar las tarifas de un apartamento")
    public ResponseEntity<PaginaResponse<TarifaResponse>> listar(
            @PathVariable String identificacion,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(
                listarTarifas.ejecutar(solicitud.aApartamento(identificacion), new SolicitudPagina(pagina)),
                mapper::aTarifa));
    }
}
