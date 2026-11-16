package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Recorrido de una reserva de principio a fin por HTTP, verificando el CUERPO de cada respuesta (no solo el código):
 * cotizar → disponibilidad → crear → confirmar sin anticipo (409) → pagar → confirmar → modificar (ajuste y saldo)
 * → cancelar con la política CONGELADA → la disponibilidad vuelve a mostrar el apartamento (RN-12).
 * Reloj fijo en el 10-mar-2027, login real y base H2 propia.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridoreserva",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoReservaIntegracionTest extends RecorridoBase {

    private static final String APT = "APT-101"; // tarifa BASE 120.000 por ocupante facturable y noche

    private static String disponibilidad(String entrada, String salida, String titular) {
        return "{\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + salida + "\",\"ocupantes\":[" + titular + "]}";
    }

    private static String modificacion(String entrada, String salida, String titular) {
        return "{\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + salida + "\",\"autor\":\"Recepción\","
                + "\"ocupantes\":[" + titular + "]}";
    }

    /** Tarjeta de un apartamento dentro de la respuesta de disponibilidad (lista vacía si no aparece). */
    private static List<Number> totalesDe(String json, String apartamento) {
        return com.jayway.jsonpath.JsonPath.read(json, "$.contenido[?(@.identificacion=='" + apartamento + "')].total");
    }

    @Test
    void recorridoCompletoDeUnaReservaDeCotizarACancelarYLiberarElApartamento() throws Exception {
        String[] huesped = huespedNuevo();
        String token = huesped[0];
        String titular = persona(huesped[1], huesped[2], "1990-01-01");
        String recepcion = recepcion();

        // 1. CU-01 cotizar: 3 noches de BASE x 1 ocupante facturable x 120.000
        String cotizada = cuerpo(crear("/api/cotizaciones", token, cotizacion(APT, "2027-03-15", "2027-03-18", titular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacionApartamento").value(APT))
                .andExpect(jsonPath("$.noches").value(3))
                .andExpect(jsonPath("$.desglose.length()").value(3))
                .andExpect(jsonPath("$.desglose[0].temporada").value("BASE"))
                .andExpect(jsonPath("$.desglose[0].tarifaPorOcupante").value(120_000))
                .andExpect(jsonPath("$.subtotalBruto").value(360_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(0))
                .andExpect(jsonPath("$.total").value(360_000))
                .andExpect(jsonPath("$.moneda").value("COP")));
        long totalCotizado = numero(cotizada, "$.total");

        // 2. CU-02 disponibilidad: el apartamento está libre y cuesta lo mismo que la cotización
        String antes = cuerpo(crear("/api/disponibilidad", token, disponibilidad("2027-03-15", "2027-03-18", titular))
                .andExpect(status().isOk()));
        assertEquals(List.of(totalCotizado), totalesDe(antes, APT).stream().map(Number::longValue).toList());

        // 3. CU-03 crear: 201 con Location, PENDIENTE, y el valor congelado es el cotizado
        MvcResult creada = crear("/api/reservas", token, reserva(APT, "2027-03-15", "2027-03-18", "PORTAL", titular, titular))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.canalOrigen").value("PORTAL"))
                .andExpect(jsonPath("$.identificacionApartamento").value(APT))
                .andExpect(jsonPath("$.noches").value(3))
                .andExpect(jsonPath("$.titular.documento").value(huesped[1]))
                .andExpect(jsonPath("$.valorTotal").value(360_000))
                .andExpect(jsonPath("$.historial[0].accion").value("CREACION"))
                .andReturn();
        String detalle = creada.getResponse().getContentAsString();
        String codigo = texto(detalle, "$.codigo");
        long versionPolitica = numero(detalle, "$.versionPolitica");
        assertEquals(totalCotizado, numero(detalle, "$.valorTotal"), "la reserva congela el valor que se cotizó");
        assertTrue(creada.getResponse().getHeader("Location").endsWith("/api/reservas/" + codigo));
        // el folio se abre con el cargo de alojamiento y sin pagos
        consultar("/api/reservas/" + codigo + "/folio", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos[0].valor").value(360_000))
                .andExpect(jsonPath("$.pagos", empty()))
                .andExpect(jsonPath("$.saldo").value(360_000))
                .andExpect(jsonPath("$.cerrado").value(false));

        // 4. con la reserva activa, el apartamento deja de ofrecerse para esas noches (RN-01)
        crear("/api/disponibilidad", token, disponibilidad("2027-03-15", "2027-03-18", titular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='" + APT + "')]", empty()));

        // 5. confirmar SIN anticipo: 409 con la cifra que falta (30 % de 360.000 = 108.000)
        es409(actualizar("/api/reservas/" + codigo + "/confirmar", recepcion, confirmacion("16:00", "Recepción")),
                "Para confirmar la reserva debe registrarse un anticipo de 108000 COP (30 % del valor); "
                        + "hasta ahora se han pagado 0 COP");
        consultar("/api/reservas/" + codigo, token).andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // 6. CU-08 pagar el anticipo: 201, el pago queda en el folio y el saldo baja
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("TRANSFERENCIA", HOY, 108_000))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/reservas/" + codigo + "/folio")))
                .andExpect(jsonPath("$.pagos.length()").value(1))
                .andExpect(jsonPath("$.pagos[0].medio").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$.pagos[0].valor").value(108_000))
                .andExpect(jsonPath("$.saldo").value(252_000));

        // 7. confirmar con el anticipo cubierto: CONFIRMADA, con su hora de llegada y su evento en el historial
        String confirmada = cuerpo(actualizar("/api/reservas/" + codigo + "/confirmar", recepcion,
                confirmacion("16:00", "Recepción"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.horaEstimadaLlegada", startsWith("16:00"))));
        List<String> acciones = com.jayway.jsonpath.JsonPath.read(confirmada, "$.historial[*].accion");
        assertTrue(acciones.contains("CONFIRMACION"), "el historial registra la confirmación: " + acciones);

        // 8. CU-04 modificar: ampliar a 4 noches; el ajuste queda en el folio y la política congelada no cambia
        actualizar("/api/reservas/" + codigo + "/modificar", recepcion, modificacion("2027-03-15", "2027-03-19", titular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.noches").value(4))
                .andExpect(jsonPath("$.valorTotal").value(480_000))
                .andExpect(jsonPath("$.versionPolitica").value(versionPolitica));
        String folio = cuerpo(consultar("/api/reservas/" + codigo + "/folio", recepcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos[0].valor").value(360_000))   // el cargo original no se toca (RN-16)
                .andExpect(jsonPath("$.ajustes.length()").value(1))
                .andExpect(jsonPath("$.ajustes[0].valor").value(120_000))  // 480.000 - 360.000
                .andExpect(jsonPath("$.pagos[0].valor").value(108_000)));
        long cargos = ((List<Number>) com.jayway.jsonpath.JsonPath.read(folio, "$.cargos[*].valor")).stream().mapToLong(Number::longValue).sum();
        long ajustes = ((List<Number>) com.jayway.jsonpath.JsonPath.read(folio, "$.ajustes[*].valor")).stream().mapToLong(Number::longValue).sum();
        long pagos = ((List<Number>) com.jayway.jsonpath.JsonPath.read(folio, "$.pagos[*].valor")).stream().mapToLong(Number::longValue).sum();
        assertEquals(cargos + ajustes - pagos, numero(folio, "$.saldo"), "saldo = cargos + ajustes - pagos");
        assertEquals(372_000, numero(folio, "$.saldo"));

        // 9. se publica una política NUEVA que retendría el 100 % hasta con 1000 h de antelación; la reserva no la usa
        crear("/api/politicas-cancelacion", admin(),
                "{\"vigenteDesde\":\"2027-03-11\",\"tramos\":[{\"horasAntelacionMinima\":0,\"porcentajeRetencion\":100},"
                        + "{\"horasAntelacionMinima\":1000,\"porcentajeRetencion\":0}]}")
                .andExpect(status().isCreated());

        // 10. CU-05 cancelar el 13-mar a las 12:00, 51 h antes de la entrada (15-mar 15:00): rige el tramo de 24 h = 50 %
        reloj.fijarEn(LocalDateTime.of(2027, 3, 13, 12, 0));
        String cancelada = cuerpo(actualizar("/api/reservas/" + codigo + "/cancelar", token,
                "{\"motivo\":\"Cambio de planes\",\"autor\":\"" + huesped[2] + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.versionPolitica").value(versionPolitica)));
        List<String> observaciones = com.jayway.jsonpath.JsonPath.read(cancelada, "$.historial[?(@.accion=='CANCELACION')].observacion");
        assertEquals(1, observaciones.size());
        assertTrue(observaciones.get(0).contains("política aplicada: versión " + versionPolitica),
                "la cancelación usa la política congelada: " + observaciones.get(0));

        // 11. el folio: reverso del alojamiento, penalidad del 50 % de 480.000 y la devolución como saldo
        String folioFinal = cuerpo(consultar("/api/reservas/" + codigo + "/folio", recepcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajustes.length()").value(2))
                .andExpect(jsonPath("$.ajustes[1].concepto").value("Reverso del alojamiento por cancelación"))
                .andExpect(jsonPath("$.ajustes[1].valor").value(-480_000)));
        List<Number> penalidades = com.jayway.jsonpath.JsonPath.read(folioFinal,
                "$.cargos[?(@.concepto=='Penalidad por cancelación')].valor");
        assertEquals(List.of(240_000L), penalidades.stream().map(Number::longValue).toList(),
                "retención del 50 % de la política congelada (la nueva habría retenido 480.000)");
        assertNotEquals(480_000L, penalidades.get(0).longValue());
        assertEquals(132_000, numero(folioFinal, "$.saldo")); // (360.000 + 240.000) + (120.000 - 480.000) - 108.000

        // 12. cancelar otra vez: 409, la reserva ya es terminal
        es409Con(actualizar("/api/reservas/" + codigo + "/cancelar", token,
                "{\"motivo\":\"Otra vez\",\"autor\":\"" + huesped[2] + "\"}"), "CANCELADA");

        // 13. RN-12: las noches se liberaron de inmediato y el apartamento vuelve a la búsqueda, al mismo precio
        String despues = cuerpo(crear("/api/disponibilidad", token, disponibilidad("2027-03-15", "2027-03-18", titular))
                .andExpect(status().isOk()));
        assertEquals(List.of(totalCotizado), totalesDe(despues, APT).stream().map(Number::longValue).toList(),
                "el apartamento reaparece disponible con el mismo valor");
        // y las 4 noches de la reserva modificada también quedaron libres
        crear("/api/disponibilidad", token, disponibilidad("2027-03-15", "2027-03-19", titular))
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='" + APT + "')].total").value(480_000));
    }

    @Test
    void unHuespedNoVeNiCancelaLaReservaDeOtroEnElRecorrido() throws Exception {
        String[] dueno = huespedNuevo();
        String[] otro = huespedNuevo();
        String titular = persona(dueno[1], dueno[2], "1990-01-01");
        String codigo = texto(cuerpo(crear("/api/reservas", dueno[0],
                reserva("APT-201", "2027-05-10", "2027-05-13", "PORTAL", titular, titular))
                .andExpect(status().isCreated())), "$.codigo");

        // el otro huésped recibe 403 al consultar, al ver el folio y al cancelar; nada cambió
        for (var respuesta : List.of(consultar("/api/reservas/" + codigo, otro[0]),
                consultar("/api/reservas/" + codigo + "/folio", otro[0]),
                actualizar("/api/reservas/" + codigo + "/cancelar", otro[0],
                        "{\"motivo\":\"Intento ajeno\",\"autor\":\"" + otro[2] + "\"}"))) {
            respuesta.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
        consultar("/api/reservas/" + codigo, dueno[0]).andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }
}
