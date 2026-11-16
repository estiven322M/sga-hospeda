package co.edu.uniquindio.sga.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;

/**
 * Catálogo cerrado de características (L-06): lo que un cliente puede pedir como filtro o asignar a un apartamento.
 * No pagina porque no es un listado de recursos sino un vocabulario fijo de pocos valores.
 */
@Service
public class ListarCaracteristicasUseCase {

    public List<Caracteristica> ejecutar() {
        return List.of(Caracteristica.values());
    }
}
