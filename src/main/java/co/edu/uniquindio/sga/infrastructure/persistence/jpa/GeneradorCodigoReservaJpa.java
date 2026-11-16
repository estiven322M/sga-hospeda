package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.GeneradorCodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Lleva un consecutivo persistente por año. Es el único adaptador con transacción propia: incrementar el contador y
 * leer el resultado debe ocurrir como una sola unidad. El incremento es atómico (un UPDATE que suma sobre el valor
 * confirmado y deja la fila bloqueada hasta el fin de la transacción), así dos peticiones simultáneas nunca obtienen
 * el mismo consecutivo.
 */
@Repository
public class GeneradorCodigoReservaJpa implements GeneradorCodigoReserva {

    private static final Logger REGISTRO = LoggerFactory.getLogger(GeneradorCodigoReservaJpa.class);
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
        if (contadores.avanzar(anio) != 1) {
            throw new IllegalStateException("No existe el contador de códigos del año " + anio);
        }
        int consecutivo = contadores.ultimo(anio);
        return new CodigoReserva(String.format(FORMATO, anio, consecutivo));
    }

    private void asegurarContador(int anio) {
        try {
            inicializador.asegurarExistencia(anio);
        } catch (DataIntegrityViolationException otroHiloLoCreoPrimero) {
            // La fila ya existe: es justo lo que se buscaba, se continúa con el bloqueo.
            REGISTRO.debug("Otro hilo creó primero el contador de códigos del año {}", anio, otroHiloLoCreoPrimero);
        }
    }
}
