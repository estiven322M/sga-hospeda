package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.request.CotizarEstanciaRequest;
import co.edu.uniquindio.sga.application.dto.response.CotizacionResponse;
import co.edu.uniquindio.sga.application.usecase.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * CU-01. Cotizar no crea ningún recurso, por eso responde 200 y no 201. Se usa POST porque la composición
 * del grupo (con fechas de nacimiento) viaja en el cuerpo.
 */
@RestController
@RequestMapping("/api/cotizaciones")
@Tag(name = "Cotizaciones", description = "Valor de una estancia, noche por noche")
public class CotizacionController {

    private final CotizarEstanciaUseCase cotizarEstancia;
    private final ReservaMapper mapper;
    private final SolicitudMapper solicitud;
    private final Clock reloj;

    public CotizacionController(CotizarEstanciaUseCase cotizarEstancia, ReservaMapper mapper,
                                SolicitudMapper solicitud, Clock reloj) {
        this.cotizarEstancia = cotizarEstancia;
        this.mapper = mapper;
        this.solicitud = solicitud;
        this.reloj = reloj;
    }

    @PostMapping
    @Operation(summary = "Cotizar una estancia (CU-01)")
    public ResponseEntity<CotizacionResponse> cotizar(@Valid @RequestBody CotizarEstanciaRequest request) {
        IdentificacionApartamento apartamento = solicitud.aApartamento(request.identificacionApartamento());
        Estancia estancia = solicitud.aEstancia(request.fechaEntrada(), request.fechaSalida());
        var valor = cotizarEstancia.ejecutar(apartamento, estancia,
                solicitud.aOcupantes(request.ocupantes(), LocalDate.now(reloj)));
        return ResponseEntity.ok(mapper.aCotizacion(apartamento, estancia, valor));
    }
}
