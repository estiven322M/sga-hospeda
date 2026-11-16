package co.edu.uniquindio.sga.infrastructure.rest.controller;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

/**
 * L-20 de extremo a extremo: aplicación completa (H2, datos sembrados, seguridad JWT con login real) y cada regla
 * propia disparada por HTTP. Lo que se comprueba es el CUERPO: en los 409, el estado, el mensaje exacto de la regla y la
 * ausencia de trazas técnicas; en el descuento, cada cifra de la cotización y que la reserva creada congele el mismo
 * total. Todas las pruebas usan el apartamento APT-202 (tarifa base 100.000; ALTA 160.000) en fechas de 2028 que no se
 * pisan entre sí. Escribe reservas, así que descarta el contexto al terminar.
 *
 * Calendario sembrado de 2028: ALTA del 15-jun al 15-jul; MEDIA del 30-abr al 4-may; el resto es BASE.
 * Parámetros: ALTA=3 y MEDIA=2 noches mínimas; descuento del 10 % desde 7 noches; adulto responsable desde los 18 años.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reglaspropias")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReglasPropiasIntegracionTest {

    private static final String APT = "APT-202";
    private static final String MENSAJE_ALTA = "En temporada ALTA la estancia mínima es de 3 noches";
    private static final String MENSAJE_MEDIA = "En temporada MEDIA la estancia mínima es de 2 noches";
    private static final String MENSAJE_RP03 = "La reserva debe incluir al menos un ocupante mayor de 18 años";

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
    }

    // ---------- utilidades ----------

    private String recepcion() throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"recepcion@sga.local\",\"contrasena\":\"Recepcion2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    private static String persona(String documento, String nacimiento) {
        return "{\"documento\":\"" + documento + "\",\"nombre\":\"Persona " + documento
                + "\",\"fechaNacimiento\":\"" + nacimiento + "\"}";
    }

    private static final String ADULTO = persona("CC-8001", "1990-01-01");

    private static String cotizacion(String entrada, String salida, String... ocupantes) {
        return "{\"identificacionApartamento\":\"" + APT + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"ocupantes\":[" + String.join(",", ocupantes) + "]}";
    }

    private static String reserva(String entrada, String salida, String titular, String... ocupantes) {
        return "{\"identificacionApartamento\":\"" + APT + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"canalOrigen\":\"DIRECTO\",\"titular\":" + titular
                + ",\"ocupantes\":[" + String.join(",", ocupantes) + "]}";
    }

    private static String modificacion(String entrada, String salida, String... ocupantes) {
        return "{\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + salida + "\",\"autor\":\"Recepción\","
                + "\"ocupantes\":[" + String.join(",", ocupantes) + "]}";
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
                                 String cuerpo) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, recepcion()).content(cuerpo));
    }

    /** Un 409 de regla de negocio: estado, mensaje exacto y nada técnico (ni trazas ni nombres de clase). */
    private void esReglaDeNegocio(ResultActions respuesta, String mensaje) throws Exception {
        MvcResult resultado = respuesta
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value(mensaje))
                .andExpect(jsonPath("$.errores").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn();
        String cuerpo = resultado.getResponse().getContentAsString();
        assertFalse(cuerpo.contains("Exception"), "el cuerpo no debe filtrar nombres de excepciones: " + cuerpo);
        assertFalse(cuerpo.contains("co.edu.uniquindio"), "el cuerpo no debe filtrar clases: " + cuerpo);
        assertFalse(cuerpo.contains("\tat "), "el cuerpo no debe traer trazas: " + cuerpo);
    }

    private static long numero(String json, String ruta) {
        return ((Number) JsonPath.read(json, ruta)).longValue();
    }

    // ---------- RP-01 · estancia mínima por temporada ----------

    @Test
    void rp01_cotizarUnaNocheEnAltaResponde409ConElMensajeDeLaRegla() throws Exception {
        esReglaDeNegocio(enviar(post("/api/cotizaciones"), cotizacion("2028-07-11", "2028-07-12", ADULTO)),
                MENSAJE_ALTA);
    }

    @Test
    void rp01_cotizarDosNochesEnAltaTambienResponde409() throws Exception {
        esReglaDeNegocio(enviar(post("/api/cotizaciones"), cotizacion("2028-07-11", "2028-07-13", ADULTO)),
                MENSAJE_ALTA);
    }

    @Test
    void rp01_cotizarTresNochesEnAltaFuncionaYCobraLaTarifaDeAlta() throws Exception {
        enviar(post("/api/cotizaciones"), cotizacion("2028-07-11", "2028-07-14", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(3))
                .andExpect(jsonPath("$.desglose[0].temporada").value("ALTA"))
                .andExpect(jsonPath("$.subtotalBruto").value(480_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(0))
                .andExpect(jsonPath("$.total").value(480_000));
    }

    @Test
    void rp01_unaNocheEnMediaResponde409ConElMinimoDeMedia() throws Exception {
        esReglaDeNegocio(enviar(post("/api/cotizaciones"), cotizacion("2028-05-01", "2028-05-02", ADULTO)),
                MENSAJE_MEDIA);
        enviar(post("/api/cotizaciones"), cotizacion("2028-05-01", "2028-05-03", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(2));
    }

    @Test
    void rp01_unaNocheEnBaseSePermite() throws Exception {
        enviar(post("/api/cotizaciones"), cotizacion("2028-03-20", "2028-03-21", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(1))
                .andExpect(jsonPath("$.desglose[0].temporada").value("BASE"));
    }

    @Test
    void rp01_unaEstanciaQueCruzaDeBaseAAltaConDosNochesResponde409() throws Exception {
        // 14-jun es BASE y 15-jun es ALTA: basta una noche en ALTA para exigir sus 3 noches
        esReglaDeNegocio(enviar(post("/api/cotizaciones"), cotizacion("2028-06-14", "2028-06-16", ADULTO)),
                MENSAJE_ALTA);
        enviar(post("/api/cotizaciones"), cotizacion("2028-06-14", "2028-06-17", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose[0].temporada").value("BASE"))
                .andExpect(jsonPath("$.desglose[1].temporada").value("ALTA"));
    }

    @Test
    void rp01_crearUnaReservaConMenosNochesQueElMinimoResponde409YNoLaGuarda() throws Exception {
        esReglaDeNegocio(enviar(post("/api/reservas"), reserva("2028-07-02", "2028-07-04", ADULTO, ADULTO)),
                "En temporada ALTA la estancia mínima es de 3 noches");

        // la estancia quedó libre: la misma estancia, pero de 3 noches, se reserva sin problema
        enviar(post("/api/reservas"), reserva("2028-07-02", "2028-07-05", ADULTO, ADULTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.noches").value(3));
    }

    @Test
    void rp01_modificarUnaReservaDeBaseParaQuedarEnAltaConDosNochesResponde409YNoCambiaNada() throws Exception {
        String creada = enviar(post("/api/reservas"), reserva("2028-03-01", "2028-03-03", ADULTO, ADULTO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creada, "$.codigo");

        esReglaDeNegocio(enviar(put("/api/reservas/" + codigo + "/modificar"),
                modificacion("2028-06-15", "2028-06-17", ADULTO)), MENSAJE_ALTA);

        // la reserva conserva sus fechas originales
        mvc.perform(get("/api/reservas/" + codigo).header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaEntrada").value("2028-03-01"))
                .andExpect(jsonPath("$.noches").value(2));
        // con 3 noches la modificación sí procede
        enviar(put("/api/reservas/" + codigo + "/modificar"), modificacion("2028-06-15", "2028-06-18", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(3))
                .andExpect(jsonPath("$.desglose[0].temporada").value("ALTA"));
    }

    @Test
    void rp01_laBusquedaDeDisponibilidadDeUnaNocheEnAltaTambienResponde409() throws Exception {
        esReglaDeNegocio(enviar(post("/api/disponibilidad"),
                "{\"fechaEntrada\":\"2028-07-11\",\"fechaSalida\":\"2028-07-12\",\"ocupantes\":[" + ADULTO + "]}"),
                MENSAJE_ALTA);
    }

    // ---------- RP-02 · descuento por estancia larga ----------

    @Test
    void rp02_conSeisNochesNoHayDescuento() throws Exception {
        enviar(post("/api/cotizaciones"), cotizacion("2028-09-01", "2028-09-07", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(6))
                .andExpect(jsonPath("$.subtotalBruto").value(600_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(0))
                .andExpect(jsonPath("$.descuento").value(0))
                .andExpect(jsonPath("$.total").value(600_000));
    }

    @Test
    void rp02_conSieteNochesLaCotizacionMuestraElDescuentoYElTotalEsBrutoMenosDescuento() throws Exception {
        String cuerpo = enviar(post("/api/cotizaciones"), cotizacion("2028-09-10", "2028-09-17", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(7))
                .andExpect(jsonPath("$.desglose.length()").value(7))
                .andExpect(jsonPath("$.subtotalBruto").value(700_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(10))
                .andExpect(jsonPath("$.descuento").value(70_000))
                .andExpect(jsonPath("$.total").value(630_000))
                .andReturn().getResponse().getContentAsString();

        // el desglose noche por noche sigue siendo el bruto: el descuento no se reparte entre las noches
        assertEquals(700_000, numero(cuerpo, "$.subtotalBruto"));
        assertEquals(numero(cuerpo, "$.subtotalBruto") - numero(cuerpo, "$.descuento"), numero(cuerpo, "$.total"));
        List<Number> subtotales = JsonPath.read(cuerpo, "$.desglose[*].subtotal");
        assertEquals(700_000, subtotales.stream().mapToLong(Number::longValue).sum());
    }

    @Test
    void rp02_elDescuentoSeCalculaSobreTodosLosOcupantesFacturables() throws Exception {
        String niño = persona("TI-8100", "2021-01-01"); // 7 años en 2028: no paga
        enviar(post("/api/cotizaciones"), cotizacion("2028-09-20", "2028-09-27", ADULTO, persona("CC-8002", "1988-02-02"), niño))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalBruto").value(1_400_000))
                .andExpect(jsonPath("$.descuento").value(140_000))
                .andExpect(jsonPath("$.total").value(1_260_000));
    }

    @Test
    void rp02_laReservaDeSieteNochesCongelaElMismoTotalQueLaCotizacionYLoConservaAlLeerla() throws Exception {
        String cotizada = enviar(post("/api/cotizaciones"), cotizacion("2028-10-01", "2028-10-08", ADULTO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long totalCotizado = numero(cotizada, "$.total");
        assertEquals(630_000, totalCotizado);

        MvcResult creada = enviar(post("/api/reservas"), reserva("2028-10-01", "2028-10-08", ADULTO, ADULTO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.noches").value(7))
                .andExpect(jsonPath("$.subtotalBruto").value(700_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(10))
                .andExpect(jsonPath("$.descuento").value(70_000))
                .andExpect(jsonPath("$.valorTotal").value(630_000))
                .andReturn();
        String codigo = JsonPath.read(creada.getResponse().getContentAsString(), "$.codigo");
        assertEquals(totalCotizado, numero(creada.getResponse().getContentAsString(), "$.valorTotal"));
        assertTrue(creada.getResponse().getHeader("Location").endsWith("/api/reservas/" + codigo));

        // leída de la base de datos en otra petición: el descuento sigue congelado
        mvc.perform(get("/api/reservas/" + codigo).header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalBruto").value(700_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(10))
                .andExpect(jsonPath("$.descuento").value(70_000))
                .andExpect(jsonPath("$.valorTotal").value(630_000));
        // el folio se abre con el valor ya descontado
        mvc.perform(get("/api/reservas/" + codigo + "/folio").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos[0].valor").value(630_000))
                .andExpect(jsonPath("$.saldo").value(630_000));
    }

    @Test
    void rp02_alModificarSeRecalculaConLosParametrosVigentesYElAjusteDelFolioEsLaDiferenciaDeLosTotales() throws Exception {
        String creada = enviar(post("/api/reservas"), reserva("2028-11-20", "2028-11-27", ADULTO, ADULTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(630_000))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creada, "$.codigo");

        // de 7 a 6 noches se pierde el descuento: 600.000 de bruto sin descuento
        enviar(put("/api/reservas/" + codigo + "/modificar"), modificacion("2028-11-20", "2028-11-26", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(6))
                .andExpect(jsonPath("$.subtotalBruto").value(600_000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(0))
                .andExpect(jsonPath("$.descuento").value(0))
                .andExpect(jsonPath("$.valorTotal").value(600_000));
        mvc.perform(get("/api/reservas/" + codigo + "/folio").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos[0].valor").value(630_000))   // el cargo original no se toca
                .andExpect(jsonPath("$.ajustes[0].valor").value(-30_000))  // 600.000 - 630.000
                .andExpect(jsonPath("$.saldo").value(600_000));

        // y de 6 a 7 noches lo recupera: la diferencia es 630.000 - 600.000
        enviar(put("/api/reservas/" + codigo + "/modificar"), modificacion("2028-11-20", "2028-11-27", ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeDescuento").value(10))
                .andExpect(jsonPath("$.valorTotal").value(630_000));
    }

    @Test
    void rp02_laTarjetaDeDisponibilidadMuestraElDescuentoYElTotalNeto() throws Exception {
        String cuerpo = enviar(post("/api/disponibilidad"),
                "{\"fechaEntrada\":\"2028-08-20\",\"fechaSalida\":\"2028-08-27\",\"ocupantes\":[" + ADULTO + "]}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Number> brutos = JsonPath.read(cuerpo, "$.contenido[?(@.identificacion=='" + APT + "')].subtotalBruto");
        List<Number> porcentajes = JsonPath.read(cuerpo, "$.contenido[?(@.identificacion=='" + APT + "')].porcentajeDescuento");
        List<Number> descuentos = JsonPath.read(cuerpo, "$.contenido[?(@.identificacion=='" + APT + "')].descuento");
        List<Number> totales = JsonPath.read(cuerpo, "$.contenido[?(@.identificacion=='" + APT + "')].total");
        assertEquals(700_000, brutos.get(0).longValue());
        assertEquals(10, porcentajes.get(0).intValue());
        assertEquals(70_000, descuentos.get(0).longValue());
        assertEquals(630_000, totales.get(0).longValue());
    }

    // ---------- RP-03 · adulto responsable ----------

    @Test
    void rp03_unaReservaSoloConUnMenorResponde409ConElMensajeDeLaRegla() throws Exception {
        String menor = persona("TI-8201", "2013-06-01"); // 15 años: paga (≥12) pero no es adulto

        esReglaDeNegocio(enviar(post("/api/reservas"), reserva("2028-10-20", "2028-10-23", menor, menor)),
                MENSAJE_RP03);
    }

    @Test
    void rp03_conDiecisieteAniosYTrescientosSesentaYCuatroDiasALaEntradaResponde409() throws Exception {
        // entrada el 20-oct-2028; cumple 18 el 21-oct-2028
        String casiAdulto = persona("TI-8202", "2010-10-21");

        esReglaDeNegocio(enviar(post("/api/reservas"), reserva("2028-10-20", "2028-10-23", casiAdulto, casiAdulto)),
                MENSAJE_RP03);
    }

    @Test
    void rp03_conExactamenteDieciochoAniosALaEntradaLaReservaSeCrea() throws Exception {
        String recienAdulto = persona("CC-8203", "2010-10-20"); // cumple 18 el día de la entrada

        enviar(post("/api/reservas"), reserva("2028-10-20", "2028-10-23", recienAdulto, recienAdulto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void rp03_laCotizacionNoValidaLaComposicionDelGrupoSoloElPrecio() throws Exception {
        // decisión de diseño: cotizar calcula precio; la composición del grupo se exige al reservar
        String menor = persona("TI-8204", "2013-06-01");

        enviar(post("/api/cotizaciones"), cotizacion("2028-10-26", "2028-10-29", menor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(300_000)); // 3 noches x 100.000 (el menor de 15 años sí paga)
    }

    @Test
    void rp03_modificarElGrupoParaQuedarseSinAdultoResponde409YLaReservaConservaSusOcupantes() throws Exception {
        String menor = persona("TI-8205", "2013-06-01");
        String adulto = persona("CC-8206", "1985-03-03");
        String creada = enviar(post("/api/reservas"), reserva("2028-12-01", "2028-12-04", menor, menor, adulto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupantes.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creada, "$.codigo");

        esReglaDeNegocio(enviar(put("/api/reservas/" + codigo + "/modificar"),
                modificacion("2028-12-01", "2028-12-04", menor)), MENSAJE_RP03);

        mvc.perform(get("/api/reservas/" + codigo).header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes.length()").value(2))
                .andExpect(jsonPath("$.titular.documento").value("TI-8205"));
    }
}
