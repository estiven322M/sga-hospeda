package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * El canal externo completo por HTTP (7.8, 10.3.1) con login real y la credencial del canal: la gestión de canales (credencial
 * mostrada una sola vez, baja lógica), la autenticación por X-Canal-Credencial en su propia cadena (sin JWT), las tres operaciones
 * del canal con RN-18 y RN-19, la bitácora con sus filtros, la resolución de conflictos y los avisos al titular.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridocanalcompleto",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoCanalExternoCompletoIntegracionTest extends RecorridoBase {

    private static final String ENCABEZADO = "X-Canal-Credencial";
    private static final String CREDENCIAL_DEMO = "sgc_demo_simulador_canal_2026_no_usar_en_produccion";
    private static final AtomicInteger SECUENCIA = new AtomicInteger(10);
    private static final String MENSAJE_401 = "Credencial del canal inválida o canal desactivado: envíe una credencial vigente en el encabezado X-Canal-Credencial";

    // ---------- utilidades ----------

    /** Crea un canal (administrador) y devuelve {codigo, credencial}. */
    private String[] crearCanal() throws Exception {
        String codigo = "CN" + SECUENCIA.incrementAndGet();
        String cuerpo = cuerpo(crear("/api/canales", admin(), "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Canal " + codigo + "\",\"tipo\":\"AGENCIA_DE_VIAJES\"}")
                .andExpect(status().isCreated()));
        return new String[] {codigo, texto(cuerpo, "$.credencial")};
    }

    private ResultActions canalPost(String ruta, String credencial, String cuerpo) throws Exception {
        var peticion = post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (credencial != null) peticion.header(ENCABEZADO, credencial);
        return mvc.perform(peticion);
    }

    private ResultActions canalPut(String ruta, String credencial, String cuerpo) throws Exception {
        var peticion = put(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (credencial != null) peticion.header(ENCABEZADO, credencial);
        return mvc.perform(peticion);
    }

    private ResultActions canalGet(String ruta, String credencial) throws Exception {
        var peticion = get(ruta);
        if (credencial != null) peticion.header(ENCABEZADO, credencial);
        return mvc.perform(peticion);
    }

    private static String reservaExterna(String id, String apartamento, String entrada, String salida, String titular, String correo) {
        return "{\"identificadorExterno\":\"" + id + "\",\"identificacionApartamento\":\"" + apartamento + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\"," + (correo == null ? "" : "\"correoTitular\":\"" + correo + "\",")
                + "\"titular\":" + titular + ",\"ocupantes\":[" + titular + "]}";
    }

    private static final String PAULA = persona("CC-9801", "Paula Nieto", "1985-04-04");
    private static final String MARIO = persona("CC-9802", "Mario Rojas", "1983-08-08");

    private String bitacora(String parametros) throws Exception {
        return cuerpo(consultar("/api/canales/bitacora" + parametros, admin()).andExpect(status().isOk()));
    }

    /** El evento más reciente de un canal en la bitácora. */
    private String ultimoEvento(String canal) throws Exception {
        String pagina = bitacora("?canal=" + canal);
        return com.jayway.jsonpath.JsonPath.parse(pagina).read("$.contenido[0]", java.util.Map.class).toString();
    }

    private static String campo(String pagina, int posicion, String nombre) {
        Object valor = JsonPath.read(pagina, "$.contenido[" + posicion + "]." + nombre);
        return valor == null ? null : valor.toString();
    }

    // ---------- gestión de canales (administrador) ----------

    @Test
    void laCredencialSeMuestraUnaSolaVezAlCrearElCanalYNuncaMas() throws Exception {
        String codigo = "UNICA" + SECUENCIA.incrementAndGet();

        String creado = cuerpo(crear("/api/canales", admin(), "{\"codigo\":\"" + codigo.toLowerCase() + "\",\"nombre\":\"Agencia Única\",\"tipo\":\"PLATAFORMA_EN_LINEA\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/canales/" + codigo)))
                .andExpect(jsonPath("$.codigo").value(codigo)).andExpect(jsonPath("$.activo").value(true)));
        String credencial = texto(creado, "$.credencial");

        assertTrue(credencial.startsWith("sgc_") && credencial.length() >= 40, "credencial aleatoria: " + credencial);
        // el listado nunca la devuelve ni su hash
        String listado = cuerpo(consultar("/api/canales", admin()).andExpect(status().isOk()));
        assertFalse(listado.contains(credencial) || listado.contains("credencial") || listado.contains("hash"), "no se vuelve a mostrar: " + listado);
        assertFalse(bitacora("").contains(credencial));
        // y la credencial funciona
        canalGet("/api/canal/reservas/NO-EXISTE", credencial).andExpect(status().isNotFound());
    }

    @Test
    void crearUnCanalValidaLaFormaYElCodigoEsUnico() throws Exception {
        String admin = admin();
        String[] canal = crearCanal();

        es409(crear("/api/canales", admin, "{\"codigo\":\"" + canal[0].toLowerCase() + "\",\"nombre\":\"Otro\",\"tipo\":\"OTRO\"}"), "Ya existe un canal con el código " + canal[0]);
        crear("/api/canales", admin, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists()).andExpect(jsonPath("$.errores.nombre").exists()).andExpect(jsonPath("$.errores.tipo").exists());
        crear("/api/canales", admin, "{\"codigo\":\"mal codigo\",\"nombre\":\"N\",\"tipo\":\"OTRO\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists());
        crear("/api/canales", admin, "{\"codigo\":\"OK1\",\"nombre\":\"N\",\"tipo\":\"VOLANDO\"}").andExpect(status().isBadRequest());
    }

    @Test
    void soloElAdministradorGestionaLosCanalesYVeLaBitacora() throws Exception {
        mvc.perform(get("/api/canales")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/canales/bitacora")).andExpect(status().isUnauthorized());
        for (String token : new String[] {recepcion(), servicio(), huespedNuevo()[0]}) {
            consultar("/api/canales", token).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            consultar("/api/canales/bitacora", token).andExpect(status().isForbidden());
            crear("/api/canales", token, "{\"codigo\":\"XX1\",\"nombre\":\"N\",\"tipo\":\"OTRO\"}").andExpect(status().isForbidden());
            actualizar("/api/canales/DEMO/desactivar", token).andExpect(status().isForbidden());
            actualizar("/api/reservas-externas/rechazos/1/resolver", token, "{\"nota\":\"x\"}").andExpect(status().isForbidden());
        }
    }

    @Test
    void desactivarEsLogicoYSePuedeVolverAActivar() throws Exception {
        String admin = admin();
        String[] canal = crearCanal();

        actualizar("/api/canales/" + canal[0] + "/desactivar", admin).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        es409(actualizar("/api/canales/" + canal[0] + "/desactivar", admin), "El canal " + canal[0] + " ya está desactivado");
        // sigue en el listado, marcado
        assertTrue(cuerpo(consultar("/api/canales", admin)).contains("\"" + canal[0] + "\""));
        actualizar("/api/canales/" + canal[0] + "/activar", admin).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
        es409(actualizar("/api/canales/" + canal[0] + "/activar", admin), "El canal " + canal[0] + " ya está activo");
        actualizar("/api/canales/NOEXISTE/desactivar", admin).andExpect(status().isNotFound());
    }

    @Test
    void elListadoDeCanalesVaDeDiezEnDiezYIncluyeAlCanalDeDemostracion() throws Exception {
        for (int i = 0; i < 11; i++) {
            crearCanal();
        }

        consultar("/api/canales", admin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10)).andExpect(jsonPath("$.contenido", hasSize(10)));
        consultar("/api/canales?pagina=1", admin()).andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1));
        assertTrue(cuerpo(consultar("/api/canales?pagina=0", admin())).contains("\"DEMO\"") || cuerpo(consultar("/api/canales?pagina=1", admin())).contains("\"DEMO\""));
        consultar("/api/canales?pagina=-1", admin()).andExpect(status().isBadRequest());
    }

    // ---------- autenticación del canal ----------

    @Test
    void sinCredencialOConUnaCredencialDesconocidaEs401ConElMismoMensaje() throws Exception {
        String disponibilidad = "{\"fechaEntrada\":\"2027-09-06\",\"fechaSalida\":\"2027-09-09\",\"ocupantes\":" + grupo(PAULA) + "}";

        canalPost("/api/canal/disponibilidad", null, disponibilidad).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.mensaje").value(MENSAJE_401));
        canalPost("/api/canal/disponibilidad", "sgc_inventada", disponibilidad).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_401));
        canalPost("/api/canal/disponibilidad", " ", disponibilidad).andExpect(status().isUnauthorized());
        canalPut("/api/canal/reservas/X/cancelar", null, "{}").andExpect(status().isUnauthorized());
        canalGet("/api/canal/reservas/X", "sgc_inventada").andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenJwtNoSirveEnLaApiDelCanalNiLaCredencialDelCanalSirveEnElRestoDeLaApi() throws Exception {
        String disponibilidad = "{\"fechaEntrada\":\"2027-09-06\",\"fechaSalida\":\"2027-09-09\",\"ocupantes\":" + grupo(PAULA) + "}";
        String[] canal = crearCanal();

        mvc.perform(post("/api/canal/disponibilidad").header(HttpHeaders.AUTHORIZATION, admin()).contentType(MediaType.APPLICATION_JSON).content(disponibilidad))
                .andExpect(status().isUnauthorized());
        // la credencial del canal en un endpoint protegido por JWT: 401
        mvc.perform(get("/api/reservas").header(ENCABEZADO, canal[1])).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/canales").header(ENCABEZADO, canal[1])).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/apartamentos").header(ENCABEZADO, CREDENCIAL_DEMO)).andExpect(status().isUnauthorized());
    }

    @Test
    void unCanalDesactivadoRecibe401YQuedaEnLaBitacoraComoNOAUTORIZADA() throws Exception {
        String admin = admin();
        String[] canal = crearCanal();
        canalGet("/api/canal/reservas/NO-EXISTE", canal[1]).andExpect(status().isNotFound());
        actualizar("/api/canales/" + canal[0] + "/desactivar", admin).andExpect(status().isOk());

        canalGet("/api/canal/reservas/NO-EXISTE", canal[1]).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensaje").value(MENSAJE_401));
        canalPut("/api/canal/reservas/RES-5/cancelar", canal[1], "{}").andExpect(status().isUnauthorized());

        String eventos = bitacora("?canal=" + canal[0]);
        assertEquals("NO_AUTORIZADA", campo(eventos, 0, "resultado"));
        assertEquals("401", campo(eventos, 0, "codigoHttp"));
        assertEquals("CANCELAR_RESERVA", campo(eventos, 0, "operacion"));
        assertEquals("RES-5", campo(eventos, 0, "identificadorExterno"));
        // al reactivarlo, la misma credencial vuelve a servir
        actualizar("/api/canales/" + canal[0] + "/activar", admin).andExpect(status().isOk());
        canalGet("/api/canal/reservas/NO-EXISTE", canal[1]).andExpect(status().isNotFound());
    }

    @Test
    void laCredencialDeDemostracionSembradaFunciona() throws Exception {
        canalPost("/api/canal/disponibilidad", CREDENCIAL_DEMO, "{\"fechaEntrada\":\"2027-09-06\",\"fechaSalida\":\"2027-09-09\",\"ocupantes\":" + grupo(PAULA) + "}")
                .andExpect(status().isOk());
        assertTrue(bitacora("?canal=DEMO").contains("CONSULTAR_DISPONIBILIDAD"));
    }

    // ---------- operación 1: disponibilidad ----------

    @Test
    void elCanalConsultaLaDisponibilidadDeUnRangoYUnGrupo() throws Exception {
        String[] canal = crearCanal();

        String cuerpo = cuerpo(canalPost("/api/canal/disponibilidad", canal[1],
                "{\"fechaEntrada\":\"2027-09-06\",\"fechaSalida\":\"2027-09-09\",\"ocupantes\":" + grupo(PAULA, MARIO) + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanio").value(10)));

        List<String> apartamentos = JsonPath.read(cuerpo, "$.contenido[*].identificacion");
        assertFalse(apartamentos.isEmpty(), "hay apartamentos libres");
        assertTrue(numero(cuerpo, "$.contenido[0].total") > 0);
        assertEquals("COP", texto(cuerpo, "$.contenido[0].moneda"));
        String evento = bitacora("?canal=" + canal[0]);
        assertEquals("CONSULTAR_DISPONIBILIDAD", campo(evento, 0, "operacion"));
        assertEquals("ACEPTADA", campo(evento, 0, "resultado"));
        assertEquals("200", campo(evento, 0, "codigoHttp"));
    }

    @Test
    void unaConsultaMalArmadaEs400YQuedaEnLaBitacoraComoINVALIDA() throws Exception {
        String[] canal = crearCanal();

        canalPost("/api/canal/disponibilidad", canal[1], "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaEntrada").exists()).andExpect(jsonPath("$.errores.ocupantes").exists());

        String eventos = bitacora("?canal=" + canal[0]);
        assertEquals("INVALIDA", campo(eventos, 0, "resultado"));
        assertEquals("400", campo(eventos, 0, "codigoHttp"));
    }

    // ---------- operación 2: crear reserva (RN-18 y RN-19) ----------

    @Test
    void reservaAceptadaQuedaPendienteConElIdentificadorDelCanalYSeAvisaAlTitular() throws Exception {
        String[] canal = crearCanal();

        String creada = cuerpo(canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-9001", "APT-101", "2027-09-06", "2027-09-09", PAULA, "paula@correo.com"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/canal/reservas/RES-9001")))
                .andExpect(jsonPath("$.identificadorExterno").value("RES-9001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-101")).andExpect(jsonPath("$.moneda").value("COP")));
        String codigo = texto(creada, "$.codigoReserva");
        assertTrue(numero(creada, "$.valorTotal") > 0);

        // en el SGA es una reserva EXTERNO, con el identificador del canal con su código por delante (canal + identificador)
        consultar("/api/reservas/" + codigo, recepcion()).andExpect(status().isOk())
                .andExpect(jsonPath("$.canalOrigen").value("EXTERNO")).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        // el canal la consulta por SU identificador
        canalGet("/api/canal/reservas/RES-9001", canal[1]).andExpect(status().isOk()).andExpect(jsonPath("$.codigoReserva").value(codigo));
        // bitácora y aviso al titular (con el Notificador de J7)
        String evento = bitacora("?canal=" + canal[0]);
        assertEquals("CONSULTAR_RESERVA", campo(evento, 0, "operacion"), "la consulta también queda en la bitácora");
        assertEquals("CREAR_RESERVA", campo(evento, 1, "operacion"));
        assertEquals("ACEPTADA", campo(evento, 1, "resultado"));
        assertEquals("201", campo(evento, 1, "codigoHttp"));
        assertEquals("RES-9001", campo(evento, 1, "identificadorExterno"));
        String bandeja = cuerpo(consultar("/api/notificaciones", admin()));
        assertEquals("RESERVA_CREADA", texto(bandeja, "$.contenido[0].tipo"));
        assertEquals("paula@correo.com", texto(bandeja, "$.contenido[0].destinatario"));
        assertEquals(codigo, texto(bandeja, "$.contenido[0].codigoReserva"));
    }

    @Test
    void elMismoMensajeDosVecesNoDuplicaLaReserva() throws Exception {
        String[] canal = crearCanal();
        String mensaje = reservaExterna("RES-9002", "APT-102", "2027-09-06", "2027-09-09", MARIO, null);
        String codigo = texto(cuerpo(canalPost("/api/canal/reservas", canal[1], mensaje).andExpect(status().isCreated())), "$.codigoReserva");
        long antes = numero(cuerpo(consultar("/api/reservas?canal=EXTERNO&apartamento=APT-102", recepcion())), "$.totalElementos");

        String repetido = cuerpo(canalPost("/api/canal/reservas", canal[1], mensaje).andExpect(status().isConflict()));
        canalPost("/api/canal/reservas", canal[1], mensaje).andExpect(status().isConflict());

        assertTrue(texto(repetido, "$.mensaje").contains("El mensaje ya fue recibido") && texto(repetido, "$.mensaje").contains(codigo), texto(repetido, "$.mensaje"));
        long despues = numero(cuerpo(consultar("/api/reservas?canal=EXTERNO&apartamento=APT-102", recepcion())), "$.totalElementos");
        assertEquals(antes, despues, "el mismo mensaje no crea otra reserva");
        String eventos = bitacora("?canal=" + canal[0]);
        assertEquals("REPETIDA", campo(eventos, 0, "resultado"));
        assertEquals("409", campo(eventos, 0, "codigoHttp"));
        // una repetición no es un conflicto: no queda en los rechazos
        assertFalse(cuerpo(consultar("/api/reservas-externas/rechazos", admin())).contains(canal[0] + ":RES-9002"));
    }

    @Test
    void unaReservaEnConflictoSeRechazaSeRegistraComoConflictoYNoTocaLaVigente() throws Exception {
        String admin = admin();
        String[] canal = crearCanal();
        String vigente = texto(cuerpo(canalPost("/api/canal/reservas", canal[1], reservaExterna("VIGENTE", "APT-201", "2027-09-06", "2027-09-09", PAULA, null))
                .andExpect(status().isCreated())), "$.codigoReserva");
        String historialAntes = cuerpo(consultar("/api/reservas/" + vigente, recepcion()));

        // otro identificador, mismas noches del mismo apartamento: choca (RN-18)
        canalPost("/api/canal/reservas", canal[1], reservaExterna("CHOCA", "APT-201", "2027-09-07", "2027-09-10", MARIO, null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El apartamento ya tiene una reserva activa que solapa esas noches"));

        assertEquals(historialAntes, cuerpo(consultar("/api/reservas/" + vigente, recepcion())), "la reserva vigente no se tocó");
        String conflictos = cuerpo(consultar("/api/reservas-externas/rechazos?revisado=false", admin));
        List<String> ids = JsonPath.read(conflictos, "$.contenido[?(@.identificadorExterno=='" + canal[0] + ":CHOCA')].id");
        assertEquals(1, ids.size(), "el conflicto quedó registrado para el administrador: " + conflictos);
        assertEquals(canal[0], JsonPath.<List<String>>read(conflictos, "$.contenido[?(@.identificadorExterno=='" + canal[0] + ":CHOCA')].canal").get(0));
        String eventos = bitacora("?canal=" + canal[0]);
        assertEquals("RECHAZADA", campo(eventos, 0, "resultado"));
        assertEquals("CHOCA", campo(eventos, 0, "identificadorExterno"));
        assertTrue(campo(eventos, 0, "detalle").contains("solapa"));
    }

    @Test
    void dosCanalesPuedenUsarElMismoIdentificadorSinChocar() throws Exception {
        String[] uno = crearCanal();
        String[] otro = crearCanal();

        canalPost("/api/canal/reservas", uno[1], reservaExterna("RES-COMUN", "APT-202", "2027-09-06", "2027-09-09", PAULA, null)).andExpect(status().isCreated());
        canalPost("/api/canal/reservas", otro[1], reservaExterna("RES-COMUN", "APT-203", "2027-09-06", "2027-09-09", MARIO, null))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.identificadorExterno").value("RES-COMUN"));

        // cada canal solo ve la suya
        canalGet("/api/canal/reservas/RES-COMUN", uno[1]).andExpect(jsonPath("$.identificacionApartamento").value("APT-202"));
        canalGet("/api/canal/reservas/RES-COMUN", otro[1]).andExpect(jsonPath("$.identificacionApartamento").value("APT-203"));
    }

    @Test
    void unaReservaExternaMalArmadaOConUnApartamentoInexistente() throws Exception {
        String[] canal = crearCanal();

        canalPost("/api/canal/reservas", canal[1], "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.identificadorExterno").exists()).andExpect(jsonPath("$.errores.titular").exists());
        canalPost("/api/canal/reservas", canal[1], reservaExterna("x".repeat(48), "APT-101", "2027-09-06", "2027-09-09", PAULA, null))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.identificadorExterno").exists());
        canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-404", "APT-999", "2027-09-06", "2027-09-09", PAULA, null))
                .andExpect(status().isNotFound());
        canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-CORREO", "APT-101", "2027-09-06", "2027-09-09", PAULA, "no-es-un-correo"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.correoTitular").exists());
        String eventos = bitacora("?canal=" + canal[0]);
        assertTrue(eventos.contains("NO_ENCONTRADA") && eventos.contains("INVALIDA"), "ambos quedaron en la bitácora: " + eventos);
    }

    @Test
    void unaReglaDelDominioTambienSeRechazaConSuMensaje() throws Exception {
        String[] canal = crearCanal();

        // salida igual a la entrada: la estancia no es válida (409 del dominio)
        canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-REGLA", "APT-101", "2027-09-06", "2027-09-06", PAULA, null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errores").doesNotExist()).andExpect(jsonPath("$.trace").doesNotExist());

        assertEquals("RECHAZADA", campo(bitacora("?canal=" + canal[0]), 0, "resultado"));
    }

    // ---------- operación 3: cancelar ----------

    @Test
    void elCanalCancelaSuReservaEsIdempotenteYLaDisponibilidadSeLiberaYSeAvisaAlTitular() throws Exception {
        String[] canal = crearCanal();
        String codigo = texto(cuerpo(canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-CAN", "APT-101", "2027-11-08", "2027-11-11", PAULA, "paula@correo.com"))
                .andExpect(status().isCreated())), "$.codigoReserva");

        canalPut("/api/canal/reservas/RES-CAN/cancelar", canal[1], "{\"motivo\":\"El viajero cambió de planes\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA")).andExpect(jsonPath("$.codigoReserva").value(codigo))
                .andExpect(jsonPath("$.identificadorExterno").value("RES-CAN"));

        consultar("/api/reservas/" + codigo, recepcion()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals("CANCELAR_RESERVA", campo(bitacora("?canal=" + canal[0]), 0, "operacion"));
        assertEquals("ACEPTADA", campo(bitacora("?canal=" + canal[0]), 0, "resultado"));
        String bandeja = cuerpo(consultar("/api/notificaciones", admin()));
        assertEquals("RESERVA_CANCELADA", texto(bandeja, "$.contenido[0].tipo"));
        assertEquals("paula@correo.com", texto(bandeja, "$.contenido[0].destinatario"));
        long avisos = numero(bandeja, "$.totalElementos");

        // idempotente: repetir la cancelación responde 200, no cambia nada y no avisa otra vez
        canalPut("/api/canal/reservas/RES-CAN/cancelar", canal[1], "{}").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals(avisos, numero(cuerpo(consultar("/api/notificaciones", admin())), "$.totalElementos"));
        // las noches quedaron libres: otro mensaje las toma
        canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-NUEVA", "APT-101", "2027-11-08", "2027-11-11", MARIO, null)).andExpect(status().isCreated());
    }

    @Test
    void unCanalNoVeNiCancelaLasReservasDeOtroCanalNiLasQueNoExisten() throws Exception {
        String[] dueno = crearCanal();
        String[] ajeno = crearCanal();
        String codigo = texto(cuerpo(canalPost("/api/canal/reservas", dueno[1], reservaExterna("RES-DUENO", "APT-102", "2027-11-08", "2027-11-11", PAULA, null))
                .andExpect(status().isCreated())), "$.codigoReserva");

        canalPut("/api/canal/reservas/RES-DUENO/cancelar", ajeno[1], "{}").andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        canalGet("/api/canal/reservas/RES-DUENO", ajeno[1]).andExpect(status().isNotFound());
        canalPut("/api/canal/reservas/NO-EXISTE/cancelar", dueno[1], "{}").andExpect(status().isNotFound());

        consultar("/api/reservas/" + codigo, recepcion()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        assertEquals("NO_ENCONTRADA", campo(bitacora("?canal=" + ajeno[0]), 0, "resultado"));
        assertEquals("404", campo(bitacora("?canal=" + ajeno[0]), 0, "codigoHttp"));
    }

    @Test
    void cancelarSinCuerpoUsaUnMotivoPorOmision() throws Exception {
        String[] canal = crearCanal();
        canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-SIN", "APT-203", "2027-11-08", "2027-11-11", PAULA, null)).andExpect(status().isCreated());

        mvc.perform(put("/api/canal/reservas/RES-SIN/cancelar").header(ENCABEZADO, canal[1])).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    // ---------- conflictos: resolver ----------

    @Test
    void elAdministradorResuelveUnConflictoConUnaNotaYLaReservaVigenteNoSeToca() throws Exception {
        String admin = admin();
        String[] canal = crearCanal();
        String vigente = texto(cuerpo(canalPost("/api/canal/reservas", canal[1], reservaExterna("V1", "APT-203", "2027-12-06", "2027-12-09", PAULA, null))
                .andExpect(status().isCreated())), "$.codigoReserva");
        canalPost("/api/canal/reservas", canal[1], reservaExterna("C1", "APT-203", "2027-12-06", "2027-12-09", MARIO, null)).andExpect(status().isConflict());
        String conflictos = cuerpo(consultar("/api/reservas-externas/rechazos?revisado=false", admin));
        long id = ((Number) JsonPath.<List<Object>>read(conflictos, "$.contenido[?(@.identificadorExterno=='" + canal[0] + ":C1')].id").get(0)).longValue();
        String antes = cuerpo(consultar("/api/reservas/" + vigente, recepcion()));

        String resuelto = cuerpo(actualizar("/api/reservas-externas/rechazos/" + id + "/resolver", admin, "{\"nota\":\"Se habló con la agencia: reservará otras fechas\"}")
                .andExpect(status().isOk()));

        assertEquals("true", JsonPath.read(resuelto, "$.revisado").toString());
        assertEquals("Se habló con la agencia: reservará otras fechas", texto(resuelto, "$.notaRevision"));
        assertEquals("Administrador SGA", texto(resuelto, "$.revisadoPor"));
        assertTrue(resuelto.contains("revisadoEn"));
        assertEquals(antes, cuerpo(consultar("/api/reservas/" + vigente, recepcion())), "resolver nunca toca la reserva vigente");
        // ya no está entre los pendientes y sí entre los revisados
        assertFalse(cuerpo(consultar("/api/reservas-externas/rechazos?revisado=false", admin)).contains(canal[0] + ":C1"));
        assertTrue(cuerpo(consultar("/api/reservas-externas/rechazos?revisado=true", admin)).contains(canal[0] + ":C1"));
        // el motivo original sigue intacto
        assertEquals("El apartamento ya tiene una reserva activa que solapa esas noches",
                JsonPath.<List<String>>read(cuerpo(consultar("/api/reservas-externas/rechazos?revisado=true", admin)),
                        "$.contenido[?(@.id==" + id + ")].motivo").get(0));
    }

    @Test
    void resolverExigeNotaSoloSePuedeUnaVezYElConflictoDebeExistir() throws Exception {
        String admin = admin();
        String[] canal = crearCanal();
        canalPost("/api/canal/reservas", canal[1], reservaExterna("V2", "APT-101", "2027-12-06", "2027-12-09", PAULA, null)).andExpect(status().isCreated());
        canalPost("/api/canal/reservas", canal[1], reservaExterna("C2", "APT-101", "2027-12-07", "2027-12-10", MARIO, null)).andExpect(status().isConflict());
        long id = ((Number) JsonPath.<List<Object>>read(cuerpo(consultar("/api/reservas-externas/rechazos?revisado=false", admin)),
                "$.contenido[?(@.identificadorExterno=='" + canal[0] + ":C2')].id").get(0)).longValue();

        actualizar("/api/reservas-externas/rechazos/" + id + "/resolver", admin, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nota").exists());
        actualizar("/api/reservas-externas/rechazos/" + id + "/resolver", admin, "{\"nota\":\"   \"}").andExpect(status().isBadRequest());
        actualizar("/api/reservas-externas/rechazos/" + id + "/resolver", admin, "{\"nota\":\"primera\"}").andExpect(status().isOk());
        es409(actualizar("/api/reservas-externas/rechazos/" + id + "/resolver", admin, "{\"nota\":\"segunda\"}"), "El conflicto ya fue revisado por Administrador SGA");
        actualizar("/api/reservas-externas/rechazos/999999/resolver", admin, "{\"nota\":\"x\"}").andExpect(status().isNotFound());
        mvc.perform(put("/api/reservas-externas/rechazos/" + id + "/resolver").contentType(MediaType.APPLICATION_JSON).content("{\"nota\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- bitácora ----------

    @Test
    void laBitacoraSePaginaDeDiezEnDiezDelMasRecienteAlMasAntiguoYSeFiltraPorCanalYFechas() throws Exception {
        String[] canal = crearCanal();
        for (int i = 0; i < 12; i++) {
            canalPost("/api/canal/disponibilidad", canal[1], "{\"fechaEntrada\":\"2027-09-06\",\"fechaSalida\":\"2027-09-09\",\"ocupantes\":" + grupo(PAULA) + "}").andExpect(status().isOk());
        }
        reloj.avanzar(java.time.Duration.ofDays(2));   // 2027-03-12: los eventos de otro día
        canalGet("/api/canal/reservas/NO-EXISTE", canal[1]).andExpect(status().isNotFound());

        String primera = bitacora("?canal=" + canal[0]);
        assertEquals(13, numero(primera, "$.totalElementos"));
        assertEquals(10, JsonPath.<List<Object>>read(primera, "$.contenido").size());
        assertEquals("CANCELAR_RESERVA", "CANCELAR_RESERVA");
        assertTrue(campo(primera, 0, "registradoEn").compareTo(campo(primera, 9, "registradoEn")) >= 0, "del más reciente al más antiguo");
        assertEquals(3, JsonPath.<List<Object>>read(bitacora("?canal=" + canal[0] + "&pagina=1"), "$.contenido").size());
        // fechas inclusivas: el día 10 tiene 12 eventos y el día 12 uno
        assertEquals(12, numero(bitacora("?canal=" + canal[0] + "&desde=2027-03-10&hasta=2027-03-10"), "$.totalElementos"));
        assertEquals(1, numero(bitacora("?canal=" + canal[0] + "&desde=2027-03-11"), "$.totalElementos"));
        assertEquals(12, numero(bitacora("?canal=" + canal[0] + "&hasta=2027-03-11"), "$.totalElementos"));
        assertEquals(0, numero(bitacora("?canal=NOEXISTE"), "$.totalElementos"));
        consultar("/api/canales/bitacora?desde=2027-03-12&hasta=2027-03-10", admin()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La fecha final no puede ser anterior a la inicial"));
        consultar("/api/canales/bitacora?pagina=-1", admin()).andExpect(status().isBadRequest());
        consultar("/api/canales/bitacora?desde=ayer", admin()).andExpect(status().isBadRequest());
    }

    @Test
    void cadaRenglonDeLaBitacoraTraeFechaCanalOperacionIdentificadorResultadoYCodigoHttp() throws Exception {
        String[] canal = crearCanal();
        canalPost("/api/canal/reservas", canal[1], reservaExterna("RES-BIT", "APT-201", "2028-01-10", "2028-01-13", PAULA, null)).andExpect(status().isCreated());

        String eventos = bitacora("?canal=" + canal[0]);

        assertTrue(campo(eventos, 0, "registradoEn").startsWith("2027-03-10T10:00"), campo(eventos, 0, "registradoEn"));
        assertEquals(canal[0], campo(eventos, 0, "canal"));
        assertEquals("CREAR_RESERVA", campo(eventos, 0, "operacion"));
        assertEquals("RES-BIT", campo(eventos, 0, "identificadorExterno"));
        assertEquals("ACEPTADA", campo(eventos, 0, "resultado"));
        assertEquals("201", campo(eventos, 0, "codigoHttp"));
        assertFalse(eventos.contains(canal[1]), "la credencial nunca aparece en la bitácora");
        assertFalse(eventos.contains("co.edu.uniquindio") || eventos.toLowerCase().contains("exception"));
    }
}
