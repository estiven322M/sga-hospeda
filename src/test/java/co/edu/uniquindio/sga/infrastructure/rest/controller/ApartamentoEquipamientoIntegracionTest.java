package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

/**
 * L-06 de extremo a extremo, parte que ESCRIBE: crear y editar apartamentos por HTTP con seguridad real. Como deja
 * apartamentos nuevos en la base de datos, descarta el contexto al terminar para no alterar las pruebas que cuentan
 * los apartamentos sembrados.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApartamentoEquipamientoIntegracionTest {

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

    private String admin() throws Exception { return token("admin@sga.local", "Admin2026"); }

    private static String cuerpoCrear(String id, String descripcionYDemas) {
        return "{\"identificacion\":\"" + id + "\",\"nombre\":\"Apartamento de prueba\"," + descripcionYDemas + "}";
    }

    @Test
    void elAdministradorCreaUnApartamentoConDotacionYCaracteristicasYLasConsultaDespues() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .content(cuerpoCrear("L06-NUEVO", "\"descripcion\":\"Cabaña con jacuzzi\",\"dormitorios\":2,"
                                + "\"capacidad\":4,\"dotacion\":[\"Nevera\",\"Chimenea\"],"
                                + "\"caracteristicas\":[\"JACUZZI\",\"VISTA_PANORAMICA\"]")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/L06-NUEVO")))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.descripcion").value("Cabaña con jacuzzi"))
                .andExpect(jsonPath("$.dotacion[0]").value("Nevera"))
                .andExpect(jsonPath("$.dotacion[1]").value("Chimenea"))
                .andExpect(jsonPath("$.caracteristicas", containsInAnyOrder("JACUZZI", "VISTA_PANORAMICA")));

        // lo guardado se lee igual desde la base de datos
        mvc.perform(get("/api/apartamentos/L06-NUEVO").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dotacion[1]").value("Chimenea"))
                .andExpect(jsonPath("$.caracteristicas", containsInAnyOrder("JACUZZI", "VISTA_PANORAMICA")));
        // y el filtro del listado ya lo encuentra junto al de la semilla
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "JACUZZI")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-301", "L06-NUEVO")));
    }

    @Test
    void elAdministradorEditaYLaDotacionYLasCaracteristicasSeReemplazan() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .content(cuerpoCrear("L06-EDITAR", "\"descripcion\":\"Original\",\"dormitorios\":1,"
                                + "\"capacidad\":2,\"dotacion\":[\"Nevera\"],\"caracteristicas\":[\"BALCON\"]")))
                .andExpect(status().isCreated());

        mvc.perform(put("/api/apartamentos/L06-EDITAR").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .content("{\"nombre\":\"Editado\",\"descripcion\":\"Descripción nueva\",\"dormitorios\":2,"
                                + "\"capacidad\":3,\"dotacion\":[\"Lavadora\",\"Wi-Fi\"],\"caracteristicas\":[\"ACCESIBLE\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Descripción nueva"))
                .andExpect(jsonPath("$.dotacion[0]").value("Lavadora"))
                .andExpect(jsonPath("$.dotacion.length()").value(2))
                .andExpect(jsonPath("$.caracteristicas", containsInAnyOrder("ACCESIBLE")));

        // la característica anterior ya no lo encuentra y la nueva sí
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "BALCON")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath(IDS, not(hasItem("L06-EDITAR"))));
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "ACCESIBLE")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath(IDS, hasItem("L06-EDITAR")));
    }

    @Test
    void unaDotacionRepetidaResponde409ConElMensajeDeLaReglaYNoCreaNada() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .content(cuerpoCrear("L06-REPETIDA", "\"descripcion\":\"D\",\"dormitorios\":1,\"capacidad\":2,"
                                + "\"dotacion\":[\"Nevera\",\"nevera\"]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("La dotación no puede repetir elementos: nevera"));

        mvc.perform(get("/api/apartamentos/L06-REPETIDA").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinDescripcionResponde400ConElCampoEnErrores() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .content(cuerpoCrear("L06-SIN-DESC", "\"dormitorios\":1,\"capacidad\":2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").value("La descripción del apartamento es obligatoria"));
    }

    @Test
    void unaCaracteristicaFueraDelCatalogoAlCrearResponde400() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .content(cuerpoCrear("L06-PISCINA", "\"descripcion\":\"D\",\"dormitorios\":1,\"capacidad\":2,"
                                + "\"caracteristicas\":[\"PISCINA\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unHuespedNoPuedeCrearApartamentosResponde403() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, token("huesped1@sga.local", "Huesped2026"))
                        .content(cuerpoCrear("L06-403", "\"descripcion\":\"D\",\"dormitorios\":1,\"capacidad\":2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
