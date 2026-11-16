package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.CancelarReservaExternaRequest;
import co.edu.uniquindio.sga.application.dto.request.ConsultarDisponibilidadRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearReservaExternaRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoDisponibleResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaExternaResponse;
import co.edu.uniquindio.sga.application.usecase.CancelarReservaExternaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarDisponibilidadCanalUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaExternaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearReservaExternaUseCase;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.CanalExternoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import co.edu.uniquindio.sga.infrastructure.security.CredencialCanalFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * API del canal externo (10.3.1): las operaciones que el SGA expone a un canal, autenticadas con la credencial del canal en el
 * encabezado {@code X-Canal-Credencial} (cadena de seguridad propia, sin JWT; ver SecurityConfig). El canal sale de la credencial: el
 * controller lo recibe ya resuelto y nunca lo toma del cuerpo. Delgado: valida la forma, convierte y delega.
 */
@RestController
@RequestMapping("/api/canal")
@Tag(name = "API del canal externo", description = "Operaciones para un canal externo, autenticadas con X-Canal-Credencial")
public class CanalExternoController {

    private static final String SOLO_CANAL = "hasAuthority('CANAL_EXTERNO')";

    private final ConsultarDisponibilidadCanalUseCase disponibilidad;
    private final CrearReservaExternaUseCase crearReserva;
    private final CancelarReservaExternaUseCase cancelarReserva;
    private final ConsultarReservaExternaUseCase consultarReserva;
    private final ReservaMapper reservaMapper;
    private final CanalExternoMapper mapper;
    private final SolicitudMapper solicitud;
    private final Clock reloj;

    public CanalExternoController(ConsultarDisponibilidadCanalUseCase disponibilidad, CrearReservaExternaUseCase crearReserva,
                                  CancelarReservaExternaUseCase cancelarReserva, ConsultarReservaExternaUseCase consultarReserva,
                                  ReservaMapper reservaMapper, CanalExternoMapper mapper, SolicitudMapper solicitud, Clock reloj) {
        this.disponibilidad = disponibilidad;
        this.crearReserva = crearReserva;
        this.cancelarReserva = cancelarReserva;
        this.consultarReserva = consultarReserva;
        this.reservaMapper = reservaMapper;
        this.mapper = mapper;
        this.solicitud = solicitud;
        this.reloj = reloj;
    }

    /** Operación 1: disponibilidad de un rango de fechas para un grupo. Consulta (200). */
    @PostMapping("/disponibilidad")
    @PreAuthorize(SOLO_CANAL)
    @Operation(summary = "Consultar la disponibilidad de un rango de fechas para un grupo (canal externo)")
    public ResponseEntity<PaginaResponse<ApartamentoDisponibleResponse>> disponibilidad(
            @RequestAttribute(CredencialCanalFilter.ATRIBUTO_CANAL) CanalVenta canal,
            @Valid @RequestBody ConsultarDisponibilidadRequest request,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        var resultado = disponibilidad.ejecutar(canal,
                solicitud.aEstancia(request.fechaEntrada(), request.fechaSalida()),
                solicitud.aOcupantes(request.ocupantes(), LocalDate.now(reloj)),
                request.capacidadMinima(), request.caracteristicas(), new SolicitudPagina(pagina));
        return ResponseEntity.ok(Paginas.aResponse(resultado, reservaMapper::aDisponible));
    }

    /**
     * Operación 2: crear una reserva con el identificador propio del canal. 201 con Location. RN-19: el mismo mensaje dos veces no
     * duplica (409). RN-18: si choca con una reserva vigente se rechaza (409) y queda como conflicto para el administrador.
     */
    @PostMapping("/reservas")
    @PreAuthorize(SOLO_CANAL)
    @Operation(summary = "Crear una reserva externa con el identificador del canal (RN-18 y RN-19)")
    public ResponseEntity<ReservaExternaResponse> crear(@RequestAttribute(CredencialCanalFilter.ATRIBUTO_CANAL) CanalVenta canal,
                                                        @Valid @RequestBody CrearReservaExternaRequest request) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        var reserva = crearReserva.ejecutar(canal, request.identificadorExterno(),
                solicitud.aApartamento(request.identificacionApartamento()),
                solicitud.aEstancia(request.fechaEntrada(), request.fechaSalida()),
                solicitud.aOcupante(request.titular(), hoy), solicitud.aOcupantes(request.ocupantes(), hoy),
                request.horaEstimadaLlegada(), request.correoTitular(), ahora);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{identificador}")
                .buildAndExpand(request.identificadorExterno().trim()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aReservaExterna(canal, reserva));
    }

    /** Consulta de una reserva propia por su identificador. Solo ve las suyas (404 si es de otro canal o no existe). */
    @GetMapping("/reservas/{identificadorExterno}")
    @PreAuthorize(SOLO_CANAL)
    @Operation(summary = "Consultar una reserva propia por su identificador externo")
    public ResponseEntity<ReservaExternaResponse> consultar(@RequestAttribute(CredencialCanalFilter.ATRIBUTO_CANAL) CanalVenta canal,
                                                            @PathVariable String identificadorExterno) {
        return ResponseEntity.ok(mapper.aReservaExterna(canal, consultarReserva.ejecutar(canal, identificadorExterno)));
    }

    /** Operación 3: cancelar una reserva propia. Operación de negocio (200); idempotente; 404 si no es suya. */
    @PutMapping("/reservas/{identificadorExterno}/cancelar")
    @PreAuthorize(SOLO_CANAL)
    @Operation(summary = "Cancelar una reserva externa creada por el canal")
    public ResponseEntity<ReservaExternaResponse> cancelar(@RequestAttribute(CredencialCanalFilter.ATRIBUTO_CANAL) CanalVenta canal,
                                                           @PathVariable String identificadorExterno,
                                                           @Valid @RequestBody(required = false) CancelarReservaExternaRequest request) {
        var cancelada = cancelarReserva.ejecutar(canal, identificadorExterno, request == null ? null : request.motivo(),
                LocalDateTime.now(reloj));
        return ResponseEntity.ok(mapper.aReservaExterna(canal, cancelada));
    }
}
