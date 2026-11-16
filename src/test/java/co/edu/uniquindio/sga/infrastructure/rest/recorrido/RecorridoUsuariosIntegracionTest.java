package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Gestión de usuarios (7.2) por HTTP con login real: el administrador lista, crea usuarios internos, cambia roles y
 * desactiva (lógico, y nunca a un titular con reservas activas); cada usuario ve y edita su perfil y cambia su contraseña;
 * y la recuperación por código (6 dígitos, 15 minutos, un solo uso, respuesta genérica). Con el proveedor local el código
 * se lee en la bandeja de notificaciones. Cada endpoint se prueba también con el rol que no corresponde y sin token.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridousuarios",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoUsuariosIntegracionTest extends RecorridoBase {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(100);
    private static final String CLAVE = "Interno2026";
    private static final String MENSAJE_GENERICO = "Si el correo está registrado, enviamos un código de recuperación que vence en 15 minutos";
    private static final String CODIGO_RECHAZADO = "El código no es válido o ya venció";

    // ---------- utilidades ----------

    private static String cuerpoUsuario(String correo, String documento, String rol) {
        return "{\"nombre\":\"Usuario " + documento + "\",\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE
                + "\",\"telefono\":\"3001234567\",\"documento\":\"" + documento + "\",\"fechaNacimiento\":\"1991-01-01\",\"rol\":\"" + rol + "\"}";
    }

    /** Crea un usuario interno y devuelve su JSON; el correo es "usuarioN@correo.com" y la clave, {@link #CLAVE}. */
    private String crearInterno(String rol) throws Exception {
        int n = SECUENCIA.incrementAndGet();
        return cuerpo(crear("/api/usuarios", admin(), cuerpoUsuario("usuario" + n + "@correo.com", "55" + n, rol))
                .andExpect(status().isCreated()));
    }

    private static void sinSecretos(String respuesta) {
        String enMinusculas = respuesta.toLowerCase();
        assertFalse(enMinusculas.contains("contrasena"), "nunca se devuelve la contraseña: " + respuesta);
        assertFalse(enMinusculas.contains("bcrypt") || respuesta.contains("$2a$"), "nunca se devuelve un hash: " + respuesta);
    }

    /** El código de recuperación más reciente que se "envió" a ese correo, leído de la bandeja (proveedor local). */
    private String codigoEnviadoA(String correo) throws Exception {
        String pagina = cuerpo(consultar("/api/notificaciones", admin()).andExpect(status().isOk()));
        assertEquals("CODIGO_RECUPERACION", texto(pagina, "$.contenido[0].tipo"));
        assertEquals(correo, texto(pagina, "$.contenido[0].destinatario"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("es (\\d{6})\\.").matcher(texto(pagina, "$.contenido[0].mensaje"));
        assertTrue(m.find(), "el mensaje lleva el código de 6 dígitos");
        return m.group(1);
    }

    private static String confirmacion(String correo, String codigo, String clave) {
        return "{\"correo\":\"" + correo + "\",\"codigo\":\"" + codigo + "\",\"contrasenaNueva\":\"" + clave + "\"}";
    }

    private void solicitar(String correo) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
    }

    private org.springframework.test.web.servlet.ResultActions confirmar(String correo, String codigo, String clave) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON)
                .content(confirmacion(correo, codigo, clave)));
    }

    private int loginConClave(String correo, String clave) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + clave + "\"}")).andReturn().getResponse().getStatus();
    }

    /** Recorre el listado de 10 en 10 hasta encontrar el correo y devuelve su campo "activo". */
    private Boolean activoSegunElListado(String correo) throws Exception {
        for (int pagina = 0; pagina < 50; pagina++) {
            String contenido = cuerpo(consultar("/api/usuarios?pagina=" + pagina, admin()).andExpect(status().isOk()));
            java.util.List<Boolean> encontrados = com.jayway.jsonpath.JsonPath.read(contenido, "$.contenido[?(@.correo=='" + correo + "')].activo");
            if (!encontrados.isEmpty()) return encontrados.get(0);
            if (pagina + 1 >= numero(contenido, "$.totalPaginas")) break;
        }
        return null;
    }

    // ---------- administración: listar ----------

    @Test
    void elAdministradorListaLosUsuariosDeDiezEnDiezSinSecretosYFiltraPorRol() throws Exception {
        for (int i = 0; i < 7; i++) {
            crearInterno("PERSONAL_SERVICIO");
        }

        String pagina = cuerpo(consultar("/api/usuarios", admin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10)).andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.contenido", hasSize(10))));
        sinSecretos(pagina);
        assertTrue(numero(pagina, "$.totalElementos") >= 12);
        assertTrue(numero(pagina, "$.totalPaginas") >= 2);
        consultar("/api/usuarios?pagina=1", admin()).andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1));

        consultar("/api/usuarios?rol=ADMINISTRADOR", admin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].rol", everyItem(is("ADMINISTRADOR"))))
                .andExpect(jsonPath("$.contenido[?(@.correo=='admin@sga.local')]", hasSize(1)));
        consultar("/api/usuarios?rol=HUESPED", admin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].rol", everyItem(is("HUESPED"))));
        consultar("/api/usuarios?rol=DUENO", admin()).andExpect(status().isBadRequest());
        consultar("/api/usuarios?pagina=-1", admin()).andExpect(status().isBadRequest());
    }

    @Test
    void soloElAdministradorListaCreaCambiaRolYDesactiva() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        for (String token : new String[] {recepcion(), servicio(), huespedNuevo()[0]}) {
            consultar("/api/usuarios", token).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            crear("/api/usuarios", token, cuerpoUsuario("x@correo.com", "1", "RECEPCIONISTA")).andExpect(status().isForbidden());
            actualizar("/api/usuarios/1/rol", token, "{\"rol\":\"HUESPED\"}").andExpect(status().isForbidden());
            actualizar("/api/usuarios/1/desactivar", token).andExpect(status().isForbidden());
        }
    }

    // ---------- administración: crear interno ----------

    @Test
    void elAdministradorCreaUnUsuarioInternoConSuRolYEsteInicaSesion() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String correo = "interno" + n + "@correo.com";

        var respuesta = crear("/api/usuarios", admin(), cuerpoUsuario("Interno" + n + "@Correo.com", "66" + n, "RECEPCIONISTA"))
                .andExpect(status().isCreated());
        String creado = cuerpo(respuesta);
        assertTrue(respuesta.andReturn().getResponse().getHeader("Location").endsWith("/api/usuarios/" + numero(creado, "$.id")));
        sinSecretos(creado);
        assertEquals(correo, texto(creado, "$.correo"));
        assertEquals("RECEPCIONISTA", texto(creado, "$.rol"));
        assertEquals(true, com.jayway.jsonpath.JsonPath.read(creado, "$.activo"));

        String sesion = cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}")).andExpect(status().isOk()));
        assertEquals("RECEPCIONISTA", com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(sesion, "$.roles").get(0));
    }

    @Test
    void crearUnUsuarioInternoValidaLaFormaYLasReglas() throws Exception {
        String admin = admin();
        int n = SECUENCIA.incrementAndGet();

        // 400: el rol de un usuario interno no puede ser HUESPED, la contraseña debe ser segura, faltan campos
        crear("/api/usuarios", admin, cuerpoUsuario("a" + n + "@correo.com", "44" + n, "HUESPED")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.rol").exists());
        crear("/api/usuarios", admin, cuerpoUsuario("b" + n + "@correo.com", "45" + n, "RECEPCIONISTA").replace(CLAVE, "corta"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.contrasena").exists());
        crear("/api/usuarios", admin, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists()).andExpect(jsonPath("$.errores.correo").exists())
                .andExpect(jsonPath("$.errores.rol").exists()).andExpect(jsonPath("$.errores.documento").exists());

        // 409: correo y documento únicos
        crear("/api/usuarios", admin, cuerpoUsuario("c" + n + "@correo.com", "46" + n, "ADMINISTRADOR")).andExpect(status().isCreated());
        es409(crear("/api/usuarios", admin, cuerpoUsuario("C" + n + "@correo.com", "47" + n, "RECEPCIONISTA")),
                "Ya existe un usuario registrado con ese correo");
        es409(crear("/api/usuarios", admin, cuerpoUsuario("d" + n + "@correo.com", "46" + n, "RECEPCIONISTA")),
                "Ya existe un usuario registrado con ese documento");
    }

    // ---------- administración: cambiar rol ----------

    @Test
    void elAdministradorCambiaElRolDeOtroUsuarioPeroNoElPropio() throws Exception {
        String admin = admin();
        String interno = crearInterno("RECEPCIONISTA");
        long id = numero(interno, "$.id");

        actualizar("/api/usuarios/" + id + "/rol", admin, "{\"rol\":\"PERSONAL_SERVICIO\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("PERSONAL_SERVICIO"))
                .andExpect(jsonPath("$.correo").value(texto(interno, "$.correo")));
        // el rol nuevo rige en el siguiente inicio de sesión
        String sesion = cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + texto(interno, "$.correo") + "\",\"contrasena\":\"" + CLAVE + "\"}")).andExpect(status().isOk()));
        assertEquals("PERSONAL_SERVICIO", com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(sesion, "$.roles").get(0));

        long idDelAdmin = numero(cuerpo(consultar("/api/usuarios/yo", admin)), "$.id");
        es409(actualizar("/api/usuarios/" + idDelAdmin + "/rol", admin, "{\"rol\":\"RECEPCIONISTA\"}"), "No puede cambiar su propio rol");
        actualizar("/api/usuarios/999999/rol", admin, "{\"rol\":\"RECEPCIONISTA\"}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        actualizar("/api/usuarios/" + id + "/rol", admin, "{\"rol\":\"DUENO\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.rol").exists());
        actualizar("/api/usuarios/" + id + "/rol", admin, "{}").andExpect(status().isBadRequest());
    }

    // ---------- administración: desactivar ----------

    @Test
    void desactivarEsLogicoElUsuarioYaNoInicieSesionPeroSigueEnElListado() throws Exception {
        String admin = admin();
        String interno = crearInterno("RECEPCIONISTA");
        long id = numero(interno, "$.id");
        String correo = texto(interno, "$.correo");
        assertEquals(200, loginConClave(correo, CLAVE));

        actualizar("/api/usuarios/" + id + "/desactivar", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false)).andExpect(jsonPath("$.correo").value(correo));

        assertEquals(401, loginConClave(correo, CLAVE));
        assertEquals(Boolean.FALSE, activoSegunElListado(correo));   // sigue en el listado, marcado como inactivo
        actualizar("/api/usuarios/" + id + "/desactivar", admin).andExpect(status().isOk());   // idempotente

        long idDelAdmin = numero(cuerpo(consultar("/api/usuarios/yo", admin)), "$.id");
        es409(actualizar("/api/usuarios/" + idDelAdmin + "/desactivar", admin), "No puede desactivar su propia cuenta");
        actualizar("/api/usuarios/999999/desactivar", admin).andExpect(status().isNotFound());
    }

    @Test
    void noSeDesactivaAUnTitularConReservasActivasPeroSiAlCancelarlasYSusReservasSeConservan() throws Exception {
        String admin = admin();
        String recepcion = recepcion();
        String[] huesped = huespedNuevo();   // {token, documento, nombre}
        long id = numero(cuerpo(consultar("/api/usuarios/yo", huesped[0])), "$.id");
        String titular = persona(huesped[1], huesped[2], "1990-01-01");
        String codigo = texto(cuerpo(crear("/api/reservas", recepcion,
                reserva("APT-201", "2027-10-04", "2027-10-07", "DIRECTO", titular, titular)).andExpect(status().isCreated())), "$.codigo");

        // PENDIENTE: lo decide el dominio por documento y responde 409
        es409Con(actualizar("/api/usuarios/" + id + "/desactivar", admin), "No se puede desactivar al usuario: es titular de reservas PENDIENTE");

        // CONFIRMADA: sigue sin poderse
        long total = numero(cuerpo(consultar("/api/reservas/" + codigo, recepcion)), "$.valorTotal");
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, total)).andExpect(status().isCreated());
        actualizar("/api/reservas/" + codigo + "/confirmar", recepcion, confirmacion("16:00", "Recepción")).andExpect(status().isOk());
        es409Con(actualizar("/api/usuarios/" + id + "/desactivar", admin), "reservas CONFIRMADA");

        // al cancelar la reserva ya no es activa: se puede desactivar, y la reserva (histórica) y su titular se conservan
        actualizar("/api/reservas/" + codigo + "/cancelar", recepcion, "{\"motivo\":\"Cambio de planes\",\"autor\":\"Recepción\"}")
                .andExpect(status().isOk());
        actualizar("/api/usuarios/" + id + "/desactivar", admin).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        consultar("/api/reservas/" + codigo, recepcion).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.titular.documento").value(huesped[1]));
    }

    // ---------- perfil ----------

    @Test
    void cadaRolConsultaYEditaSoloSuPropioPerfilYElCorreoNoSeEdita() throws Exception {
        for (String[] cuenta : new String[][] {{"admin@sga.local", "Admin2026", "ADMINISTRADOR"},
                {"recepcion@sga.local", "Recepcion2026", "RECEPCIONISTA"}, {"servicio@sga.local", "Servicio2026", "PERSONAL_SERVICIO"},
                {"huesped2@sga.local", "Huesped2026", "HUESPED"}}) {
            String perfil = cuerpo(consultar("/api/usuarios/yo", token(cuenta[0], cuenta[1])).andExpect(status().isOk()));
            sinSecretos(perfil);
            assertEquals(cuenta[0], texto(perfil, "$.correo"));
            assertEquals(cuenta[2], texto(perfil, "$.rol"));
        }
        mvc.perform(MockMvcRequestBuilders.get("/api/usuarios/yo")).andExpect(status().isUnauthorized());

        String interno = crearInterno("RECEPCIONISTA");
        String token = token(texto(interno, "$.correo"), CLAVE);
        actualizar("/api/usuarios/yo", token,
                "{\"nombre\":\"Nombre Nuevo\",\"telefono\":\"3109998877\",\"correo\":\"otro@correo.com\",\"documento\":\"1\",\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nombre Nuevo"))
                .andExpect(jsonPath("$.telefono").value("3109998877"))
                .andExpect(jsonPath("$.correo").value(texto(interno, "$.correo")))         // el correo no se edita
                .andExpect(jsonPath("$.documento").value(texto(interno, "$.documento")))   // ni el documento
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"));                      // ni el rol

        consultar("/api/usuarios/yo", token).andExpect(jsonPath("$.nombre").value("Nombre Nuevo"));
        actualizar("/api/usuarios/yo", token, "{\"nombre\":\"\",\"telefono\":\"abc\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists()).andExpect(jsonPath("$.errores.telefono").exists());
        mvc.perform(MockMvcRequestBuilders.put("/api/usuarios/yo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"telefono\":\"3109998877\"}")).andExpect(status().isUnauthorized());
    }

    // ---------- cambio de contraseña ----------

    @Test
    void cambiarLaContrasenaExigeLaActualYLaNuevaSigueLasReglasDelRegistro() throws Exception {
        String interno = crearInterno("PERSONAL_SERVICIO");
        String correo = texto(interno, "$.correo");
        String token = token(correo, CLAVE);

        // 400: la nueva debe ser segura (8 caracteres, mayúscula y número) y los dos campos son obligatorios
        actualizar("/api/usuarios/yo/contrasena", token, "{\"contrasenaActual\":\"" + CLAVE + "\",\"contrasenaNueva\":\"corta\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.contrasenaNueva").exists());
        actualizar("/api/usuarios/yo/contrasena", token, "{\"contrasenaActual\":\"" + CLAVE + "\",\"contrasenaNueva\":\"sinmayusculas1\"}")
                .andExpect(status().isBadRequest());
        actualizar("/api/usuarios/yo/contrasena", token, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasenaActual").exists()).andExpect(jsonPath("$.errores.contrasenaNueva").exists());

        // 409: la actual no es correcta, o la nueva es igual a la actual
        es409(actualizar("/api/usuarios/yo/contrasena", token, "{\"contrasenaActual\":\"Equivocada1\",\"contrasenaNueva\":\"Nueva2026Clave\"}"),
                "La contraseña actual no es correcta");
        es409(actualizar("/api/usuarios/yo/contrasena", token, "{\"contrasenaActual\":\"" + CLAVE + "\",\"contrasenaNueva\":\"" + CLAVE + "\"}"),
                "La contraseña nueva debe ser distinta de la actual");
        assertEquals(200, loginConClave(correo, CLAVE));   // nada cambió

        actualizar("/api/usuarios/yo/contrasena", token, "{\"contrasenaActual\":\"" + CLAVE + "\",\"contrasenaNueva\":\"Nueva2026Clave\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").value("La contraseña se cambió correctamente"));
        assertEquals(401, loginConClave(correo, CLAVE));
        assertEquals(200, loginConClave(correo, "Nueva2026Clave"));
        mvc.perform(MockMvcRequestBuilders.put("/api/usuarios/yo/contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenaActual\":\"a\",\"contrasenaNueva\":\"Nueva2026Clave\"}")).andExpect(status().isUnauthorized());
    }

    // ---------- recuperación de contraseña ----------

    @Test
    void recuperarLaContrasenaConElCodigoRecibidoQueSoloSirveUnaVez() throws Exception {
        String interno = crearInterno("RECEPCIONISTA");
        String correo = texto(interno, "$.correo");

        solicitar(correo);
        String codigo = codigoEnviadoA(correo);
        assertEquals(6, codigo.length());

        confirmar(correo, codigo, "Recuperada2026").andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("La contraseña se cambió correctamente; ya puede iniciar sesión"));
        assertEquals(401, loginConClave(correo, CLAVE));
        assertEquals(200, loginConClave(correo, "Recuperada2026"));

        // de un solo uso: el mismo código ya no sirve
        es409(confirmar(correo, codigo, "OtraClave2026"), CODIGO_RECHAZADO);
        assertEquals(200, loginConClave(correo, "Recuperada2026"));
    }

    @Test
    void laRespuestaDeSolicitarEsLaMismaExistaONoElCorreoYNoSeEnviaNadaAUnCorreoDesconocido() throws Exception {
        String conocido = texto(crearInterno("RECEPCIONISTA"), "$.correo");
        String respuestaConocido = cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"" + conocido + "\"}")).andExpect(status().isOk()));
        long total = numero(cuerpo(consultar("/api/notificaciones", admin())), "$.totalElementos");

        String respuestaDesconocido = cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"nadie.registrado@correo.com\"}")).andExpect(status().isOk()));

        assertEquals(respuestaConocido, respuestaDesconocido);
        assertEquals(total, numero(cuerpo(consultar("/api/notificaciones", admin())), "$.totalElementos"));   // no se envió nada
        // un usuario desactivado tampoco recibe código, y se responde igual
        String desactivado = crearInterno("RECEPCIONISTA");
        actualizar("/api/usuarios/" + numero(desactivado, "$.id") + "/desactivar", admin()).andExpect(status().isOk());
        assertEquals(respuestaConocido, cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"" + texto(desactivado, "$.correo") + "\"}")).andExpect(status().isOk())));
        assertEquals(total, numero(cuerpo(consultar("/api/notificaciones", admin())), "$.totalElementos"));
    }

    @Test
    void solicitarYConfirmarSonPublicosYValidanLaForma() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"no-es-un-correo\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.correo").exists());
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        confirmar("a@correo.com", "12", "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists()).andExpect(jsonPath("$.errores.contrasenaNueva").exists());
        confirmar("a@correo.com", "123456", "sinmayuscula1").andExpect(status().isBadRequest());
        confirmar("a@correo.com", "abcdef", "Valida2026Clave").andExpect(status().isBadRequest());
    }

    @Test
    void confirmarConUnCorreoDesconocidoOUnCodigoIncorrectoDaElMismoRechazo() throws Exception {
        String correo = texto(crearInterno("RECEPCIONISTA"), "$.correo");
        solicitar(correo);
        String codigo = codigoEnviadoA(correo);
        String otro = "000000".equals(codigo) ? "111111" : "000000";

        es409(confirmar("nadie.registrado@correo.com", "123456", "Valida2026Clave"), CODIGO_RECHAZADO);
        es409(confirmar(correo, otro, "Valida2026Clave"), CODIGO_RECHAZADO);
        assertEquals(200, loginConClave(correo, CLAVE));   // la contraseña no cambió
    }

    @Test
    void elCodigoVenceALosQuinceMinutos() throws Exception {
        String correo = texto(crearInterno("RECEPCIONISTA"), "$.correo");
        solicitar(correo);
        String codigo = codigoEnviadoA(correo);

        reloj.avanzar(java.time.Duration.ofMinutes(15));
        es409(confirmar(correo, codigo, "Tarde2026Clave"), CODIGO_RECHAZADO);
        assertEquals(200, loginConClave(correo, CLAVE));

        // se pide uno nuevo y ese sí sirve (reemplaza al anterior)
        solicitar(correo);
        String nuevo = codigoEnviadoA(correo);
        confirmar(correo, nuevo, "Tarde2026Clave").andExpect(status().isOk());
        assertEquals(200, loginConClave(correo, "Tarde2026Clave"));
    }

    @Test
    void unCodigoJustoAntesDeVencerTodaviaSirve() throws Exception {
        String correo = texto(crearInterno("RECEPCIONISTA"), "$.correo");
        solicitar(correo);
        String codigo = codigoEnviadoA(correo);

        reloj.avanzar(java.time.Duration.ofMinutes(14).plusSeconds(59));

        confirmar(correo, codigo, "Justo2026Clave").andExpect(status().isOk());
    }

    @Test
    void trasCincoIntentosFallidosNiElCodigoCorrectoSirve() throws Exception {
        String correo = texto(crearInterno("RECEPCIONISTA"), "$.correo");
        solicitar(correo);
        String codigo = codigoEnviadoA(correo);
        String otro = "000000".equals(codigo) ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            es409(confirmar(correo, otro, "Fuerza2026Bruta"), CODIGO_RECHAZADO);
        }

        es409(confirmar(correo, codigo, "Fuerza2026Bruta"), CODIGO_RECHAZADO);   // bloqueado: hay que pedir otro
        assertEquals(200, loginConClave(correo, CLAVE));
        solicitar(correo);
        confirmar(correo, codigoEnviadoA(correo), "Fuerza2026Bruta").andExpect(status().isOk());
    }

    @Test
    void laSolicitudNuncaDevuelveElCodigoSoloLlegaPorElCanalDeNotificacion() throws Exception {
        String correo = texto(crearInterno("RECEPCIONISTA"), "$.correo");
        solicitar(correo);
        String codigo = codigoEnviadoA(correo);

        // la solicitud nunca devuelve el código: llega solo por el canal de notificación
        String respuesta = cuerpo(mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"" + correo + "\"}")).andExpect(status().isOk()));
        assertFalse(respuesta.contains(codigo));
        assertFalse(respuesta.matches(".*\\d{6}.*"), "ningún código de 6 dígitos en la respuesta: " + respuesta);
    }
}
