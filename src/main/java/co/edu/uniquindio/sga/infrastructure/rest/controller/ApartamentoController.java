package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.ActualizarApartamentoRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearApartamentoRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.ActivarApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.ActualizarApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.DarDeBajaApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarApartamentosUseCase;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Adaptador REST del recurso Apartamento. No hay DELETE: la baja es lógica (regla 11) y se expresa con una
 * operación de negocio. Tampoco hay endpoint genérico de cambio de estado.
 */
@RestController
@RequestMapping("/api/apartamentos")
@Tag(name = "Apartamentos", description = "Inventario de apartamentos del alojamiento")
public class ApartamentoController {

    private final CrearApartamentoUseCase crearApartamento;
    private final ConsultarApartamentoUseCase consultarApartamento;
    private final ListarApartamentosUseCase listarApartamentos;
    private final ActualizarApartamentoUseCase actualizarApartamento;
    private final ActivarApartamentoUseCase activarApartamento;
    private final DarDeBajaApartamentoUseCase darDeBajaApartamento;
    private final ApartamentoMapper mapper;
    private final SolicitudMapper solicitud;

    public ApartamentoController(CrearApartamentoUseCase crearApartamento,
                                 ConsultarApartamentoUseCase consultarApartamento,
                                 ListarApartamentosUseCase listarApartamentos,
                                 ActualizarApartamentoUseCase actualizarApartamento,
                                 ActivarApartamentoUseCase activarApartamento,
                                 DarDeBajaApartamentoUseCase darDeBajaApartamento,
                                 ApartamentoMapper mapper, SolicitudMapper solicitud) {
        this.crearApartamento = crearApartamento;
        this.consultarApartamento = consultarApartamento;
        this.listarApartamentos = listarApartamentos;
        this.actualizarApartamento = actualizarApartamento;
        this.activarApartamento = activarApartamento;
        this.darDeBajaApartamento = darDeBajaApartamento;
        this.mapper = mapper;
        this.solicitud = solicitud;
    }

    /** 201 con Location. El apartamento nace inactivo hasta tener tarifas completas (7.4). */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear un apartamento")
    public ResponseEntity<ApartamentoResponse> crear(@Valid @RequestBody CrearApartamentoRequest request) {
        Apartamento apartamento = crearApartamento.ejecutar(solicitud.aApartamento(request.identificacion()),
                request.nombre(), request.dormitorios(), request.capacidad());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{identificacion}")
                .buildAndExpand(apartamento.getIdentificacion().valor()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aResponse(apartamento));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO')")
    @Operation(summary = "Listar apartamentos (activos o no), de 10 en 10")
    public ResponseEntity<PaginaResponse<ApartamentoResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(
                listarApartamentos.ejecutar(new SolicitudPagina(pagina)), mapper::aResponse));
    }

    @GetMapping("/{identificacion}")
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO')")
    @Operation(summary = "Consultar un apartamento")
    public ResponseEntity<ApartamentoResponse> obtener(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                consultarApartamento.ejecutar(solicitud.aApartamento(identificacion))));
    }

    @PutMapping("/{identificacion}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Editar los datos de un apartamento")
    public ResponseEntity<ApartamentoResponse> actualizar(@PathVariable String identificacion,
                                                         @Valid @RequestBody ActualizarApartamentoRequest request) {
        return ResponseEntity.ok(mapper.aResponse(actualizarApartamento.ejecutar(
                solicitud.aApartamento(identificacion), request.nombre(), request.dormitorios(),
                request.capacidad())));
    }

    /** Operación de negocio: exige tarifa en todas las temporadas (7.4). */
    @PutMapping("/{identificacion}/activar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Activar un apartamento (requiere tarifas completas)")
    public ResponseEntity<ApartamentoResponse> activar(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                activarApartamento.ejecutar(solicitud.aApartamento(identificacion))));
    }

    /** Baja lógica: el apartamento deja de venderse, no se elimina. */
    @PutMapping("/{identificacion}/baja")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Dar de baja un apartamento (baja lógica)")
    public ResponseEntity<ApartamentoResponse> darDeBaja(@PathVariable String identificacion) {
        return ResponseEntity.ok(mapper.aResponse(
                darDeBajaApartamento.ejecutar(solicitud.aApartamento(identificacion))));
    }
}
