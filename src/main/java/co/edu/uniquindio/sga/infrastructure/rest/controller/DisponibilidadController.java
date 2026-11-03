package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.request.ConsultarDisponibilidadRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoDisponibleResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.ConsultarDisponibilidadUseCase;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/** CU-02. Es una consulta que no crea recursos (200). Se usa POST porque la composición del grupo viaja en el cuerpo. */
@RestController
@RequestMapping("/api/disponibilidad")
@Tag(name = "Disponibilidad", description = "Apartamentos libres para una estancia y un grupo")
public class DisponibilidadController {

    private final ConsultarDisponibilidadUseCase consultarDisponibilidad;
    private final ReservaMapper mapper;
    private final SolicitudMapper solicitud;
    private final Clock reloj;

    public DisponibilidadController(ConsultarDisponibilidadUseCase consultarDisponibilidad, ReservaMapper mapper,
                                    SolicitudMapper solicitud, Clock reloj) {
        this.consultarDisponibilidad = consultarDisponibilidad;
        this.mapper = mapper;
        this.solicitud = solicitud;
        this.reloj = reloj;
    }

    @PostMapping
    @Operation(summary = "Consultar apartamentos disponibles (CU-02)")
    public ResponseEntity<PaginaResponse<ApartamentoDisponibleResponse>> consultar(
            @Valid @RequestBody ConsultarDisponibilidadRequest request,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        var resultado = consultarDisponibilidad.ejecutar(
                solicitud.aEstancia(request.fechaEntrada(), request.fechaSalida()),
                solicitud.aOcupantes(request.ocupantes(), LocalDate.now(reloj)),
                request.capacidadMinima(),
                solicitud.aDinero(request.precioMinimo()),
                solicitud.aDinero(request.precioMaximo()),
                new SolicitudPagina(pagina));
        return ResponseEntity.ok(Paginas.aResponse(resultado, mapper::aDisponible));
    }
}
