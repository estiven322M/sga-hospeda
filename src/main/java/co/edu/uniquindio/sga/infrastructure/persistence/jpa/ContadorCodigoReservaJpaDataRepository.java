package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ContadorCodigoReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContadorCodigoReservaJpaDataRepository
        extends JpaRepository<ContadorCodigoReservaEntity, Integer> {

    /**
     * Crea la fila del año SOLO si no existe y nunca toca una existente. No se usa {@code save}: el año es un
     * {@code int} y Spring Data no considera "nueva" una entidad con identificador primitivo distinto de cero, así que
     * {@code save} hace {@code merge}, y un {@code merge} sobre una fila que otro hilo ya creó y avanzó la reescribe con
     * {@code ultimo = 0}: el contador se reinicia y se repiten códigos de reserva. Si dos hilos llegan a la vez, uno
     * viola la clave primaria (lo atiende el generador) y la fila queda intacta.
     */
    @Modifying
    @Query(value = "insert into contador_codigo_reserva (anio, ultimo) select cast(:anio as integer), 0 "
            + "where not exists (select 1 from contador_codigo_reserva where anio = :anio)", nativeQuery = true)
    int crearSiNoExiste(@Param("anio") int anio);

    /**
     * Incrementa el contador del año en UNA sola sentencia: la base de datos bloquea la fila y suma sobre el último
     * valor confirmado. Leer el valor, sumarle uno en Java y guardarlo permitiría que dos transacciones escribieran el
     * mismo número.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ContadorCodigoReservaEntity c set c.ultimo = c.ultimo + 1 where c.anio = :anio")
    int avanzar(@Param("anio") int anio);

    /** Lee el consecutivo de la transacción en curso; tras {@link #avanzar(int)} la fila sigue bloqueada por ella. */
    @Query("select c.ultimo from ContadorCodigoReservaEntity c where c.anio = :anio")
    int ultimo(@Param("anio") int anio);
}
