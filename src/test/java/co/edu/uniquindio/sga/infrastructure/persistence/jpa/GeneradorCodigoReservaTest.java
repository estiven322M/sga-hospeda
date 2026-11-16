package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sin transacción de prueba: cada hilo debe abrir la suya, como en producción. Cada prueba usa un año propio. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({GeneradorCodigoReservaJpa.class, InicializadorContadorCodigoReserva.class})
class GeneradorCodigoReservaTest {

    private static final int HILOS = 8;
    private static final int CODIGOS_POR_HILO = 5;

    @Autowired
    private GeneradorCodigoReservaJpa generador;
    @Autowired
    private ContadorCodigoReservaJpaDataRepository contadores;
    @Autowired
    private PlatformTransactionManager transacciones;

    @Test
    void el_primer_codigo_del_anio_es_el_consecutivo_uno_con_el_formato_exigido() {
        assertEquals(new CodigoReserva("RES-2030-00001"), generador.generar(LocalDate.of(2030, 6, 1)));
    }

    @Test
    void los_codigos_del_mismo_anio_son_consecutivos() {
        LocalDate fecha = LocalDate.of(2031, 1, 1);

        generador.generar(fecha);
        CodigoReserva segundo = generador.generar(fecha.plusMonths(5));

        assertEquals("RES-2031-00002", segundo.valor());
    }

    @Test
    void cada_anio_lleva_su_propio_consecutivo() {
        generador.generar(LocalDate.of(2032, 12, 31));
        generador.generar(LocalDate.of(2032, 12, 31));

        assertEquals("RES-2033-00001", generador.generar(LocalDate.of(2033, 1, 1)).valor());
    }

    /**
     * Regresión del defecto que repetía códigos: crear el contador de un año que ya existe lo reiniciaba a 0, porque
     * guardar una entidad con identificador primitivo es un merge que reescribe la fila. Crear no debe tocar lo que existe.
     */
    @Test
    void crear_el_contador_de_un_anio_que_ya_existe_no_reinicia_el_consecutivo() {
        LocalDate fecha = LocalDate.of(2034, 3, 1);
        generador.generar(fecha);
        generador.generar(fecha);
        generador.generar(fecha);

        int creadas = new TransactionTemplate(transacciones).execute(estado -> contadores.crearSiNoExiste(2034));

        assertEquals(0, creadas);
        assertEquals(3, contadores.findById(2034).orElseThrow().getUltimo());
        assertEquals("RES-2034-00004", generador.generar(fecha).valor());
    }

    /**
     * Estrés de la carrera de la PRIMERA vez: varios hilos llegan a la vez a un año sin contador. Cada ronda usa un año
     * nuevo y suelta a todos los hilos juntos (con un cerrojo de arranque) para que coincidan en la creación de la fila.
     */
    @Test
    void la_creacion_simultanea_del_contador_de_un_anio_nuevo_no_repite_codigos() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(HILOS);
        try {
            for (int ronda = 0; ronda < 25; ronda++) {
                LocalDate fecha = LocalDate.of(2100 + ronda, 1, 1);
                java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
                List<Future<List<String>>> resultados = new ArrayList<>();
                for (int h = 0; h < HILOS; h++) {
                    resultados.add(hilos.submit(() -> {
                        salida.await();
                        return IntStream.range(0, 3).mapToObj(i -> generador.generar(fecha).valor()).toList();
                    }));
                }
                salida.countDown();
                List<String> codigos = new ArrayList<>();
                for (Future<List<String>> resultado : resultados) {
                    codigos.addAll(obtener(resultado));
                }
                assertEquals(HILOS * 3, codigos.stream().collect(Collectors.toSet()).size(), "ronda " + ronda);
            }
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void peticiones_simultaneas_nunca_obtienen_el_mismo_codigo() throws Exception {
        LocalDate fecha = LocalDate.of(2040, 1, 1);
        ExecutorService hilos = Executors.newFixedThreadPool(HILOS);
        try {
            List<Callable<List<String>>> tareas = IntStream.range(0, HILOS)
                    .<Callable<List<String>>>mapToObj(h -> () -> IntStream.range(0, CODIGOS_POR_HILO)
                            .mapToObj(i -> generador.generar(fecha).valor()).toList())
                    .toList();
            List<String> codigos = new ArrayList<>();
            for (Future<List<String>> resultado : hilos.invokeAll(tareas)) {
                codigos.addAll(obtener(resultado));
            }

            Set<String> distintos = codigos.stream().collect(Collectors.toSet());
            assertEquals(HILOS * CODIGOS_POR_HILO, distintos.size());
            assertEquals("RES-2040-00001", codigos.stream().sorted().findFirst().orElseThrow());
            assertEquals(String.format("RES-2040-%05d", HILOS * CODIGOS_POR_HILO),
                    codigos.stream().sorted().reduce((a, b) -> b).orElseThrow());
        } finally {
            hilos.shutdownNow();
        }
    }

    private static List<String> obtener(Future<List<String>> resultado) throws InterruptedException, ExecutionException {
        return resultado.get();
    }
}
