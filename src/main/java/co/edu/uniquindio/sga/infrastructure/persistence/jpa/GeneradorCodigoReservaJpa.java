package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.GeneradorCodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Lleva un consecutivo persistente por año. Es el único adaptador con transacción propia: leer, incrementar y
 * bloquear el contador debe ocurrir como una sola unidad para que dos peticiones simultáneas no repitan código.
 */
@Repository
public class GeneradorCodigoReservaJpa implements GeneradorCodigoReserva {

    private static final String FORMATO = "RES-%04d-%05d";

    private final ContadorCodigoReservaJpaDataRepository contadores;
    private final InicializadorContadorCodigoReserva inicializador;

    GeneradorCodigoReservaJpa(ContadorCodigoReservaJpaDataRepository contadores,
                              InicializadorContadorCodigoReserva inicializador) {
        this.contadores = contadores;
        this.inicializador = inicializador;
    }

    @Override
    @Transactional
    public CodigoReserva generar(LocalDate fecha) {
        int anio = fecha.getYear();
        asegurarContador(anio);
        int consecutivo = contadores.bloquearPorAnio(anio)
                .orElseThrow(() -> new IllegalStateException("No existe el contador de códigos del año " + anio))
                .avanzar();
        return new CodigoReserva(String.format(FORMATO, anio, consecutivo));
    }

    private void asegurarContador(int anio) {
        try {
            inicializador.asegurarExistencia(anio);
        } catch (DataIntegrityViolationException otroHiloLoCreoPrimero) {
            // La fila ya existe: es justo lo que se buscaba, se continúa con el bloqueo.
        }
    }
}
