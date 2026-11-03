package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ContadorCodigoReservaEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea la fila del año en su propia transacción: si dos hilos la crean a la vez, el que pierde falla sin
 * dañar la transacción de quien está generando el código.
 */
@Component
class InicializadorContadorCodigoReserva {

    private final ContadorCodigoReservaJpaDataRepository datos;

    InicializadorContadorCodigoReserva(ContadorCodigoReservaJpaDataRepository datos) {
        this.datos = datos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asegurarExistencia(int anio) {
        if (!datos.existsById(anio)) {
            datos.saveAndFlush(new ContadorCodigoReservaEntity(anio));
        }
    }
}
