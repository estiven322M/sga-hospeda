package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.usecase.ListarCaracteristicasUseCase;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Catálogo cerrado de características de apartamento (L-06). Solo lectura. Devuelve la lista completa porque es un
 * vocabulario fijo (no un listado de recursos), de modo que el cliente arma sus filtros y formularios sin copiar el enum.
 */
@RestController
@RequestMapping("/api/caracteristicas")
@Tag(name = "Apartamentos", description = "Inventario de apartamentos del alojamiento")
public class CaracteristicaController {

    private final ListarCaracteristicasUseCase listarCaracteristicas;

    public CaracteristicaController(ListarCaracteristicasUseCase listarCaracteristicas) {
        this.listarCaracteristicas = listarCaracteristicas;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO')")
    @Operation(summary = "Listar el catálogo de características de apartamento")
    public ResponseEntity<List<Caracteristica>> listar() {
        return ResponseEntity.ok(listarCaracteristicas.ejecutar());
    }
}
