package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.JsonPath;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Operación diaria por HTTP con el reloj movido por la prueba: llegada (RN-10, RN-11) → apartamento OCUPADO → cierre del
 * folio (RN-17) → salida → PENDIENTE_PREPARACION → preparación → disponible; no-show tras la hora límite (L-15) y el
 * tiempo de preparación entre reservas (RN-20). Cada prueba usa su propio apartamento y fechas para no depender del orden.
 *
 * RN-20 se reproduce aquí subiendo el tiempo de preparación a 6 horas (propiedad
 * {@code sga.alojamiento.tiempo-preparacion-horas}): la ventana entre la salida (11:00) y la entrada (15:00) es de 4 h,
 * así que dos reservas que se tocan el mismo día ya no caben.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridooperacion",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440",
        "sga.alojamiento.tiempo-preparacion-horas=6"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoOperacionDiariaIntegracionTest extends RecorridoBase {

    private static final String MENSAJE_RN20 = "No se respeta el tiempo de preparación configurado entre reservas";

    /** Crea una reserva DIRECTO, paga el anticipo del 30 % y la confirma. Devuelve su código. */
    private String reservarYConfirmar(String apartamento, String entrada, String salida, long total) throws Exception {
        String recepcion = recepcion();
        String codigo = texto(cuerpo(crear("/api/reservas", recepcion,
                reserva(apartamento, entrada, salida, "DIRECTO", ADULTO_1, ADULTO_1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(total))), "$.codigo");
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, total * 30 / 100))
                .andExpect(status().isCreated());
        actualizar("/api/reservas/" + codigo + "/confirmar", recepcion, confirmacion("15:30", "Recepción"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        return codigo;
    }

    @Test
    void recorridoDeLlegadaOcupacionCierreDeFolioSalidaYPreparacion() throws Exception {
        String recepcion = recepcion();
        String admin = admin();
        String servicio = servicio();
        // R1 entra HOY en APT-101 (3 noches x 120.000 = 360.000); R2 entra MAÑANA en APT-202 (3 x 100.000 = 300.000)
        String r1 = reservarYConfirmar("APT-101", "2027-03-10", "2027-03-13", 360_000);
        String r2 = reservarYConfirmar("APT-202", "2027-03-11", "2027-03-14", 300_000);

        // RN-10: no hay llegada antes de la fecha de entrada; nada cambia (ni la reserva ni el apartamento)
        es409(actualizar("/api/reservas/" + r2 + "/registrar-llegada", recepcion, autor("Recepción")),
                "No se puede registrar la llegada antes de la fecha de entrada");
        consultar("/api/reservas/" + r2, recepcion).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        consultar("/api/apartamentos/APT-202", recepcion).andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));

        // RN-11: solo un apartamento PREPARADO recibe a un grupo. Fuera de servicio -> 409 y la reserva sigue CONFIRMADA
        actualizar("/api/apartamentos/APT-101/estado-operativo/declarar-fuera-de-servicio", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("FUERA_DE_SERVICIO"));
        es409(actualizar("/api/reservas/" + r1 + "/registrar-llegada", recepcion, autor("Recepción")),
                "No se puede pasar de FUERA_DE_SERVICIO a OCUPADO");
        consultar("/api/reservas/" + r1, recepcion).andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // reincorporarlo lo deja pendiente de preparación: todavía no recibe grupos
        actualizar("/api/apartamentos/APT-101/estado-operativo/reincorporar", admin)
                .andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"));
        es409(actualizar("/api/reservas/" + r1 + "/registrar-llegada", recepcion, autor("Recepción")),
                "No se puede pasar de PENDIENTE_PREPARACION a OCUPADO");
        // en preparación tampoco
        actualizar("/api/apartamentos/APT-101/estado-operativo/iniciar-preparacion", servicio)
                .andExpect(jsonPath("$.estadoOperativo").value("EN_PREPARACION"));
        es409(actualizar("/api/reservas/" + r1 + "/registrar-llegada", recepcion, autor("Recepción")),
                "No se puede pasar de EN_PREPARACION a OCUPADO");
        // preparado: ya puede recibir
        actualizar("/api/apartamentos/APT-101/estado-operativo/marcar-preparado", servicio)
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));

        // CU-06 llegada: la reserva pasa a EN_CURSO y el apartamento a OCUPADO
        String enCurso = cuerpo(actualizar("/api/reservas/" + r1 + "/registrar-llegada", recepcion, autor("Recepción"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO")));
        List<String> acciones = JsonPath.read(enCurso, "$.historial[*].accion");
        assertTrue(acciones.contains("REGISTRO"), "el historial registra la llegada: " + acciones);
        consultar("/api/apartamentos/APT-101", recepcion).andExpect(jsonPath("$.estadoOperativo").value("OCUPADO"));
        // la vista del personal de servicio: los ocupados son el 101 (este recorrido) y el 203 (sembrado EN_CURSO)
        consultar("/api/apartamentos?estadoOperativo=OCUPADO", servicio)
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-101")))
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-203")));

        // RN-17: el folio tiene saldo (360.000 - 108.000) y no se cierra sin autorización; sin cerrarlo no hay salida
        es409(actualizar("/api/reservas/" + r1 + "/folio/cerrar", recepcion, "{}"),
                "No se puede cerrar un folio con saldo pendiente sin autorización registrada");
        es409(actualizar("/api/reservas/" + r1 + "/registrar-salida", recepcion, autor("Recepción")),
                "No se puede registrar la salida: el folio debe cerrarse primero "
                        + "(un folio con saldo pendiente exige la autorización del administrador)");
        consultar("/api/reservas/" + r1, recepcion).andExpect(jsonPath("$.estado").value("EN_CURSO"));
        consultar("/api/apartamentos/APT-101", recepcion).andExpect(jsonPath("$.estadoOperativo").value("OCUPADO"));

        // se paga el saldo, el folio queda en cero y ahora sí se cierra; un folio cerrado no admite más movimientos
        crear("/api/reservas/" + r1 + "/folio/pagos", recepcion, pago("TARJETA", HOY, 252_000))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0));
        actualizar("/api/reservas/" + r1 + "/folio/cerrar", recepcion, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado").value(true))
                .andExpect(jsonPath("$.saldo").value(0));
        es409(crear("/api/reservas/" + r1 + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, 1_000)),
                "Un folio cerrado no admite movimientos");

        // CU-07 salida: FINALIZADA y el apartamento queda PENDIENTE_PREPARACION
        actualizar("/api/reservas/" + r1 + "/registrar-salida", recepcion, autor("Recepción"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"));
        consultar("/api/apartamentos/APT-101", recepcion).andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"));
        consultar("/api/apartamentos?estadoOperativo=PENDIENTE_PREPARACION", servicio)
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-101")));

        // CU-10 preparación: el personal de servicio lo prepara y vuelve a estar listo
        actualizar("/api/apartamentos/APT-101/estado-operativo/iniciar-preparacion", servicio)
                .andExpect(jsonPath("$.estadoOperativo").value("EN_PREPARACION"));
        actualizar("/api/apartamentos/APT-101/estado-operativo/marcar-preparado", servicio)
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
        es409(actualizar("/api/apartamentos/APT-101/estado-operativo/iniciar-preparacion", servicio),
                "No se puede pasar de PREPARADO a EN_PREPARACION");
        consultar("/api/apartamentos?estadoOperativo=PREPARADO", servicio)
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-101")))
                .andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-203"))));

        // y vuelve a la búsqueda para fechas futuras
        crear("/api/disponibilidad", recepcion, "{\"fechaEntrada\":\"2027-04-20\",\"fechaSalida\":\"2027-04-23\",\"ocupantes\":["
                + ADULTO_1 + "]}")
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='APT-101')].total").value(360_000));
    }

    @Test
    void elNoShowSoloSeDeclaraDesdeLaHoraLimiteYAplicaLaPoliticaCongelada() throws Exception {
        String recepcion = recepcion();
        // entra HOY en APT-301: 3 noches x 140.000 = 420.000; anticipo del 30 % = 126.000
        String codigo = reservarYConfirmar("APT-301", "2027-03-10", "2027-03-13", 420_000);

        // L-15: hasta un minuto antes de la hora límite (22:00) del día de entrada, 409 y nada cambia
        reloj.fijarEn(LocalDateTime.of(2027, 3, 10, 12, 0));
        es409(actualizar("/api/reservas/" + codigo + "/declarar-no-show", recepcion, autor("Sistema")),
                "No se puede declarar no-show antes de la hora límite del día de entrada");
        reloj.fijarEn(LocalDateTime.of(2027, 3, 10, 21, 59));
        es409(actualizar("/api/reservas/" + codigo + "/declarar-no-show", recepcion, autor("Sistema")),
                "No se puede declarar no-show antes de la hora límite del día de entrada");
        consultar("/api/reservas/" + codigo, recepcion).andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // a las 22:00 en punto ya se puede: NO_SHOW, con la política de la versión congelada
        reloj.fijarEn(LocalDateTime.of(2027, 3, 10, 22, 0));
        String noShow = cuerpo(actualizar("/api/reservas/" + codigo + "/declarar-no-show", recepcion, autor("Sistema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_SHOW")));
        long version = ((Number) JsonPath.read(noShow, "$.versionPolitica")).longValue();
        List<String> observaciones = JsonPath.read(noShow, "$.historial[?(@.accion=='NO_SHOW')].observacion");
        assertEquals(1, observaciones.size());
        assertTrue(observaciones.get(0).contains("política aplicada: versión " + version), observaciones.get(0));

        // el folio: reverso del alojamiento y penalidad del tramo de antelación 0 (100 % de 420.000)
        String folio = cuerpo(consultar("/api/reservas/" + codigo + "/folio", recepcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajustes[0].concepto").value("Reverso del alojamiento por no-show"))
                .andExpect(jsonPath("$.ajustes[0].valor").value(-420_000)));
        List<Number> penalidades = JsonPath.read(folio, "$.cargos[?(@.concepto=='Penalidad por no-show')].valor");
        assertEquals(List.of(420_000L), penalidades.stream().map(Number::longValue).toList());
        assertEquals(294_000, ((Number) JsonPath.read(folio, "$.saldo")).longValue()); // 420.000 + 420.000 - 420.000 - 126.000

        // la reserva dejó de estar activa: no se puede declarar de nuevo y las noches se liberaron (RN-12)
        es409Con(actualizar("/api/reservas/" + codigo + "/declarar-no-show", recepcion, autor("Sistema")), "NO_SHOW");
        crear("/api/disponibilidad", recepcion, "{\"fechaEntrada\":\"2027-03-10\",\"fechaSalida\":\"2027-03-13\",\"ocupantes\":["
                + ADULTO_1 + "]}")
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='APT-301')].total").value(420_000));
    }

    @Test
    void elTiempoDePreparacionEntreReservasImpideQueDosEstanciasSeTocanElMismoDiaRN20() throws Exception {
        String recepcion = recepcion();
        // A ocupa APT-102 del 20 al 23 de marzo
        crear("/api/reservas", recepcion, reserva("APT-102", "2027-03-20", "2027-03-23", "DIRECTO", ADULTO_1, ADULTO_1))
                .andExpect(status().isCreated());

        // B empieza el día en que sale A: con 6 h de preparación no caben en la ventana de 4 h (11:00 a 15:00)
        es409(crear("/api/reservas", recepcion, reserva("APT-102", "2027-03-23", "2027-03-26", "DIRECTO", ADULTO_2, ADULTO_2)),
                MENSAJE_RN20);
        // C termina el día en que entra A: el mismo problema en el otro sentido
        es409(crear("/api/reservas", recepcion, reserva("APT-102", "2027-03-17", "2027-03-20", "DIRECTO", ADULTO_2, ADULTO_2)),
                MENSAJE_RN20);
        // la búsqueda coincide con la reserva: para el 23 de marzo el apartamento no se ofrece
        crear("/api/disponibilidad", recepcion, "{\"fechaEntrada\":\"2027-03-23\",\"fechaSalida\":\"2027-03-26\",\"ocupantes\":["
                + ADULTO_2 + "]}")
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='APT-102')]").isEmpty());

        // D empieza un día después de la salida: cabe
        crear("/api/reservas", recepcion, reserva("APT-102", "2027-03-24", "2027-03-27", "DIRECTO", ADULTO_2, ADULTO_2))
                .andExpect(status().isCreated());
        // y solo existen A y D: las dos rechazadas no dejaron nada guardado
        consultar("/api/reservas?apartamento=APT-102", recepcion)
                .andExpect(jsonPath("$.totalElementos").value(2));
    }
}
