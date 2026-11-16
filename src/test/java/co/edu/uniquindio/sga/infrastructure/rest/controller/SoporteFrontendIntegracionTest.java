package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Lo que el frontend necesita del backend (Guías 15, 18 y 20), de extremo a extremo y comprobando el CUERPO:
 * <ul>
 *   <li>{@code GET /api/reservas/mias}: las reservas del huésped autenticado (por el claim documento del token),
 *       de 10 en 10, de la más reciente a la más antigua, con filtro de estado; un huésped no ve las de otro.</li>
 *   <li>{@code GET /api/reservas}: llegadas y salidas previstas (salidaDesde/salidaHasta) y búsqueda por el nombre del
 *       titular (contiene, sin distinguir mayúsculas), sin cambiar el significado del filtro {@code titular}.</li>
 *   <li>{@code GET /api/apartamentos?estadoOperativo=}: lo que el personal de servicio necesita para preparar.</li>
 * </ul>
 * Tiene su propia base H2 porque escribe (crea y cancela reservas del huésped que registra).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:soportefrontend")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SoporteFrontendIntegracionTest {

    private static final String CODIGOS = "$.contenido[*].codigo";
    private static final String IDS = "$.contenido[*].identificacion";
    private static final AtomicInteger SECUENCIA = new AtomicInteger(100);

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
    }

    // ---------- utilidades ----------

    private String token(String correo, String contrasena) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    private String recepcion() throws Exception { return token("recepcion@sga.local", "Recepcion2026"); }
    private String servicio() throws Exception { return token("servicio@sga.local", "Servicio2026"); }
    private String huesped1() throws Exception { return token("huesped1@sga.local", "Huesped2026"); }
    private String huesped2() throws Exception { return token("huesped2@sga.local", "Huesped2026"); }

    /** Registra a un huésped nuevo (público) y devuelve su token y su documento. */
    private String[] huespedNuevo() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String documento = "99" + n;
        String correo = "nuevo" + n + "@correo.com";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Huesped Nuevo " + n + "\",\"correo\":\"" + correo
                                + "\",\"contrasena\":\"Nuevo2026Clave\",\"telefono\":\"31000000" + n
                                + "\",\"documento\":\"" + documento + "\",\"fechaNacimiento\":\"1990-01-01\"}"))
                .andExpect(status().isCreated());
        return new String[]{token(correo, "Nuevo2026Clave"), documento};
    }

    /** Reserva por el PORTAL a nombre del huésped, de 3 noches desde {@code entrada}, en APT-101. */
    private String reservar(String token, String documento, String entrada, String salida) throws Exception {
        String persona = "{\"documento\":\"" + documento + "\",\"nombre\":\"Huesped " + documento
                + "\",\"fechaNacimiento\":\"1990-01-01\"}";
        String cuerpo = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .content("{\"identificacionApartamento\":\"APT-101\",\"fechaEntrada\":\"" + entrada
                                + "\",\"fechaSalida\":\"" + salida + "\",\"canalOrigen\":\"PORTAL\",\"titular\":"
                                + persona + ",\"ocupantes\":[" + persona + "]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.codigo");
    }

    private void esError(org.springframework.test.web.servlet.ResultActions respuesta, int estado) throws Exception {
        respuesta.andExpect(status().is(estado)).andExpect(jsonPath("$.status").value(estado))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ---------- GET /api/reservas/mias ----------

    @Test
    void mias_unHuespedVeSoloSusReservasYNoLasDeOtro() throws Exception {
        // sembrado: la PENDIENTE del PORTAL es del huésped 1 y la FINALIZADA del PORTAL es del huésped 2
        String delHuesped1 = mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, huesped1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].canalOrigen").value("PORTAL"))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andReturn().getResponse().getContentAsString();
        String delHuesped2 = mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, huesped2()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("FINALIZADA"))
                .andReturn().getResponse().getContentAsString();

        List<String> codigos1 = JsonPath.read(delHuesped1, CODIGOS);
        List<String> codigos2 = JsonPath.read(delHuesped2, CODIGOS);
        assertFalse(codigos1.isEmpty());
        assertFalse(codigos2.isEmpty());
        // ninguna reserva aparece en las dos listas
        assertTrue(codigos1.stream().noneMatch(codigos2::contains), "los huéspedes no deben compartir reservas");

        // coincide exactamente con lo que recepción ve al filtrar por el documento de cada huésped
        mvc.perform(get("/api/reservas").param("titular", "1094000111").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(CODIGOS, containsInAnyOrder(codigos1.toArray())));
        mvc.perform(get("/api/reservas").param("titular", "1094000222").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(CODIGOS, containsInAnyOrder(codigos2.toArray())));
    }

    @Test
    void mias_elFiltroDeEstadoSoloDevuelveLasDeEseEstadoYUnEstadoQueNoTieneDaPaginaVacia() throws Exception {
        mvc.perform(get("/api/reservas/mias").param("estado", "PENDIENTE").header(HttpHeaders.AUTHORIZATION, huesped1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"));
        // el huésped 2 no tiene ninguna PENDIENTE: no aparece la del huésped 1
        mvc.perform(get("/api/reservas/mias").param("estado", "PENDIENTE").header(HttpHeaders.AUTHORIZATION, huesped2()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", empty()))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void mias_unEstadoFueraDelCatalogoResponde400() throws Exception {
        esError(mvc.perform(get("/api/reservas/mias").param("estado", "VOLANDO")
                .header(HttpHeaders.AUTHORIZATION, huesped1())), 400);
        esError(mvc.perform(get("/api/reservas/mias").param("pagina", "-1")
                .header(HttpHeaders.AUTHORIZATION, huesped1())), 400);
    }

    @Test
    void mias_soloLoPuedeUsarUnHuespedLosDemasRolesRecibenElPreciso403YSinTokenEs401() throws Exception {
        esError(mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, recepcion())), 403);
        esError(mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, servicio())), 403);
        esError(mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION,
                token("admin@sga.local", "Admin2026"))), 403);
        esError(mvc.perform(get("/api/reservas/mias")), 401);
    }

    @Test
    void mias_unHuespedRecienRegistradoSinReservasVeUnaPaginaVaciaNoLasDeLosDemas() throws Exception {
        String[] nuevo = huespedNuevo();

        mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, nuevo[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", empty()))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void mias_pagina_de_10_en_10_de_la_mas_reciente_a_la_mas_antigua_y_filtra_por_estado() throws Exception {
        String[] huesped = huespedNuevo();
        List<String> creadas = new ArrayList<>();
        // 11 reservas de 3 noches cada 4 días en febrero y marzo de 2028 (temporada BASE)
        java.time.LocalDate inicio = java.time.LocalDate.of(2028, 2, 1);
        for (int i = 0; i < 11; i++) {
            creadas.add(reservar(huesped[0], huesped[1], inicio.plusDays(4L * i).toString(),
                    inicio.plusDays(4L * i + 3).toString()));
        }

        String primera = mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, huesped[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(10))
                // la más reciente primero: la última que se creó encabeza la lista
                .andExpect(jsonPath("$.contenido[0].codigo").value(creadas.get(10)))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/reservas/mias").param("pagina", "1").header(HttpHeaders.AUTHORIZATION, huesped[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                // la más antigua queda al final: es la primera que se creó
                .andExpect(jsonPath("$.contenido[0].codigo").value(creadas.get(0)));

        // el orden es por creación descendente en toda la página
        List<String> momentos = JsonPath.read(primera, "$.contenido[*].creadaEn");
        for (int i = 1; i < momentos.size(); i++) {
            assertFalse(LocalDateTime.parse(momentos.get(i - 1)).isBefore(LocalDateTime.parse(momentos.get(i))),
                    "creadaEn debe ser descendente: " + momentos);
        }

        // el huésped cancela una (las suyas sí puede) y el filtro de estado la separa
        mvc.perform(put("/api/reservas/" + creadas.get(3) + "/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, huesped[0])
                        .content("{\"motivo\":\"Cambio de planes\",\"autor\":\"Huesped\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reservas/mias").param("estado", "CANCELADA").header(HttpHeaders.AUTHORIZATION, huesped[0]))
                .andExpect(jsonPath(CODIGOS, contains(creadas.get(3))))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/reservas/mias").param("estado", "PENDIENTE").header(HttpHeaders.AUTHORIZATION, huesped[0]))
                .andExpect(jsonPath("$.totalElementos").value(10))
                .andExpect(jsonPath(CODIGOS, not(hasItem(creadas.get(3)))));

        // y ninguna de estas aparece en la lista de otro huésped
        mvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, huesped1()))
                .andExpect(jsonPath(CODIGOS, not(hasItem(creadas.get(10)))));
    }

    // ---------- GET /api/reservas: salidas previstas y nombre del titular ----------

    @Test
    void reservas_nombreTitular_buscaPorTextoContenidoSinImportarMayusculas() throws Exception {
        mvc.perform(get("/api/reservas").param("nombreTitular", "titular CC-1002").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].nombreTitular").value("Titular CC-1002"))
                .andExpect(jsonPath("$.contenido[0].estado").value("CONFIRMADA"));
        // otra mayúscula/minúscula y solo un fragmento
        mvc.perform(get("/api/reservas").param("nombreTitular", "ULAR cc-100").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(4)); // CC-1002, 1003, 1005 y 1006
        mvc.perform(get("/api/reservas").param("nombreTitular", "nadie se llama asi").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
    }

    @Test
    void reservas_nombreTitular_seCombinaConLosDemasFiltrosYLosComodinesSeTomanLiteralmente() throws Exception {
        mvc.perform(get("/api/reservas").param("nombreTitular", "titular").param("estado", "CANCELADA")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].nombreTitular").value("Titular CC-1005"));
        // un % o un _ escrito por el usuario no es un comodín: ningún nombre los contiene
        mvc.perform(get("/api/reservas").param("nombreTitular", "%").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
        mvc.perform(get("/api/reservas").param("nombreTitular", "_").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
    }

    @Test
    void reservas_elFiltroTitularSigueSiendoElDocumentoExactoYNoElNombre() throws Exception {
        // por documento: encuentra la reserva
        mvc.perform(get("/api/reservas").param("titular", "CC-1002").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].nombreTitular").value("Titular CC-1002"));
        // un nombre, o un fragmento del documento, no es un documento: no encuentra nada
        mvc.perform(get("/api/reservas").param("titular", "CC-100").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
        mvc.perform(get("/api/reservas").param("titular", "Laura Gomez").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
        // y por nombre sí: ahí está la diferencia
        mvc.perform(get("/api/reservas").param("nombreTitular", "Laura").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk());
    }

    @Test
    void reservas_llegadasYSalidasPrevistasDeUnDia() throws Exception {
        // la reserva CONFIRMADA de CC-1002 es la de los datos sembrados "dentro de 10 días"
        String cuerpo = mvc.perform(get("/api/reservas").param("nombreTitular", "CC-1002")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(cuerpo, "$.contenido[0].codigo");
        String entrada = JsonPath.read(cuerpo, "$.contenido[0].fechaEntrada");
        String salida = JsonPath.read(cuerpo, "$.contenido[0].fechaSalida");
        java.time.LocalDate diaDeSalida = java.time.LocalDate.parse(salida);

        // llegadas del día: entradaDesde = entradaHasta
        mvc.perform(get("/api/reservas").param("entradaDesde", entrada).param("entradaHasta", entrada)
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(CODIGOS, hasItem(codigo)));
        // salidas del día: salidaDesde = salidaHasta
        mvc.perform(get("/api/reservas").param("salidaDesde", salida).param("salidaHasta", salida)
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODIGOS, hasItem(codigo)))
                .andExpect(jsonPath("$.contenido[*].fechaSalida", not(hasItem(diaDeSalida.minusDays(1).toString()))))
                .andExpect(jsonPath("$.contenido[*].fechaSalida", not(hasItem(diaDeSalida.plusDays(1).toString()))));
        // el día anterior y el posterior no la incluyen (extremos inclusivos, pero solo ese día)
        mvc.perform(get("/api/reservas").param("salidaDesde", diaDeSalida.plusDays(1).toString())
                        .param("nombreTitular", "CC-1002").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
        mvc.perform(get("/api/reservas").param("salidaHasta", diaDeSalida.minusDays(1).toString())
                        .param("nombreTitular", "CC-1002").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath("$.contenido", empty()));
        // un rango que la contiene y la fecha mal escrita
        mvc.perform(get("/api/reservas").param("salidaDesde", diaDeSalida.minusDays(2).toString())
                        .param("salidaHasta", diaDeSalida.plusDays(2).toString())
                        .param("nombreTitular", "CC-1002").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(CODIGOS, contains(codigo)));
        esError(mvc.perform(get("/api/reservas").param("salidaDesde", "ayer")
                .header(HttpHeaders.AUTHORIZATION, recepcion())), 400);
    }

    @Test
    void reservas_losFiltrosNuevosSiguenReservadosARecepcionYAdministrador() throws Exception {
        esError(mvc.perform(get("/api/reservas").param("nombreTitular", "titular")
                .header(HttpHeaders.AUTHORIZATION, huesped1())), 403);
        esError(mvc.perform(get("/api/reservas").param("salidaDesde", "2028-01-01")
                .header(HttpHeaders.AUTHORIZATION, servicio())), 403);
        esError(mvc.perform(get("/api/reservas").param("nombreTitular", "titular")), 401);
    }

    // ---------- GET /api/apartamentos?estadoOperativo= ----------

    @Test
    void apartamentos_estadoOperativo_filtraPorLaCondicionFisicaParaElPersonalDeServicio() throws Exception {
        mvc.perform(get("/api/apartamentos").param("estadoOperativo", "FUERA_DE_SERVICIO")
                        .header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, contains("APT-302")))
                .andExpect(jsonPath("$.contenido[0].estadoOperativo").value("FUERA_DE_SERVICIO"));
        mvc.perform(get("/api/apartamentos").param("estadoOperativo", "OCUPADO")
                        .header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(jsonPath(IDS, contains("APT-203")));
        mvc.perform(get("/api/apartamentos").param("estadoOperativo", "PREPARADO")
                        .header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-101", "APT-102", "APT-201", "APT-202", "APT-301")));
        // ninguno está pendiente de preparación ni en preparación en el sembrado
        mvc.perform(get("/api/apartamentos").param("estadoOperativo", "PENDIENTE_PREPARACION")
                        .header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(jsonPath("$.contenido", empty()))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void apartamentos_estadoOperativo_seCombinaConLasCaracteristicas() throws Exception {
        // BALCON lo tienen 101, 201 y 302; de ellos, el 302 está fuera de servicio
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "BALCON").param("estadoOperativo", "PREPARADO")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(IDS, containsInAnyOrder("APT-101", "APT-201")));
        mvc.perform(get("/api/apartamentos").param("caracteristicas", "BALCON")
                        .param("estadoOperativo", "FUERA_DE_SERVICIO").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(jsonPath(IDS, contains("APT-302")));
        // sin filtro de estado siguen apareciendo todos
        mvc.perform(get("/api/apartamentos").header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(jsonPath("$.totalElementos").value(7));
    }

    @Test
    void apartamentos_estadoOperativo_validaElValorYExigeToken() throws Exception {
        esError(mvc.perform(get("/api/apartamentos").param("estadoOperativo", "DESCOMPUESTO")
                .header(HttpHeaders.AUTHORIZATION, servicio())), 400);
        esError(mvc.perform(get("/api/apartamentos").param("estadoOperativo", "OCUPADO")), 401);
    }
}
