package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Notificaciones al titular (10.3.2-A) por HTTP con el proveedor local (el que viene activo, sin internet ni credenciales):
 * a quién se avisa en cada caso (cuenta, correo de contacto o nadie), los tres momentos de la reserva y la bandeja
 * {@code GET /api/notificaciones} solo para el administrador, de 10 en 10 y de la más reciente a la más antigua.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridonotificaciones",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoNotificacionesIntegracionTest extends RecorridoBase {

    private static final String SIN_CUENTA_CON_CORREO = persona("CC-9701", "Visita Con Correo", "1990-01-01");
    private static final String[] APARTAMENTOS = {"APT-101", "APT-102", "APT-201", "APT-202", "APT-203"};
    private static final String SIN_CUENTA_NI_CORREO = persona("CC-9702", "Visita Sin Correo", "1990-01-01");

    /** Una reserva DIRECTO con el correo de contacto opcional del titular. */
    private static String reservaConCorreo(String apartamento, String entrada, String salida, String titular, String correo) {
        return reserva(apartamento, entrada, salida, "DIRECTO", titular, titular)
                .replace("\"canalOrigen\":\"DIRECTO\",", "\"canalOrigen\":\"DIRECTO\",\"correoTitular\":\"" + correo + "\",");
    }

    /** La primera página de la bandeja (el aviso más reciente es $.contenido[0]). */
    private String masReciente() throws Exception {
        return cuerpo(consultar("/api/notificaciones", admin()).andExpect(status().isOk()));
    }

    // ---------- los tres momentos de la reserva, avisados al correo de contacto ----------

    @Test
    void creaConfirmaYCancelaAvisandoAlCorreoDeContactoDelTitularSinCuenta() throws Exception {
        String recepcion = recepcion();
        String creada = cuerpo(crear("/api/reservas", recepcion,
                reservaConCorreo("APT-101", "2027-09-06", "2027-09-09", SIN_CUENTA_CON_CORREO, "Visita@Correo.com"))
                .andExpect(status().isCreated()));
        String codigo = texto(creada, "$.codigo");

        String creacion = masReciente();
        assertEquals("RESERVA_CREADA", texto(creacion, "$.contenido[0].tipo"));
        assertEquals("visita@correo.com", texto(creacion, "$.contenido[0].destinatario"));
        assertEquals("ENVIADA", texto(creacion, "$.contenido[0].estado"));
        assertEquals(codigo, texto(creacion, "$.contenido[0].codigoReserva"));
        assertEquals("Reserva " + codigo + " registrada", texto(creacion, "$.contenido[0].asunto"));
        assertTrue(texto(creacion, "$.contenido[0].mensaje").contains("Visita Con Correo"));
        assertTrue(texto(creacion, "$.contenido[0].mensaje").contains("2027-09-06 al 2027-09-09 (3 noches)"));

        long total = numero(creada, "$.valorTotal");
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, total)).andExpect(status().isCreated());
        actualizar("/api/reservas/" + codigo + "/confirmar", recepcion, confirmacion("16:00", "Recepción")).andExpect(status().isOk());

        String confirmacion = masReciente();
        assertEquals("RESERVA_CONFIRMADA", texto(confirmacion, "$.contenido[0].tipo"));
        assertEquals("visita@correo.com", texto(confirmacion, "$.contenido[0].destinatario"));
        assertEquals(codigo, texto(confirmacion, "$.contenido[0].codigoReserva"));
        assertEquals("ENVIADA", texto(confirmacion, "$.contenido[0].estado"));

        actualizar("/api/reservas/" + codigo + "/cancelar", recepcion, "{\"motivo\":\"Cambio de planes\",\"autor\":\"Recepción\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));

        String cancelacion = masReciente();
        assertEquals("RESERVA_CANCELADA", texto(cancelacion, "$.contenido[0].tipo"));
        assertEquals("visita@correo.com", texto(cancelacion, "$.contenido[0].destinatario"));
        assertEquals("ENVIADA", texto(cancelacion, "$.contenido[0].estado"));
    }

    @Test
    void siElTitularTieneCuentaSeUsaElCorreoDeSuCuentaAunqueSeDeUnCorreoDeContacto() throws Exception {
        String[] huesped = huespedNuevo();   // {token, documento, nombre}
        String correoDeLaCuenta = texto(cuerpo(consultar("/api/usuarios/yo", huesped[0]).andExpect(status().isOk())), "$.correo");
        String titular = persona(huesped[1], huesped[2], "1990-01-01");

        crear("/api/reservas", recepcion(),
                reservaConCorreo("APT-102", "2027-09-06", "2027-09-09", titular, "otro@correo.com"))
                .andExpect(status().isCreated());

        String aviso = masReciente();
        assertEquals("RESERVA_CREADA", texto(aviso, "$.contenido[0].tipo"));
        assertEquals(correoDeLaCuenta, texto(aviso, "$.contenido[0].destinatario"));
        assertEquals("ENVIADA", texto(aviso, "$.contenido[0].estado"));
    }

    @Test
    void sinCuentaNiCorreoDeContactoQuedaRegistradoQueNoSePudoNotificar() throws Exception {
        String creada = cuerpo(crear("/api/reservas", recepcion(),
                reserva("APT-201", "2027-09-06", "2027-09-09", "DIRECTO", SIN_CUENTA_NI_CORREO, SIN_CUENTA_NI_CORREO))
                .andExpect(status().isCreated()));   // la reserva se creó: avisar no era condición

        String aviso = masReciente();
        assertEquals("RESERVA_CREADA", texto(aviso, "$.contenido[0].tipo"));
        assertEquals("SIN_CORREO", texto(aviso, "$.contenido[0].estado"));
        assertEquals(texto(creada, "$.codigo"), texto(aviso, "$.contenido[0].codigoReserva"));
        assertEquals("No hay un correo conocido para el titular", texto(aviso, "$.contenido[0].detalle"));
        org.junit.jupiter.api.Assertions.assertNull(com.jayway.jsonpath.JsonPath.using(com.jayway.jsonpath.Configuration.builder()
                .options(com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS).build()).parse(aviso).read("$.contenido[0].destinatario"));
    }

    @Test
    void unCorreoDeContactoMalEscritoSeRechazaConUn400AntesDeCrearNada() throws Exception {
        crear("/api/reservas", recepcion(),
                reservaConCorreo("APT-202", "2027-09-06", "2027-09-09", SIN_CUENTA_CON_CORREO, "no-es-un-correo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correoTitular").exists());
    }

    @Test
    void unaReservaRechazadaPorElDominioNoDejaNingunAviso() throws Exception {
        String recepcion = recepcion();
        crear("/api/reservas", recepcion, reservaConCorreo("APT-203", "2027-09-06", "2027-09-09", SIN_CUENTA_CON_CORREO, "visita@correo.com"))
                .andExpect(status().isCreated());
        long totalAntes = numero(masReciente(), "$.totalElementos");

        es409(crear("/api/reservas", recepcion, reservaConCorreo("APT-203", "2027-09-06", "2027-09-09", SIN_CUENTA_CON_CORREO, "visita@correo.com")),
                "El apartamento ya tiene una reserva activa que solapa esas noches");

        assertEquals(totalAntes, numero(masReciente(), "$.totalElementos"));
    }

    // ---------- la bandeja ----------

    @Test
    void laBandejaPagina10EnDiezDeLaMasRecienteALaMasAntigua() throws Exception {
        String recepcion = recepcion();
        for (int i = 0; i < 12; i++) {   // 12 avisos más: cada reserva sin correo deja un SIN_CORREO
            String persona = persona("CC-96" + (10 + i), "Visita " + i, "1990-01-01");
            crear("/api/reservas", recepcion,
                    reserva(APARTAMENTOS[i % 5], "2028-0" + (1 + i / 5) + "-10", "2028-0" + (1 + i / 5) + "-13", "DIRECTO", persona, persona))
                    .andExpect(status().isCreated());
        }

        consultar("/api/notificaciones", admin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.contenido", hasSize(10)))
                .andExpect(jsonPath("$.totalElementos").exists());
        String primera = cuerpo(consultar("/api/notificaciones?pagina=0", admin()));
        String segunda = cuerpo(consultar("/api/notificaciones?pagina=1", admin()));
        assertTrue(numero(primera, "$.totalElementos") >= 12);
        assertTrue(texto(primera, "$.contenido[0].registradaEn").compareTo(texto(primera, "$.contenido[9].registradaEn")) >= 0,
                "de la más reciente a la más antigua");
        assertTrue(texto(primera, "$.contenido[9].registradaEn").compareTo(texto(segunda, "$.contenido[0].registradaEn")) >= 0);
        consultar("/api/notificaciones?pagina=-1", admin()).andExpect(status().isBadRequest());
    }

    @Test
    void laBandejaSoloLaVeElAdministrador() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/notificaciones")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        consultar("/api/notificaciones", recepcion()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        consultar("/api/notificaciones", servicio()).andExpect(status().isForbidden());
        consultar("/api/notificaciones", huespedNuevo()[0]).andExpect(status().isForbidden());
        consultar("/api/notificaciones", admin()).andExpect(status().isOk());
    }

    @Test
    void laRespuestaDeLaBandejaNoTraeTrazasNiClasesJava() throws Exception {
        String respuesta = cuerpo(consultar("/api/notificaciones", admin()).andExpect(status().isOk()));

        assertFalse(respuesta.contains("co.edu.uniquindio"));
        assertFalse(respuesta.toLowerCase().contains("exception"));
    }

    @Test
    void lasCabecerasDeLaBandejaNoCambianElFormato() throws Exception {
        consultar("/api/notificaciones", admin()).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/json")))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(org.hamcrest.Matchers.anyOf(
                        is("ENVIADA"), is("FALLIDA"), is("SIN_CORREO")))));
    }
}
