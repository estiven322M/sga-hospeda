package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

/**
 * J6 de extremo a extremo, con login real y comprobando el CUERPO de cada respuesta: los datos del alojamiento (L-01, L-02,
 * L-16), los servicios adicionales y su cargo en el folio (L-17), los medios de pago (L-18), el calendario de ocupación
 * (7.1, 7.5) y el aviso de capacidad (7.3). Cada endpoint se prueba también con el rol que no corresponde (403) y sin token
 * (401). Tiene su propia base H2 porque escribe.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:alojamientoservicios")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AlojamientoServiciosIntegracionTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
    }

    // ---------- utilidades ----------

    private String token(String correo, String contrasena) throws Exception {
        String cuerpo = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    private String admin() throws Exception { return token("admin@sga.local", "Admin2026"); }
    private String recepcion() throws Exception { return token("recepcion@sga.local", "Recepcion2026"); }
    private String servicio() throws Exception { return token("servicio@sga.local", "Servicio2026"); }
    private String huesped() throws Exception { return token("huesped1@sga.local", "Huesped2026"); }

    private static HttpHeaders con(String token) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.set(HttpHeaders.AUTHORIZATION, token);
        return cabeceras;
    }

    /** Toda respuesta 409 cumple la aserción común: status 409, mensaje de la regla, errores nulo y sin trazas. */
    private static void conflicto(ResultActions respuesta, String mensaje) throws Exception {
        respuesta.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value(mensaje))
                .andExpect(jsonPath("$.errores").value(nullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private ResultActions put(String ruta, String token, String cuerpo) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(ruta).headers(con(token)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions post(String ruta, String token, String cuerpo) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(ruta).headers(con(token)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static final String PERSONA = "{\"documento\":\"%s\",\"nombre\":\"Cliente %s\",\"fechaNacimiento\":\"1990-01-01\"}";

    /** Crea una reserva DIRECTO (recepción) de {@code ocupantes} adultos y devuelve su código. */
    private String crearReserva(String apartamento, String entrada, String salida, int ocupantes, String documentoBase)
            throws Exception {
        StringBuilder grupo = new StringBuilder();
        for (int i = 0; i < ocupantes; i++) {
            if (i > 0) grupo.append(',');
            grupo.append(String.format(PERSONA, documentoBase + "-" + i, documentoBase + "-" + i));
        }
        String titular = String.format(PERSONA, documentoBase + "-0", documentoBase + "-0");
        String cuerpo = post("/api/reservas", recepcion(), "{\"identificacionApartamento\":\"" + apartamento
                + "\",\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + salida
                + "\",\"canalOrigen\":\"DIRECTO\",\"titular\":" + titular + ",\"ocupantes\":[" + grupo + "]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.codigo");
    }

    private void pagar(String codigo, String medio, long valor) throws Exception {
        post("/api/reservas/" + codigo + "/folio/pagos", recepcion(),
                "{\"medio\":\"" + medio + "\",\"fecha\":\"" + LocalDate.now() + "\",\"valor\":" + valor + "}")
                .andExpect(status().isCreated());
    }

    private long saldo(String codigo) throws Exception {
        String folio = mvc.perform(get("/api/reservas/" + codigo + "/folio").headers(con(recepcion())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(folio, "$.saldo")).longValue();
    }

    /** Crea una reserva, paga su anticipo (30 %) y la confirma: queda CONFIRMADA y con el folio abierto. */
    private String reservaConfirmada(String apartamento, String entrada, String salida, int ocupantes, String documentoBase)
            throws Exception {
        String codigo = crearReserva(apartamento, entrada, salida, ocupantes, documentoBase);
        long valor = saldo(codigo);
        pagar(codigo, "EFECTIVO", Math.round(valor * 0.30));
        put("/api/reservas/" + codigo + "/confirmar", recepcion(), "{\"horaEstimadaLlegada\":\"16:00\",\"autor\":\"Recepción\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        return codigo;
    }

    // ================= ALOJAMIENTO (L-01, L-02, L-16) =================

    @Test
    void alojamiento_laConsultaEsPublicaYTraeLosDatosSembradosDeApplicationProperties() throws Exception {
        mvc.perform(get("/api/alojamiento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hospeda Salento"))
                .andExpect(jsonPath("$.ciudad").value("Salento, Quindío"))
                .andExpect(jsonPath("$.direccion").value("Carrera 6 # 3-45, Salento, Quindío"))
                .andExpect(jsonPath("$.latitud").value(4.6368))
                .andExpect(jsonPath("$.longitud").value(-75.57))
                .andExpect(jsonPath("$.horaEntrada").value("15:00:00"))
                .andExpect(jsonPath("$.horaSalida").value("11:00:00"))
                .andExpect(jsonPath("$.normasConvivencia").value(containsString("Silencio entre las 10 p. m.")))
                .andExpect(jsonPath("$.politicaMascotas").value("No se aceptan mascotas.")) // L-16
                .andExpect(jsonPath("$.mediosPago", containsInAnyOrder("EFECTIVO", "TARJETA", "TRANSFERENCIA"))); // L-18
    }

    @Test
    void alojamiento_losDatosSeEditanSoloConElRolAdministradorYElDominioValida() throws Exception {
        String edicion = "{\"nombre\":\"Hospeda Salento\",\"descripcion\":\"Apartamentos con vista al valle\","
                + "\"ciudad\":\"Salento, Quindío\",\"direccion\":\"Carrera 6 # 3-45\",\"latitud\":4.6368,\"longitud\":-75.57,"
                + "\"horaEntrada\":\"14:00\",\"horaSalida\":\"10:00\",\"normasConvivencia\":\"Silencio desde las 9 p. m.\","
                + "\"politicaMascotas\":\"Se aceptan mascotas pequeñas con un cargo de limpieza.\"}";

        // rol que no corresponde y sin token
        mvc.perform(MockMvcRequestBuilders.put("/api/alojamiento").headers(con(recepcion())).contentType(MediaType.APPLICATION_JSON).content(edicion))
                .andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.put("/api/alojamiento").contentType(MediaType.APPLICATION_JSON).content(edicion))
                .andExpect(status().isUnauthorized());

        // el administrador edita y la consulta pública lo refleja
        put("/api/alojamiento", admin(), edicion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaEntrada").value("14:00:00"))
                .andExpect(jsonPath("$.politicaMascotas").value("Se aceptan mascotas pequeñas con un cargo de limpieza."));
        mvc.perform(get("/api/alojamiento"))
                .andExpect(jsonPath("$.horaSalida").value("10:00:00"))
                .andExpect(jsonPath("$.normasConvivencia").value("Silencio desde las 9 p. m."));

        // 409: la salida debe ser anterior a la entrada; y no se modificó nada
        conflicto(put("/api/alojamiento", admin(), edicion.replace("\"horaEntrada\":\"14:00\"", "\"horaEntrada\":\"09:00\"")),
                "La hora de salida debe ser anterior a la hora de entrada");
        mvc.perform(get("/api/alojamiento")).andExpect(jsonPath("$.horaEntrada").value("14:00:00"));

        // 400: la forma (latitud fuera de rango, política de mascotas vacía)
        put("/api/alojamiento", admin(), edicion.replace("\"latitud\":4.6368", "\"latitud\":91"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.latitud").exists());
        put("/api/alojamiento", admin(), edicion.replace("Se aceptan mascotas pequeñas con un cargo de limpieza.", ""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.politicaMascotas").exists());

        // se deja como estaba para las demás pruebas
        put("/api/alojamiento", admin(), edicion.replace("14:00", "15:00").replace("10:00", "11:00")
                .replace("Se aceptan mascotas pequeñas con un cargo de limpieza.", "No se aceptan mascotas.")
                .replace("Silencio desde las 9 p. m.", "Silencio entre las 10 p. m. y las 7 a. m."))
                .andExpect(status().isOk());
    }

    @Test
    void alojamiento_losParametrosSonSoloLecturaYSoloDelAdministrador() throws Exception {
        mvc.perform(get("/api/alojamiento/parametros").headers(con(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralEdadFacturableAnios").value(12))
                .andExpect(jsonPath("$.tiempoPreparacionHoras").value(3))
                .andExpect(jsonPath("$.plazoConfirmacionHoras").value(24))
                .andExpect(jsonPath("$.anticipoPorcentaje").value(30))
                .andExpect(jsonPath("$.horaLimiteNoShow").value("22:00:00"))
                .andExpect(jsonPath("$.estanciaMinimaPorTemporada.ALTA").value(3))
                .andExpect(jsonPath("$.nochesDescuentoEstanciaLarga").value(7))
                .andExpect(jsonPath("$.edadAdultoResponsableAnios").value(18))
                .andExpect(jsonPath("$.calendarioMaximoNoches").value(62))
                .andExpect(jsonPath("$.mediosPago", hasSize(3)))
                .andExpect(jsonPath("$.nota").value(containsString("configuración del sistema")));
        mvc.perform(get("/api/alojamiento/parametros").headers(con(recepcion()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/alojamiento/parametros").headers(con(huesped()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/alojamiento/parametros")).andExpect(status().isUnauthorized());
    }

    // ================= SERVICIOS ADICIONALES (L-17) =================

    @Test
    void servicios_vienenAlMenosTresSembradosYRecepcionLosVeSinPoderCrearlos() throws Exception {
        mvc.perform(get("/api/servicios-adicionales").headers(con(recepcion())).param("soloActivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.contenido[*].codigo", hasItem("DESAYUNO")))
                .andExpect(jsonPath("$.contenido[?(@.codigo=='DESAYUNO')].valor").value(25000))
                .andExpect(jsonPath("$.contenido[?(@.codigo=='DESAYUNO')].moneda").value("COP"))
                .andExpect(jsonPath("$.contenido[?(@.codigo=='BIENVENIDA')].generaCargo").value(false));
        post("/api/servicios-adicionales", recepcion(),
                "{\"codigo\":\"X\",\"nombre\":\"X\",\"generaCargo\":true,\"valor\":1}").andExpect(status().isForbidden());
        mvc.perform(get("/api/servicios-adicionales").headers(con(huesped()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/servicios-adicionales").headers(con(servicio()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/servicios-adicionales")).andExpect(status().isUnauthorized());
    }

    @Test
    void servicios_elAdministradorLosGestionaConBajaLogicaYLasReglasDelDominio() throws Exception {
        // crear: 201 con Location y nace activo
        post("/api/servicios-adicionales", admin(),
                "{\"codigo\":\"spa\",\"nombre\":\"Masaje\",\"descripcion\":\"Una hora\",\"generaCargo\":true,\"valor\":80000}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/servicios-adicionales/SPA")))
                .andExpect(jsonPath("$.codigo").value("SPA"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.valor").value(80000));

        // 409: código repetido, y la coherencia entre "genera cargo" y el valor
        conflicto(post("/api/servicios-adicionales", admin(),
                "{\"codigo\":\"SPA\",\"nombre\":\"Otro\",\"generaCargo\":true,\"valor\":1}"),
                "Ya existe un servicio adicional con el código SPA");
        conflicto(post("/api/servicios-adicionales", admin(),
                "{\"codigo\":\"GRATIS\",\"nombre\":\"Gratis\",\"generaCargo\":true,\"valor\":0}"),
                "Un servicio adicional que genera cargo debe tener un valor mayor que cero");
        conflicto(post("/api/servicios-adicionales", admin(),
                "{\"codigo\":\"CORT\",\"nombre\":\"Cortesía\",\"generaCargo\":false,\"valor\":500}"),
                "Un servicio adicional que no genera cargo no puede tener valor");
        // 400: forma
        post("/api/servicios-adicionales", admin(), "{\"codigo\":\"\",\"nombre\":\"X\",\"generaCargo\":true,\"valor\":-5}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.codigo").exists())
                .andExpect(jsonPath("$.errores.valor").exists());

        // editar
        put("/api/servicios-adicionales/SPA", admin(),
                "{\"nombre\":\"Masaje relajante\",\"descripcion\":\"Una hora\",\"generaCargo\":true,\"valor\":90000}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Masaje relajante"))
                .andExpect(jsonPath("$.valor").value(90000));
        put("/api/servicios-adicionales/NO-EXISTE", admin(), "{\"nombre\":\"X\",\"generaCargo\":true,\"valor\":1}")
                .andExpect(status().isNotFound());

        // baja lógica: no se borra, deja de estar entre los activos y no se puede repetir
        put("/api/servicios-adicionales/SPA/baja", admin(), "{}").andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        conflicto(put("/api/servicios-adicionales/SPA/baja", admin(), "{}"), "El servicio adicional SPA ya está dado de baja");
        mvc.perform(get("/api/servicios-adicionales").headers(con(admin())).param("soloActivos", "true"))
                .andExpect(status().isOk()); // el listado es de recepción y de administración
        mvc.perform(get("/api/servicios-adicionales").headers(con(recepcion())).param("soloActivos", "true"))
                .andExpect(jsonPath("$.contenido[*].codigo", org.hamcrest.Matchers.not(hasItem("SPA"))));
        mvc.perform(get("/api/servicios-adicionales").headers(con(recepcion())))
                .andExpect(jsonPath("$.contenido[*].codigo", hasItem("SPA"))); // sigue existiendo
        // volver a dar de alta
        put("/api/servicios-adicionales/SPA/activar", admin(), "{}").andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
        conflicto(put("/api/servicios-adicionales/SPA/activar", admin(), "{}"), "El servicio adicional SPA ya está activo");
        put("/api/servicios-adicionales/SPA/baja", recepcion(), "{}").andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.put("/api/servicios-adicionales/SPA/baja").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ================= CARGO DE SERVICIO ADICIONAL EN EL FOLIO (L-17, 7.7) =================

    @Test
    void cargo_seRegistraEnUnaReservaConfirmadaConElValorPorLaCantidad() throws Exception {
        String codigo = reservaConfirmada("APT-203", "2028-02-01", "2028-02-04", 2, "7001");
        long antes = saldo(codigo);

        post("/api/reservas/" + codigo + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"desayuno\",\"cantidad\":2}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/reservas/" + codigo + "/folio")))
                .andExpect(jsonPath("$.cargos[*].concepto", hasItem("Servicio adicional: Desayuno típico x2")))
                .andExpect(jsonPath("$.cargos[?(@.concepto=='Servicio adicional: Desayuno típico x2')].valor").value(50000))
                .andExpect(jsonPath("$.saldo").value(antes + 50_000))
                .andExpect(jsonPath("$.cerrado").value(false));
        // lo que se cargó queda en el folio y no se edita: consultarlo de nuevo lo confirma
        mvc.perform(get("/api/reservas/" + codigo + "/folio").headers(con(recepcion())))
                .andExpect(jsonPath("$.cargos", hasSize(2)));
    }

    @Test
    void cargo_cadaReglaDelDominioResponde409ConSuMensaje() throws Exception {
        String pendiente = crearReserva("APT-203", "2028-03-01", "2028-03-03", 1, "7002");
        String confirmada = reservaConfirmada("APT-203", "2028-02-10", "2028-02-12", 1, "7003");

        // la reserva debe estar CONFIRMADA o EN_CURSO
        conflicto(post("/api/reservas/" + pendiente + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":1}"),
                "Solo se registran servicios adicionales en reservas CONFIRMADA o EN_CURSO; la reserva " + pendiente + " está PENDIENTE");
        // el servicio debe generar cargo
        conflicto(post("/api/reservas/" + confirmada + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"BIENVENIDA\",\"cantidad\":1}"),
                "El servicio adicional BIENVENIDA no genera cargo");
        // el servicio debe estar activo
        post("/api/servicios-adicionales", admin(), "{\"codigo\":\"TEMP\",\"nombre\":\"Temporal\",\"generaCargo\":true,\"valor\":10000}")
                .andExpect(status().isCreated());
        put("/api/servicios-adicionales/TEMP/baja", admin(), "{}").andExpect(status().isOk());
        conflicto(post("/api/reservas/" + confirmada + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"TEMP\",\"cantidad\":1}"),
                "El servicio adicional TEMP está dado de baja");
        // el folio debe estar abierto: se paga todo y se cierra
        pagar(confirmada, "TRANSFERENCIA", saldo(confirmada));
        put("/api/reservas/" + confirmada + "/folio/cerrar", recepcion(), "{}").andExpect(status().isOk()).andExpect(jsonPath("$.cerrado").value(true));
        conflicto(post("/api/reservas/" + confirmada + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":1}"),
                "Un folio cerrado no admite movimientos");
    }

    @Test
    void cargo_formaPermisosYRecursosQueNoExisten() throws Exception {
        String codigo = reservaConfirmada("APT-203", "2028-02-20", "2028-02-22", 1, "7004");

        post("/api/reservas/" + codigo + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":0}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.cantidad").exists());
        post("/api/reservas/" + codigo + "/folio/cargos", recepcion(), "{\"cantidad\":1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.codigoServicio").exists());
        post("/api/reservas/" + codigo + "/folio/cargos", recepcion(), "{\"codigoServicio\":\"NO-EXISTE\",\"cantidad\":1}")
                .andExpect(status().isNotFound());
        post("/api/reservas/RES-2000-00001/folio/cargos", recepcion(), "{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":1}")
                .andExpect(status().isNotFound());
        // administración sí puede; huésped y personal de servicio no; sin token tampoco
        post("/api/reservas/" + codigo + "/folio/cargos", admin(), "{\"codigoServicio\":\"LAVANDERIA\",\"cantidad\":1}")
                .andExpect(status().isCreated());
        post("/api/reservas/" + codigo + "/folio/cargos", huesped(), "{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":1}")
                .andExpect(status().isForbidden());
        post("/api/reservas/" + codigo + "/folio/cargos", servicio(), "{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":1}")
                .andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.post("/api/reservas/" + codigo + "/folio/cargos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoServicio\":\"DESAYUNO\",\"cantidad\":1}")).andExpect(status().isUnauthorized());
    }

    // ================= MEDIOS DE PAGO (L-18) =================

    @Test
    void pago_unMedioNoAceptadoSeRechazaConLaListaDeLosAceptados() throws Exception {
        String codigo = crearReserva("APT-203", "2028-04-10", "2028-04-12", 1, "7005");

        conflicto(post("/api/reservas/" + codigo + "/folio/pagos", recepcion(),
                        "{\"medio\":\"CRIPTOMONEDA\",\"fecha\":\"" + LocalDate.now() + "\",\"valor\":50000}"),
                "El medio de pago CRIPTOMONEDA no es aceptado; los medios aceptados son: EFECTIVO, TARJETA, TRANSFERENCIA");
        mvc.perform(get("/api/reservas/" + codigo + "/folio").headers(con(recepcion())))
                .andExpect(jsonPath("$.pagos", hasSize(0))); // no se registró nada

        // los medios aceptados funcionan, sin distinguir mayúsculas
        post("/api/reservas/" + codigo + "/folio/pagos", recepcion(),
                "{\"medio\":\"tarjeta\",\"fecha\":\"" + LocalDate.now() + "\",\"valor\":50000}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.pagos[0].medio").value("tarjeta"));
    }

    // ================= CALENDARIO (7.1, 7.5) =================

    @Test
    void calendario_recepcionVeElDetalleYElHuespedSoloLibreONoDisponible() throws Exception {
        String codigo = reservaConfirmada("APT-203", "2028-05-10", "2028-05-12", 1, "7006");
        String ruta = "/api/apartamentos/APT-203/calendario";

        // recepción: estado real, código y estado de la reserva; la noche de la salida (12) queda libre
        mvc.perform(get(ruta).headers(con(recepcion())).param("desde", "2028-05-09").param("hasta", "2028-05-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-203"))
                .andExpect(jsonPath("$.noches", hasSize(4)))
                .andExpect(jsonPath("$.noches[0].estado").value("LIBRE"))
                .andExpect(jsonPath("$.noches[0].codigoReserva").value(nullValue()))
                .andExpect(jsonPath("$.noches[1].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.noches[1].codigoReserva").value(codigo))
                .andExpect(jsonPath("$.noches[1].estadoReserva").value("CONFIRMADA"))
                .andExpect(jsonPath("$.noches[2].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.noches[3].estado").value("LIBRE"));
        // el administrador ve lo mismo que recepción
        mvc.perform(get(ruta).headers(con(admin())).param("desde", "2028-05-10").param("hasta", "2028-05-11"))
                .andExpect(jsonPath("$.noches[0].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.noches[0].codigoReserva").value(codigo));
        // el huésped solo ve libre o no disponible, sin código ni estado de la reserva
        mvc.perform(get(ruta).headers(con(huesped())).param("desde", "2028-05-09").param("hasta", "2028-05-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches[0].estado").value("LIBRE"))
                .andExpect(jsonPath("$.noches[1].estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.noches[1].codigoReserva").value(nullValue()))
                .andExpect(jsonPath("$.noches[1].estadoReserva").value(nullValue()))
                .andExpect(jsonPath("$.noches[3].estado").value("LIBRE"));
    }

    @Test
    void calendario_unBloqueoApareceComoBloqueadaParaRecepcionYNoDisponibleParaElHuesped() throws Exception {
        put("/api/apartamentos/APT-202/estado-operativo/declarar-fuera-de-servicio", admin(), "{}"); // no influye en el calendario
        post("/api/apartamentos/APT-202/bloqueos", admin(),
                "{\"fechaInicio\":\"2028-06-10\",\"fechaFin\":\"2028-06-12\",\"motivo\":\"Pintura\"}").andExpect(status().isCreated());

        mvc.perform(get("/api/apartamentos/APT-202/calendario").headers(con(recepcion())).param("desde", "2028-06-10").param("hasta", "2028-06-13"))
                .andExpect(jsonPath("$.noches[0].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.noches[1].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.noches[2].estado").value("LIBRE"));
        mvc.perform(get("/api/apartamentos/APT-202/calendario").headers(con(huesped())).param("desde", "2028-06-10").param("hasta", "2028-06-13"))
                .andExpect(jsonPath("$.noches[0].estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.noches[2].estado").value("LIBRE"));
    }

    @Test
    void calendario_elRangoAdmiteHasta62NochesYUnaMasEs409() throws Exception {
        String ruta = "/api/apartamentos/APT-101/calendario";

        mvc.perform(get(ruta).headers(con(recepcion())).param("desde", "2028-07-01").param("hasta", "2028-09-01")) // 62 noches
                .andExpect(status().isOk()).andExpect(jsonPath("$.noches", hasSize(62)));
        conflicto(mvc.perform(get(ruta).headers(con(recepcion())).param("desde", "2028-07-01").param("hasta", "2028-09-02")),
                "El calendario admite como máximo 62 noches; se pidieron 63");
        conflicto(mvc.perform(get(ruta).headers(con(recepcion())).param("desde", "2028-07-05").param("hasta", "2028-07-05")),
                "La fecha de salida debe ser posterior a la fecha de entrada");
    }

    @Test
    void calendario_formaPermisosYApartamentoInexistente() throws Exception {
        String ruta = "/api/apartamentos/APT-101/calendario";

        mvc.perform(get(ruta).headers(con(recepcion())).param("desde", "2028-07-01")).andExpect(status().isBadRequest());
        mvc.perform(get(ruta).headers(con(recepcion())).param("desde", "ayer").param("hasta", "2028-07-05")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/apartamentos/APT-999/calendario").headers(con(recepcion())).param("desde", "2028-07-01").param("hasta", "2028-07-05"))
                .andExpect(status().isNotFound());
        // el personal de servicio no usa el calendario; sin token tampoco
        mvc.perform(get(ruta).headers(con(servicio())).param("desde", "2028-07-01").param("hasta", "2028-07-05")).andExpect(status().isForbidden());
        mvc.perform(get(ruta).param("desde", "2028-07-01").param("hasta", "2028-07-05")).andExpect(status().isUnauthorized());
    }

    // ================= AVISO DE CAPACIDAD (7.3) =================

    @Test
    void capacidad_alBajarlaLaRespuestaListaLasReservasQueQuedanPorEncimaSinBloquear() throws Exception {
        String cuerpo = mvc.perform(get("/api/apartamentos/APT-202").headers(con(admin()))).andReturn().getResponse().getContentAsString();
        String nombre = JsonPath.read(cuerpo, "$.nombre");
        String edicion = "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Apartamento amplio para familias\",\"dormitorios\":3,"
                + "\"capacidad\":%d,\"dotacion\":[\"Cocina equipada\"],\"caracteristicas\":[\"PARQUEADERO\",\"ACCESIBLE\"]}";
        String de4 = crearReserva("APT-202", "2028-08-10", "2028-08-12", 4, "7007");
        String de2 = crearReserva("APT-202", "2028-08-20", "2028-08-22", 2, "7008");

        // bajar de 8 a 3: la reserva de 4 personas queda por encima, la de 2 no; la actualización se hace igual
        put("/api/apartamentos/APT-202", admin(), String.format(edicion, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidad").value(3))
                .andExpect(jsonPath("$.reservasPorEncimaDeLaCapacidad", hasSize(1)))
                .andExpect(jsonPath("$.reservasPorEncimaDeLaCapacidad[0].codigo").value(de4))
                .andExpect(jsonPath("$.reservasPorEncimaDeLaCapacidad[0].totalOcupantes").value(4))
                .andExpect(jsonPath("$.reservasPorEncimaDeLaCapacidad[0].estado").value("PENDIENTE"));
        // la reserva no se modificó: conserva su grupo de 4
        mvc.perform(get("/api/reservas/" + de4).headers(con(recepcion())))
                .andExpect(jsonPath("$.ocupantes", hasSize(4)));
        mvc.perform(get("/api/reservas/" + de2).headers(con(recepcion()))).andExpect(status().isOk());

        // subir la capacidad no avisa de nada
        put("/api/apartamentos/APT-202", admin(), String.format(edicion, 8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reservasPorEncimaDeLaCapacidad", hasSize(0)));
    }
}
