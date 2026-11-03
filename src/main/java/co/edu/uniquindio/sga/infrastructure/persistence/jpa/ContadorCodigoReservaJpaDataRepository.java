package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ContadorCodigoReservaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContadorCodigoReservaJpaDataRepository
        extends JpaRepository<ContadorCodigoReservaEntity, Integer> {

    /** Bloquea la fila hasta el fin de la transacción: dos hilos no pueden leer el mismo consecutivo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContadorCodigoReservaEntity c where c.anio = :anio")
    Optional<ContadorCodigoReservaEntity> bloquearPorAnio(@Param("anio") int anio);
}
