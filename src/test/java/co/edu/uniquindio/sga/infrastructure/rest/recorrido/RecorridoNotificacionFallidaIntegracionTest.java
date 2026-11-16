package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * I-04 e I-05 con el adaptador REAL: el proveedor es {@code smtp} pero apunta a un puerto cerrado de esta misma máquina
 * (nunca a un servicio externo, I-08), así que cada envío falla de verdad. El fallo no puede impedir reservar, confirmar,
 * cancelar ni cobrar; queda como FALLIDA en la bandeja (que el administrador ve) y la recuperación de contraseña responde lo
 * mismo de siempre sin dejar el código en ningún registro.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridonotificacionfallida",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440",
        "sga.notificaciones.proveedor=smtp",
        "sga.notificaciones.tiempo-espera-ms=1500",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=1",
        "spring.mail.username=sga@prueba.local",
        "spring.mail.password=sin-clave"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoNotificacionFallidaIntegracionTest extends RecorridoBase {

    private static final String TITULAR = persona("CC-9801", "Visita Sin Servidor", "1990-01-01");

    private String bandeja() throws Exception {
        return cuerpo(consultar("/api/notificaciones", admin()).andExpect(status().isOk()));
    }

    @Test
    void siElServidorDeCorreoFallaLaReservaSeCreaSeConfirmaYSeCancelaIgual() throws Exception {
        String recepcion = recepcion();
        String cuerpo = reserva("APT-101", "2027-09-06", "2027-09-09", "DIRECTO", TITULAR, TITULAR)
                .replace("\"canalOrigen\":\"DIRECTO\",", "\"canalOrigen\":\"DIRECTO\",\"correoTitular\":\"visita@correo.com\",");

        String creada = cuerpo(crear("/api/reservas", recepcion, cuerpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE")));
        String codigo = texto(creada, "$.codigo");

        // la bandeja deja constancia del fallo, con su causa, y la reserva existe
        String aviso = bandeja();
        assertEquals("RESERVA_CREADA", texto(aviso, "$.contenido[0].tipo"));
        assertEquals("FALLIDA", texto(aviso, "$.contenido[0].estado"));
        assertEquals("visita@correo.com", texto(aviso, "$.contenido[0].destinatario"));
        assertTrue(texto(aviso, "$.contenido[0].detalle").startsWith("No se pudo enviar el correo"));
        consultar("/api/reservas/" + codigo, recepcion).andExpect(status().isOk());

        // cobrar, confirmar y cancelar siguen funcionando con el proveedor caído
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, numero(creada, "$.valorTotal")))
                .andExpect(status().isCreated());
        actualizar("/api/reservas/" + codigo + "/confirmar", recepcion, confirmacion("16:00", "Recepción"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        assertEquals("RESERVA_CONFIRMADA", texto(bandeja(), "$.contenido[0].tipo"));
        assertEquals("FALLIDA", texto(bandeja(), "$.contenido[0].estado"));
        actualizar("/api/reservas/" + codigo + "/cancelar", recepcion, "{\"motivo\":\"Cambio de planes\",\"autor\":\"Recepción\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals("RESERVA_CANCELADA", texto(bandeja(), "$.contenido[0].tipo"));
    }

    @Test
    void elFalloDelProveedorNuncaLlegaAlClienteComoErrorNiConTrazas() throws Exception {
        String respuesta = cuerpo(crear("/api/reservas", recepcion(),
                reserva("APT-102", "2027-09-06", "2027-09-09", "DIRECTO", TITULAR, TITULAR)).andExpect(status().isCreated()));

        assertFalse(respuesta.toLowerCase().contains("mail"), "la respuesta de la reserva no habla del correo: " + respuesta);
        assertFalse(respuesta.contains("co.edu.uniquindio") || respuesta.contains("javax.mail") || respuesta.contains("jakarta.mail"));
    }

    @Test
    void laRecuperacionDeContrasenaRespondeLoMismoYElCodigoNoQuedaEnLaBandeja() throws Exception {
        String respuesta = cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"huesped1@sga.local\"}")).andExpect(status().isOk()));

        assertEquals("Si el correo está registrado, enviamos un código de recuperación que vence en 15 minutos",
                texto(respuesta, "$.mensaje"));
        String aviso = bandeja();
        assertEquals("CODIGO_RECUPERACION", texto(aviso, "$.contenido[0].tipo"));
        assertEquals("FALLIDA", texto(aviso, "$.contenido[0].estado"));
        assertEquals("(contenido sensible omitido)", texto(aviso, "$.contenido[0].mensaje"));
        assertFalse(aviso.matches("(?s).*es \\d{6}\\..*"), "el código no queda en ningún registro");
    }
}
