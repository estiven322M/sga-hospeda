package co.edu.uniquindio.sga.infrastructure.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.response.NotificacionResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.ListarNotificacionesUseCase;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.NotificacionMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

/**
 * Bandeja de notificaciones (10.3.2-A): los avisos que el sistema intentó enviar al titular. En modo local es el buzón
 * con el que se demuestran los envíos sin internet; con el proveedor real muestra el resultado de cada envío. Solo
 * lectura y solo del administrador.
 */
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Bandeja de los avisos por correo enviados al titular")
public class NotificacionController {

    private final ListarNotificacionesUseCase listar;
    private final NotificacionMapper mapper;

    public NotificacionController(ListarNotificacionesUseCase listar, NotificacionMapper mapper) {
        this.listar = listar;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar la bandeja de notificaciones, de 10 en 10, de la más reciente a la más antigua")
    public ResponseEntity<PaginaResponse<NotificacionResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(Paginas.aResponse(listar.ejecutar(new SolicitudPagina(pagina)), mapper::aResponse));
    }
}
