package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.JsonPath;

import co.edu.uniquindio.sga.application.usecase.VencerReservasPendientesUseCase;
import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * RN-21: una reserva PENDIENTE que supera el plazo de confirmación (24 h) se cancela sola y libera sus noches. No tiene
 * endpoint (es un proceso programado cada 5 minutos que en la aplicación real dispara {@code VencimientoProgramado}); aquí
 * el reloj de la prueba avanza y se ejecuta el mismo caso de uso, y todo lo demás se comprueba por HTTP sobre el cuerpo.
 * Está en su propia clase porque vence TODAS las pendientes de la base: no puede compartirla con otros recorridos.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridovencimiento",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoVencimientoIntegracionTest extends RecorridoBase {

    @Autowired private VencerReservasPendientesUseCase vencerReservas;

    @Test
    void unaReservaPendienteQueSuperaElPlazoSeCancelaSolaLiberaLasNochesYDejaElAnticipoComoSaldoAFavor() throws Exception {
        String recepcion = recepcion();
        // PENDIENTE creada el 10-mar a las 10:00 en APT-201 (3 x 95.000 = 285.000), con un pago parcial de 50.000
        String codigo = texto(cuerpo(crear("/api/reservas", recepcion,
                reserva("APT-201", "2027-04-10", "2027-04-13", "DIRECTO", ADULTO_1, ADULTO_1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.valorTotal").value(285_000))), "$.codigo");
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, 50_000)).andExpect(status().isCreated());
        String disponibilidad = "{\"fechaEntrada\":\"2027-04-10\",\"fechaSalida\":\"2027-04-13\",\"ocupantes\":[" + ADULTO_1 + "]}";
        crear("/api/disponibilidad", recepcion, disponibilidad)
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='APT-201')]").isEmpty());

        // justo en el límite (24 h exactas) todavía no vence: el plazo se supera, no se alcanza
        reloj.fijarEn(LocalDateTime.of(2027, 3, 11, 10, 0));
        vencerReservas.ejecutar(reloj.ahora());
        consultar("/api/reservas/" + codigo, recepcion).andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // un minuto después ya superó el plazo: el proceso la cancela sin que nadie intervenga
        reloj.fijarEn(LocalDateTime.of(2027, 3, 11, 10, 1));
        assertTrue(vencerReservas.ejecutar(reloj.ahora()) >= 1, "debe cancelar al menos la reserva de este recorrido");
        String cancelada = cuerpo(consultar("/api/reservas/" + codigo, recepcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA")));
        List<String> observaciones = JsonPath.read(cancelada, "$.historial[?(@.accion=='VENCIMIENTO')].observacion");
        assertEquals(List.of("Cancelada por vencimiento del plazo de confirmación"), observaciones);
        List<String> autores = JsonPath.read(cancelada, "$.historial[?(@.accion=='VENCIMIENTO')].autor");
        assertEquals(List.of("SISTEMA"), autores);

        // el folio revierte el alojamiento SIN penalidad y lo pagado queda como saldo a favor
        String folio = cuerpo(consultar("/api/reservas/" + codigo + "/folio", recepcion)
                .andExpect(jsonPath("$.ajustes[0].concepto").value("Reverso del alojamiento por vencimiento de la reserva"))
                .andExpect(jsonPath("$.ajustes[0].valor").value(-285_000))
                .andExpect(jsonPath("$.saldo").value(-50_000)));
        List<Object> penalidades = JsonPath.read(folio, "$.cargos[?(@.concepto=='Penalidad por vencimiento')]");
        assertTrue(penalidades.isEmpty(), "el vencimiento no cobra penalidad");

        // RN-12: las noches quedaron libres de inmediato
        crear("/api/disponibilidad", recepcion, disponibilidad)
                .andExpect(jsonPath("$.contenido[?(@.identificacion=='APT-201')].total").value(285_000));

        // el proceso es idempotente: una segunda ejecución no vuelve a tocar nada
        assertEquals(0, vencerReservas.ejecutar(reloj.ahora()));
        consultar("/api/reservas/" + codigo + "/folio", recepcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajustes.length()").value(1));
    }
}
