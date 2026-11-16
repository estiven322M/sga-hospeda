package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

/**
 * L-06 de extremo a extremo: aplicación completa (H2, datos sembrados, seguridad JWT real). Solo lee, de modo que
 * puede compartir contexto con el resto de pruebas de integración. Lo que se comprueba es el CUERPO de cada
 * respuesta: qué apartamentos devuelve cada filtro, no solo que el código sea 200.
 *
 * Datos sembrados (CatalogoDatosPrueba): TERRAZA = 102 y 301; BALCON = 101, 201 y 302; ACCESIBLE = 202 y 203;
 * PARQUEADERO = 102 y 202; JACUZZI = 301; VISTA_AL_PARQUE = 101; VISTA_PANORAMICA = 301.
 */
@SpringBootTest
class ApartamentoCaracteristicasIntegracionTest {

    private static final String IDS = "$.contenido[*].identificacion";

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
    }

    private String token(String correo, String contrasena) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    private String recepcion() throws Exception { return token("recepcion@sga.local", "Recepcion2026"); }

    // ---------- Catálogo: GET /api/caracteristicas ----------

    @Test
    void elCatalogoLoVenLosCuatroRolesYDevuelveLosSieteValores() throws Exception {
        List<String> credenciales = List.of("admin@sga.local:Admin2026", "recepcion@sga.local:Recepcion2026",
                "servicio@sga.local:Servicio2026", "huesped1@sga.local:Huesped2026");
        for (String par : credenciales) {
            String[] partes = par.split(":");
            mvc.perform(get("/api/caracteristicas").header(HttpHeaders.AUTHORIZATION, token(partes[0], partes[1])))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", containsInAnyOrder("TERRAZA", "BALCON", "VISTA_AL_PARQUE",
                            "VISTA_PANORAMICA", "JACUZZI", "PARQUEADERO", "ACCESIBLE")));
        }
    }

    @Test
    void elCatalogoSinTokenResponde401() throws Exception {
        mvc.perform(get("/api/caracteristicas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ---------- Datos sembrados ----------

    @Test
    void cadaApartamentoSembradoTieneDescripcionDotacionYAlMenosUnaCaracteristica() throws Exception {
        String cuerpo = mvc.perform(get("/api/apartamentos").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int total = JsonPath.read(cuerpo, "$.contenido.length()");
        assertTrue(total >= 5, "el negocio exige al menos 5 apartamentos");
        Set<Set<String>> conjuntosDistintos = new HashSet<>();
        for (int i = 0; i < total; i++) {
            String id = JsonPath.read(cuerpo, "$.contenido[" + i + "].identificacion");
            String descripcion = JsonPath.read(cuerpo, "$.contenido[" + i + "].descripcion");
            List<String> dotacion = JsonPath.read(cuerpo, "$.contenido[" + i + "].dotacion");
            List<String> caracteristicas = JsonPath.read(cuerpo, "$.contenido[" + i + "].caracteristicas");
            assertFalse(descripcion.isBlank(), id + " sin descripción");
            assertFalse(dotacion.isEmpty(), id + " sin dotación");
            assertFalse(caracteristicas.isEmpty(), id + " sin característica diferenciadora");
            conjuntosDistintos.add(new HashSet<>(caracteristicas));
        }
        // diferenciadoras: no todos los apartamentos comparten el mismo conjunto
        assertTrue(conjuntosDistintos.size() >= 4, "los apartamentos deberían diferenciarse entre sí");
    }

    @Test
    void consultarUnApartamentoDevuelveSuDescripcionDotacionYCaracteristicas() throws Exception {
        mvc.perform(get("/api/apartamentos/APT-301").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacion").value("APT-301"))
                .andExpect(jsonPath("$.descripcion").value(
                        "Penthouse en el último piso con terraza, jacuzzi y vista panorámica de la ciudad."))
                .andExpect(jsonPath("$.caracteristicas",
                        containsInAnyOrder("VISTA_PANORAMICA", "JACUZZI", "TERRAZA")))
                .andExpect(jsonPath("$.dotacion", hasItem("Aire acondicionado")));
    }

    // ---------- GET /api/apartamentos?caracteristicas=... ----------

    @Test
    void elListadoSinFiltroDevuelveTodosLosApartamentosSembrados() throws Exception {
        mvc.perform(get("/api/apartamentos").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, hasItem("APT-101")))
                .andExpect(jsonPath(IDS, hasItem("APT-302")));
    }

    @Test
    void filtrarPorTerrazaDevuelveExactamenteLosApartamentosQueLaTienen() throws Exception {
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "TERRAZA")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-102", "APT-301")))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void filtrarPorBalconDevuelveSusTresApartamentos() throws Exception {
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "BALCON")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-101", "APT-201", "APT-302")));
    }

    @Test
    void filtrarPorAccesibleYPorJacuzziDevuelveConjuntosDistintos() throws Exception {
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "ACCESIBLE")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-202", "APT-203")));
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "JACUZZI")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-301")));
    }

    @Test
    void conVariasCaracteristicasElApartamentoDebeTenerTodasNoAlguna() throws Exception {
        // TERRAZA la tienen 102 y 301; PARQUEADERO la tienen 102 y 202; juntas: solo el 102
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "TERRAZA", "PARQUEADERO")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-102")))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void unaCombinacionSinCoincidenciasDevuelveUnaPaginaVacia() throws Exception {
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "JACUZZI", "ACCESIBLE")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", empty()))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void unaCaracteristicaFueraDelCatalogoResponde400ConErrorResponse() throws Exception {
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "PISCINA")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- POST /api/disponibilidad con filtro de características ----------

    private String disponibilidad(String caracteristicasJson) {
        return "{\"fechaEntrada\":\"2028-08-21\",\"fechaSalida\":\"2028-08-24\"," + caracteristicasJson
                + "\"ocupantes\":[{\"documento\":\"CC-7001\",\"nombre\":\"Adulto Uno\",\"fechaNacimiento\":\"1990-01-01\"}]}";
    }

    @Test
    void laDisponibilidadSinFiltroTraeLosSieteApartamentosYCadaTarjetaConSusDatosDescriptivos() throws Exception {
        String cuerpo = mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion()).content(disponibilidad("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, hasSize(7)))
                .andReturn().getResponse().getContentAsString();

        List<String> descripcion = JsonPath.read(cuerpo, "$.contenido[?(@.identificacion=='APT-301')].descripcion");
        List<List<String>> caracteristicas =
                JsonPath.read(cuerpo, "$.contenido[?(@.identificacion=='APT-301')].caracteristicas");
        assertEquals(1, descripcion.size());
        assertTrue(descripcion.get(0).startsWith("Penthouse"));
        assertTrue(caracteristicas.get(0).contains("JACUZZI"));
    }

    @Test
    void laDisponibilidadFiltraPorUnaCaracteristica() throws Exception {
        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion())
                        .content(disponibilidad("\"caracteristicas\":[\"TERRAZA\"],")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-102", "APT-301")));
    }

    @Test
    void laDisponibilidadConVariasCaracteristicasExigeTodas() throws Exception {
        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion())
                        .content(disponibilidad("\"caracteristicas\":[\"TERRAZA\",\"PARQUEADERO\"],")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-102")));
        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion())
                        .content(disponibilidad("\"caracteristicas\":[\"JACUZZI\",\"ACCESIBLE\"],")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", empty()));
    }

    @Test
    void laDisponibilidadCombinaElFiltroDeCaracteristicasConLaCapacidadMinima() throws Exception {
        // BALCON: 101 (cap. 4), 201 (cap. 3) y 302 (cap. 2); con capacidad mínima 4 solo queda el 101
        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion())
                        .content(disponibilidad("\"capacidadMinima\":4,\"caracteristicas\":[\"BALCON\"],")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-101")));
    }

    @Test
    void laDisponibilidadConUnaCaracteristicaFueraDelCatalogoResponde400() throws Exception {
        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion())
                        .content(disponibilidad("\"caracteristicas\":[\"PISCINA\"],")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elTotalYElDesgloseDeLaTarjetaSiguenPresentesJuntoALasCaracteristicas() throws Exception {
        String cuerpo = mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, recepcion())
                        .content(disponibilidad("\"caracteristicas\":[\"JACUZZI\"],")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals("APT-301", JsonPath.read(cuerpo, "$.contenido[0].identificacion"));
        assertEquals(3, (int) JsonPath.read(cuerpo, "$.contenido[0].desglose.length()"));
        // 3 noches en BASE (21 a 23 de agosto de 2028), 1 ocupante facturable, tarifa base de 140.000
        assertEquals(420_000, ((Number) JsonPath.read(cuerpo, "$.contenido[0].total")).longValue());
    }
}
