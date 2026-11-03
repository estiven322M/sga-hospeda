package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
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

import co.edu.uniquindio.sga.application.dto.request.AutorRequest;
import co.edu.uniquindio.sga.application.dto.request.CancelarReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.ConfirmarReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.ModificarReservaRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaResumenResponse;
import co.edu.uniquindio.sga.application.usecase.CancelarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.DeclararNoShowUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarReservasUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga.application.usecase.ModificarReservaUseCase;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Adaptador REST del recurso Reserva. Delgado: valida la forma (@Valid), convierte con MapStruct,
 * delega en el caso de uso y arma la respuesta HTTP. No decide ninguna regla de negocio.
 * La fecha y hora actuales las pone el servidor (Clock con la zona del alojamiento), no el cliente.
 * No hay endpoint genérico de cambio de estado: cada transición es una operación de negocio.
 */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Ciclo de vida de la reserva")
public class ReservaController {

    private final CrearReservaUseCase crearReserva;
    private final ConsultarReservaUseCase consultarReserva;
    private final ListarReservasUseCase listarReservas;
    private final ModificarReservaUseCase modificarReserva;
    private final CancelarReservaUseCase cancelarReserva;
    private final ConfirmarReservaUseCase confirmarReserva;
    private final RegistrarLlegadaUseCase registrarLlegada;
    private final RegistrarSalidaUseCase registrarSalida;
    private final DeclararNoShowUseCase declararNoShow;
    private final ReservaMapper mapper;
    private final SolicitudMapper solicitud;
    private final Clock reloj;

    public ReservaController(CrearReservaUseCase crearReserva, ConsultarReservaUseCase consultarReserva,
                             ListarReservasUseCase listarReservas, ModificarReservaUseCase modificarReserva,
                             CancelarReservaUseCase cancelarReserva, ConfirmarReservaUseCase confirmarReserva,
                             RegistrarLlegadaUseCase registrarLlegada, RegistrarSalidaUseCase registrarSalida,
                             DeclararNoShowUseCase declararNoShow,
                             ReservaMapper mapper, SolicitudMapper solicitud, Clock reloj) {
        this.crearReserva = crearReserva;
        this.consultarReserva = consultarReserva;
        this.listarReservas = listarReservas;
        this.modificarReserva = modificarReserva;
        this.cancelarReserva = cancelarReserva;
        this.confirmarReserva = confirmarReserva;
        this.registrarLlegada = registrarLlegada;
        this.registrarSalida = registrarSalida;
        this.declararNoShow = declararNoShow;
        this.mapper = mapper;
        this.solicitud = solicitud;
        this.reloj = reloj;
    }

    /** CU-03. 201 con Location hacia la reserva creada. */
    @PostMapping
    @Operation(summary = "Crear una reserva (CU-03)")
    public ResponseEntity<ReservaDetalleResponse> crear(@Valid @RequestBody CrearReservaRequest request) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        Reserva reserva = crearReserva.ejecutar(
                solicitud.aApartamento(request.identificacionApartamento()),
                solicitud.aEstancia(request.fechaEntrada(), request.fechaSalida()),
                solicitud.aOcupante(request.titular(), hoy),
                solicitud.aOcupantes(request.ocupantes(), hoy),
                request.canalOrigen(),
                request.horaEstimadaLlegada(),
                ahora);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{codigo}").buildAndExpand(reserva.getCodigo().valor()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aDetalle(reserva));
    }

    /** Listado filtrable de 10 en 10, de la reserva más reciente a la más antigua. */
    @GetMapping
    @Operation(summary = "Listar reservas con filtros")
    public ResponseEntity<PaginaResponse<ReservaResumenResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entradaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entradaHasta,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) String apartamento,
            @RequestParam(required = false) CanalOrigen canal,
            @RequestParam(required = false) String titular,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        var resultado = listarReservas.ejecutar(
                solicitud.aFiltro(entradaDesde, entradaHasta, estado, apartamento, canal, titular),
                new SolicitudPagina(pagina));
        return ResponseEntity.ok(Paginas.aResponse(resultado, mapper::aResumen));
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Consultar una reserva por su código")
    public ResponseEntity<ReservaDetalleResponse> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aDetalle(consultarReserva.ejecutar(solicitud.aCodigo(codigo))));
    }

    /** CU-04. Operación de negocio: 200. */
    @PutMapping("/{codigo}/modificar")
    @Operation(summary = "Modificar fechas u ocupantes de una reserva (CU-04)")
    public ResponseEntity<ReservaDetalleResponse> modificar(@PathVariable String codigo,
                                                           @Valid @RequestBody ModificarReservaRequest request) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Reserva reserva = modificarReserva.ejecutar(
                solicitud.aCodigo(codigo),
                request.identificacionApartamento() == null || request.identificacionApartamento().isBlank()
                        ? null : solicitud.aApartamento(request.identificacionApartamento()),
                solicitud.aEstancia(request.fechaEntrada(), request.fechaSalida()),
                solicitud.aOcupantes(request.ocupantes(), ahora.toLocalDate()),
                request.autor(),
                ahora);
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** Confirmación (PENDIENTE → CONFIRMADA), con el anticipo cubierto. Operación de negocio: 200. */
    @PutMapping("/{codigo}/confirmar")
    @Operation(summary = "Confirmar una reserva")
    public ResponseEntity<ReservaDetalleResponse> confirmar(@PathVariable String codigo,
                                                           @Valid @RequestBody ConfirmarReservaRequest request) {
        Reserva reserva = confirmarReserva.ejecutar(
                solicitud.aCodigo(codigo), request.horaEstimadaLlegada(), request.autor(), LocalDateTime.now(reloj));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-06. CONFIRMADA → EN_CURSO y el apartamento pasa a OCUPADO. Operación de negocio: 200. */
    @PutMapping("/{codigo}/registrar-llegada")
    @Operation(summary = "Registrar la llegada del grupo (CU-06)")
    public ResponseEntity<ReservaDetalleResponse> registrarLlegada(@PathVariable String codigo,
                                                                  @Valid @RequestBody AutorRequest request) {
        Reserva reserva = registrarLlegada.ejecutar(
                solicitud.aCodigo(codigo), request.autor(), LocalDateTime.now(reloj));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-07. EN_CURSO → FINALIZADA y el apartamento pasa a PENDIENTE_PREPARACION. Exige el folio cerrado. */
    @PutMapping("/{codigo}/registrar-salida")
    @Operation(summary = "Registrar la salida del grupo (CU-07)")
    public ResponseEntity<ReservaDetalleResponse> registrarSalida(@PathVariable String codigo,
                                                                 @Valid @RequestBody AutorRequest request) {
        Reserva reserva = registrarSalida.ejecutar(
                solicitud.aCodigo(codigo), request.autor(), LocalDateTime.now(reloj));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CONFIRMADA → NO_SHOW desde la hora límite del día de entrada; aplica la política congelada. */
    @PutMapping("/{codigo}/declarar-no-show")
    @Operation(summary = "Declarar no-show")
    public ResponseEntity<ReservaDetalleResponse> declararNoShow(@PathVariable String codigo,
                                                                @Valid @RequestBody AutorRequest request) {
        Reserva reserva = declararNoShow.ejecutar(
                solicitud.aCodigo(codigo), request.autor(), LocalDateTime.now(reloj));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-05. Operación de negocio: 200. */
    @PutMapping("/{codigo}/cancelar")
    @Operation(summary = "Cancelar una reserva (CU-05)")
    public ResponseEntity<ReservaDetalleResponse> cancelar(@PathVariable String codigo,
                                                          @Valid @RequestBody CancelarReservaRequest request) {
        Reserva reserva = cancelarReserva.ejecutar(
                solicitud.aCodigo(codigo), request.motivo(), request.autor(), LocalDateTime.now(reloj));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }
}
