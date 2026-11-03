package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.CerrarFolioRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarPagoRequest;
import co.edu.uniquindio.sga.application.dto.response.FolioResponse;
import co.edu.uniquindio.sga.application.usecase.CerrarFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarPagoUseCase;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.FolioMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Adaptador REST del recurso Folio. El folio de una reserva se identifica por el código de esa reserva. */
@RestController
@RequestMapping("/api/reservas/{codigo}/folio")
@Tag(name = "Folios", description = "Cargos, ajustes, pagos y saldo de una reserva")
public class FolioController {

    private final ConsultarFolioUseCase consultarFolio;
    private final RegistrarPagoUseCase registrarPago;
    private final CerrarFolioUseCase cerrarFolio;
    private final FolioMapper mapper;
    private final SolicitudMapper solicitud;

    public FolioController(ConsultarFolioUseCase consultarFolio, RegistrarPagoUseCase registrarPago,
                           CerrarFolioUseCase cerrarFolio, FolioMapper mapper, SolicitudMapper solicitud) {
        this.consultarFolio = consultarFolio;
        this.registrarPago = registrarPago;
        this.cerrarFolio = cerrarFolio;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    @GetMapping
    @Operation(summary = "Consultar el folio de una reserva con su saldo")
    public ResponseEntity<FolioResponse> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aResponse(consultarFolio.ejecutar(solicitud.aCodigo(codigo))));
    }

    /**
     * Cierra el folio (requisito de la salida, 7.6). Con saldo pendiente es CU-11: exige quién autoriza y el motivo
     * (RN-17); sin ellos, 409. Operación de negocio: 200.
     */
    @PutMapping("/cerrar")
    @Operation(summary = "Cerrar el folio; con saldo pendiente exige autorización (CU-11)")
    public ResponseEntity<FolioResponse> cerrar(@PathVariable String codigo,
                                               @Valid @RequestBody CerrarFolioRequest request) {
        var folio = cerrarFolio.ejecutar(solicitud.aCodigo(codigo), solicitud.aAutorizacionCierre(request));
        return ResponseEntity.ok(mapper.aResponse(folio));
    }

    /** CU-08. Crea un pago (201); Location apunta al folio, que devuelve con el saldo actualizado. */
    @PostMapping("/pagos")
    @Operation(summary = "Registrar un pago en el folio (CU-08)")
    public ResponseEntity<FolioResponse> registrarPago(@PathVariable String codigo,
                                                      @Valid @RequestBody RegistrarPagoRequest request) {
        var folio = registrarPago.ejecutar(solicitud.aCodigo(codigo), solicitud.aPago(request));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservas/{codigo}/folio").buildAndExpand(codigo).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aResponse(folio));
    }
}
