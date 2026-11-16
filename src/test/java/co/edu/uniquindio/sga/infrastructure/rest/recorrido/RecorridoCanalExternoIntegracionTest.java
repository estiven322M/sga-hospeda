package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.JsonPath;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Canal externo por HTTP (L-22): una reserva EXTERNO con su identificador, el mismo mensaje recibido dos veces (RN-19), y
 * una reserva en conflicto que se rechaza SIN tocar la vigente y deja constancia (RN-18), con su contenido en
 * {@code GET /api/reservas-externas/rechazos}. Reloj fijo: la constancia lleva el momento del rechazo.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridoexterno",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoCanalExternoIntegracionTest extends RecorridoBase {

    private static final String APT = "APT-102"; // tarifa BASE 110.000, MEDIA 137.500 (el 5 y 6 de abril son MEDIA y el 7 es BASE: 385.000)
    private static final String TITULAR_1 = persona("CC-9601", "Paula Nieto", "1985-04-04");
    private static final String TITULAR_2 = persona("CC-9602", "Mario Rojas", "1983-08-08");
    private static final String MENSAJE_SOLAPE = "El apartamento ya tiene una reserva activa que solapa esas noches";

    @Test
    void recorridoDelCanalExternoDeLaRecepcionALaConstanciaDeRechazo() throws Exception {
        String admin = admin();

        // 1. el canal envía su reserva con su identificador: 201 y el identificador queda en la reserva
        String vigente = cuerpo(crear("/api/reservas", admin, reservaExterna(APT, "2027-04-05", "2027-04-08", "BKG-1001", TITULAR_1))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.canalOrigen").value("EXTERNO"))
                .andExpect(jsonPath("$.identificadorExterno").value("BKG-1001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.valorTotal").value(385_000)));
        String codigoVigente = texto(vigente, "$.codigo");

        // 2. RN-19: el MISMO mensaje recibido otra vez no crea otra reserva; el 409 nombra la que ya existe
        es409Con(crear("/api/reservas", admin, reservaExterna(APT, "2027-04-05", "2027-04-08", "BKG-1001", TITULAR_1)),
                "El mensaje ya fue recibido: el canal EXTERNO con el identificador BKG-1001 ya creó la reserva " + codigoVigente);
        consultar("/api/reservas?canal=EXTERNO&apartamento=" + APT, admin)
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].codigo").value(codigoVigente));

        // 3. RN-19: el canal EXTERNO exige identificador, y los demás canales no lo llevan
        es409(crear("/api/reservas", admin, reservaExterna(APT, "2027-06-05", "2027-06-08", null, TITULAR_1)),
                "Una reserva del canal EXTERNO debe traer su identificador externo");
        es409(crear("/api/reservas", recepcion(), "{\"identificacionApartamento\":\"" + APT + "\",\"fechaEntrada\":\"2027-06-05\","
                        + "\"fechaSalida\":\"2027-06-08\",\"canalOrigen\":\"DIRECTO\",\"identificadorExterno\":\"BKG-9\","
                        + "\"titular\":" + TITULAR_1 + ",\"ocupantes\":[" + TITULAR_1 + "]}"),
                "Una reserva del canal DIRECTO no lleva identificador externo");

        // 4. RN-18: otra reserva externa sobre las mismas noches (6 al 9 de abril) se rechaza con 409 de RN-01
        es409(crear("/api/reservas", admin, reservaExterna(APT, "2027-04-06", "2027-04-09", "BKG-1002", TITULAR_2)),
                MENSAJE_SOLAPE);

        // 5. la reserva vigente NO se tocó: mismas fechas, mismo valor, mismo estado, mismo identificador
        consultar("/api/reservas/" + codigoVigente, admin)
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.fechaEntrada").value("2027-04-05"))
                .andExpect(jsonPath("$.fechaSalida").value("2027-04-08"))
                .andExpect(jsonPath("$.identificadorExterno").value("BKG-1001"))
                .andExpect(jsonPath("$.valorTotal").value(385_000))
                .andExpect(jsonPath("$.titular.documento").value("CC-9601"));
        // y la rechazada no quedó como reserva: sigue habiendo una sola de ese canal en el apartamento
        consultar("/api/reservas?canal=EXTERNO&apartamento=" + APT, admin).andExpect(jsonPath("$.totalElementos").value(1));

        // 6. la constancia del rechazo: la más reciente primero, con todo su contenido
        String rechazos = cuerpo(consultar("/api/reservas-externas/rechazos", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.contenido[0].canalOrigen").value("EXTERNO"))
                .andExpect(jsonPath("$.contenido[0].identificadorExterno").value("BKG-1002"))
                .andExpect(jsonPath("$.contenido[0].identificacionApartamento").value(APT))
                .andExpect(jsonPath("$.contenido[0].fechaEntrada").value("2027-04-06"))
                .andExpect(jsonPath("$.contenido[0].fechaSalida").value("2027-04-09"))
                .andExpect(jsonPath("$.contenido[0].motivo").value(MENSAJE_SOLAPE))
                .andExpect(jsonPath("$.contenido[0].rechazadaEn").value("2027-03-10T10:00:00")));
        // el rechazo sembrado (RN-18, EXT-CONFLICTO-001) sigue ahí, más abajo; el mensaje repetido de BKG-1001 no es un rechazo
        List<String> identificadores = JsonPath.read(rechazos, "$.contenido[*].identificadorExterno");
        assertEquals("BKG-1002", identificadores.get(0));
        assertEquals(1, identificadores.stream().filter("BKG-1002"::equals).count());

        // 7. RN-19 también cuenta un conflicto SIN identificador repetido: un mensaje nuevo en conflicto se rechaza igual
        es409(crear("/api/reservas", admin, reservaExterna(APT, "2027-04-07", "2027-04-10", "BKG-1003", TITULAR_2)),
                MENSAJE_SOLAPE);
        consultar("/api/reservas-externas/rechazos", admin)
                .andExpect(jsonPath("$.contenido[0].identificadorExterno").value("BKG-1003"))
                .andExpect(jsonPath("$.contenido[*].identificadorExterno", hasItem("BKG-1002")));
    }

    @Test
    void soloElAdministradorCreaReservasExternasYConsultaLosRechazos() throws Exception {
        // recepción no registra reservas por el canal EXTERNO ni consulta los rechazos: 403 con ErrorResponse
        crear("/api/reservas", recepcion(), reservaExterna("APT-201", "2027-07-05", "2027-07-08", "BKG-7001", TITULAR_1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.trace").doesNotExist());
        consultar("/api/reservas-externas/rechazos", recepcion())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        consultar("/api/reservas-externas/rechazos", huespedNuevo()[0]).andExpect(status().isForbidden());
        // y lo que recepción intentó no se creó
        consultar("/api/reservas?canal=EXTERNO&apartamento=APT-201", admin())
                .andExpect(jsonPath("$.contenido[*].identificadorExterno", not(hasItem("BKG-7001"))));
    }
}
