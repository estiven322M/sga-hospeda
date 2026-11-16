package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

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

import co.edu.uniquindio.sga.application.dto.request.ActualizarServicioAdicionalRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearServicioAdicionalRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.ServicioAdicionalResponse;
import co.edu.uniquindio.sga.application.usecase.ActualizarServicioAdicionalUseCase;
import co.edu.uniquindio.sga.application.usecase.CambiarActividadServicioAdicionalUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearServicioAdicionalUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarServiciosAdicionalesUseCase;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ServicioAdicionalMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Adaptador REST de los servicios adicionales (L-17). La gestión es del administrador; recepción los consulta para poder
 * cargarlos al folio. No hay DELETE: la baja es lógica y se expresa con una operación de negocio.
 */
@RestController
@RequestMapping("/api/servicios-adicionales")
@Tag(name = "Servicios adicionales", description = "Servicios que el alojamiento ofrece y pueden generar un cargo en el folio")
public class ServicioAdicionalController {

    private final CrearServicioAdicionalUseCase crear;
    private final ActualizarServicioAdicionalUseCase actualizar;
    private final CambiarActividadServicioAdicionalUseCase cambiarActividad;
    private final ListarServiciosAdicionalesUseCase listar;
    private final ServicioAdicionalMapper mapper;

    public ServicioAdicionalController(CrearServicioAdicionalUseCase crear, ActualizarServicioAdicionalUseCase actualizar,
                                       CambiarActividadServicioAdicionalUseCase cambiarActividad,
                                       ListarServiciosAdicionalesUseCase listar, ServicioAdicionalMapper mapper) {
        this.crear = crear;
        this.actualizar = actualizar;
        this.cambiarActividad = cambiarActividad;
        this.listar = listar;
        this.mapper = mapper;
    }

    /** 201 con Location. Un servicio nuevo nace activo. */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear un servicio adicional")
    public ResponseEntity<ServicioAdicionalResponse> crear(@Valid @RequestBody CrearServicioAdicionalRequest request) {
        ServicioAdicional servicio = crear.ejecutar(request.codigo(), request.nombre(), request.descripcion(),
                request.generaCargo(), Dinero.de(request.valor()));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codigo}")
                .buildAndExpand(servicio.getCodigo()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aResponse(servicio));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Listar los servicios adicionales, de 10 en 10 (opcionalmente solo los activos)")
    public ResponseEntity<PaginaResponse<ServicioAdicionalResponse>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(listar.ejecutar(soloActivos, new SolicitudPagina(pagina)), mapper::aResponse));
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Editar un servicio adicional")
    public ResponseEntity<ServicioAdicionalResponse> actualizar(@PathVariable String codigo,
                                                               @Valid @RequestBody ActualizarServicioAdicionalRequest request) {
        return ResponseEntity.ok(mapper.aResponse(actualizar.ejecutar(codigo, request.nombre(), request.descripcion(),
                request.generaCargo(), Dinero.de(request.valor()))));
    }

    /** Baja lógica: el servicio deja de poderse cargar, no se elimina. */
    @PutMapping("/{codigo}/baja")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Dar de baja un servicio adicional (baja lógica)")
    public ResponseEntity<ServicioAdicionalResponse> darDeBaja(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aResponse(cambiarActividad.darDeBaja(codigo)));
    }

    @PutMapping("/{codigo}/activar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Volver a dar de alta un servicio adicional")
    public ResponseEntity<ServicioAdicionalResponse> activar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aResponse(cambiarActividad.activar(codigo)));
    }
}
