package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.CrearCanalVentaRequest;
import co.edu.uniquindio.sga.application.dto.response.CanalVentaCreadoResponse;
import co.edu.uniquindio.sga.application.dto.response.CanalVentaResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.CambiarActividadCanalVentaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearCanalVentaUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarBitacoraCanalUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarCanalesVentaUseCase;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.CanalExternoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Gestión de los canales de venta externos y su bitácora (7.8), solo para el ADMINISTRADOR. No hay DELETE: la baja es lógica y se
 * expresa con una operación de negocio. La credencial de un canal solo aparece en la respuesta de crearlo.
 */
@RestController
@RequestMapping("/api/canales")
@Tag(name = "Canales de venta", description = "Canales externos, su credencial y la bitácora de mensajes intercambiados")
public class CanalVentaController {

    private final CrearCanalVentaUseCase crear;
    private final ListarCanalesVentaUseCase listar;
    private final CambiarActividadCanalVentaUseCase cambiarActividad;
    private final ListarBitacoraCanalUseCase bitacora;
    private final CanalExternoMapper mapper;

    public CanalVentaController(CrearCanalVentaUseCase crear, ListarCanalesVentaUseCase listar,
                                CambiarActividadCanalVentaUseCase cambiarActividad, ListarBitacoraCanalUseCase bitacora,
                                CanalExternoMapper mapper) {
        this.crear = crear;
        this.listar = listar;
        this.cambiarActividad = cambiarActividad;
        this.bitacora = bitacora;
        this.mapper = mapper;
    }

    /** 201 con Location. La respuesta trae la credencial en claro: la ÚNICA vez que se muestra. */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear un canal de venta (la credencial se muestra una sola vez)")
    public ResponseEntity<CanalVentaCreadoResponse> crear(@Valid @RequestBody CrearCanalVentaRequest request) {
        var resultado = crear.ejecutar(request.codigo(), request.nombre(), request.tipo());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codigo}")
                .buildAndExpand(resultado.canal().getCodigo()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aCreado(resultado.canal(), resultado.credencial()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar los canales de venta, de 10 en 10")
    public ResponseEntity<PaginaResponse<CanalVentaResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(listar.ejecutar(new SolicitudPagina(pagina)), mapper::aResponse));
    }

    /** Baja lógica: la credencial deja de servir (401); el historial y las reservas del canal se conservan. */
    @PutMapping("/{codigo}/desactivar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Desactivar un canal (baja lógica)")
    public ResponseEntity<CanalVentaResponse> desactivar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aResponse(cambiarActividad.desactivar(codigo)));
    }

    @PutMapping("/{codigo}/activar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Volver a activar un canal")
    public ResponseEntity<CanalVentaResponse> activar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aResponse(cambiarActividad.activar(codigo)));
    }

    /** Bitácora de los mensajes intercambiados con los canales: del más reciente al más antiguo, con filtros por canal y fechas. */
    @GetMapping("/bitacora")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Consultar la bitácora del canal externo, de 10 en 10, filtrable por canal y fechas")
    public ResponseEntity<PaginaResponse<EventoCanalResponse>> bitacora(
            @RequestParam(required = false) String canal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(bitacora.ejecutar(canal, desde, hasta, new SolicitudPagina(pagina)), mapper::aResponse));
    }
}
