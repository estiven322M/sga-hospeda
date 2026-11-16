package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;
import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Imágenes de apartamentos (7.3) y foto de perfil (7.2) por HTTP con el almacén LOCAL (el que viene activo: sin internet ni
 * credenciales), con login real y comprobando el CUERPO de cada respuesta: la galería sembrada, la subida multipart, los 409 de
 * cada regla (formato, tamaño, máximo 10, una sola principal, última imagen de un apartamento activo, activar sin imagen), los 403
 * y 401 de cada endpoint y que los archivos se sirven en /imagenes/** sin token.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridoimagenes",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoImagenesIntegracionTest extends RecorridoBase {

    private static final String BASE_URL = "http://localhost:8080/imagenes/";
    private static final int MB = 1024 * 1024;

    // ---------- utilidades ----------

    private static MockMultipartFile imagen(String nombre, byte[] bytes) {
        return new MockMultipartFile("imagenes", nombre, "application/octet-stream", bytes);
    }

    private ResultActions subir(String apartamento, String token, MockMultipartFile... archivos) throws Exception {
        var peticion = multipart("/api/apartamentos/" + apartamento + "/imagenes");
        for (MockMultipartFile archivo : archivos) {
            peticion.file(archivo);
        }
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mvc.perform(peticion);
    }

    private ResultActions subirFoto(String token, byte[] bytes, String nombre) throws Exception {
        var peticion = multipart(HttpMethod.PUT, "/api/usuarios/yo/foto")
                .file(new MockMultipartFile("foto", nombre, "application/octet-stream", bytes));
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mvc.perform(peticion);
    }

    private String apartamento(String id) throws Exception {
        return cuerpo(consultar("/api/apartamentos/" + id, admin()).andExpect(status().isOk()));
    }

    private static List<String> ids(String apartamento) {
        return JsonPath.read(apartamento, "$.imagenes[*].id");
    }

    private static List<String> urls(String apartamento) {
        return JsonPath.read(apartamento, "$.imagenes[*].url");
    }

    private byte[] descargar(String url) throws Exception {
        assertTrue(url.startsWith(BASE_URL), "URL pública del almacén local: " + url);
        return mvc.perform(get("/" + url.substring("http://localhost:8080/".length())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private static boolean esPng(byte[] bytes) {
        return bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }

    // ---------- galería sembrada ----------

    @Test
    void todoApartamentoSembradoTieneImagenesConUnaSolaPrincipalYSusArchivosSeSirvenSinToken() throws Exception {
        String pagina = cuerpo(consultar("/api/apartamentos", admin()).andExpect(status().isOk()));
        int total = JsonPath.<List<?>>read(pagina, "$.contenido").size();

        assertTrue(total >= 7, "los 7 apartamentos sembrados (más los que creen otras pruebas): " + total);
        for (int i = 0; i < total; i++) {
            String ruta = "$.contenido[" + i + "]";
            String id = texto(pagina, ruta + ".identificacion");
            List<Boolean> principales = JsonPath.read(pagina, ruta + ".imagenes[*].principal");
            assertTrue(principales.size() >= 1, id + " tiene al menos una imagen");
            assertEquals(1, principales.stream().filter(p -> p).count(), id + " tiene exactamente una principal");
            String principal = texto(pagina, ruta + ".imagenPrincipal");
            assertTrue(principal.startsWith(BASE_URL + "apartamentos/" + id + "/"), id + ": " + principal);
            assertEquals(principal, JsonPath.<List<String>>read(pagina, ruta + ".imagenes[?(@.principal==true)].url").get(0));
        }
        // las imágenes de ejemplo las pide el navegador con <img>, que no envía el token
        byte[] bytes = descargar(texto(pagina, "$.contenido[0].imagenPrincipal"));
        assertTrue(esPng(bytes), "es un PNG de verdad");
    }

    @Test
    void elDetalleLaDisponibilidadYElListadoIncluyenLaImagenPrincipalYLaGaleria() throws Exception {
        String detalle = apartamento("APT-101");
        assertNotNull(texto(detalle, "$.imagenPrincipal"));
        assertEquals(2, ids(detalle).size());

        String disponibles = cuerpo(crear("/api/disponibilidad", huespedNuevo()[0],
                "{\"fechaEntrada\":\"2027-09-06\",\"fechaSalida\":\"2027-09-09\",\"ocupantes\":" + grupo(ADULTO_1) + "}")
                .andExpect(status().isOk()));
        List<String> principales = JsonPath.read(disponibles, "$.contenido[*].imagenPrincipal");
        assertFalse(principales.isEmpty());
        assertTrue(principales.stream().allMatch(u -> u != null && u.startsWith(BASE_URL + "apartamentos/APT-")), "cada tarjeta trae su imagen: " + principales);
        assertTrue(JsonPath.<List<List<?>>>read(disponibles, "$.contenido[*].imagenes").stream().allMatch(g -> g.size() >= 1));
    }

    @Test
    void unArchivoQueNoExisteEnElAlmacenEsUn404YNoSeSaleDeLaCarpeta() throws Exception {
        mvc.perform(get("/imagenes/apartamentos/APT-101/no-existe.png")).andExpect(status().isNotFound());
        int estado = mvc.perform(get("/imagenes/..%2F..%2Fapplication.properties")).andReturn().getResponse().getStatus();
        assertNotEquals(200, estado, "no se pueden leer archivos fuera de la carpeta de imágenes");
    }

    // ---------- subir ----------

    @Test
    void elAdministradorSubeVariasImagenesYLaPrincipalNoCambia() throws Exception {
        String antes = apartamento("APT-102");
        String principalAntes = texto(antes, "$.imagenPrincipal");

        ResultActions respuesta = subir("APT-102", admin(),
                imagen("sala.png", ImagenesDePrueba.png()), imagen("cocina.webp", ImagenesDePrueba.webp()), imagen("balcon.jpg", ImagenesDePrueba.jpg()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/apartamentos/APT-102")))
                .andExpect(jsonPath("$.imagenes", hasSize(5)));
        String despues = cuerpo(respuesta);

        assertEquals(principalAntes, texto(despues, "$.imagenPrincipal"), "subir más imágenes no cambia la principal");
        assertEquals(1, JsonPath.<List<Boolean>>read(despues, "$.imagenes[?(@.principal==true)].principal").size());
        List<String> nuevas = urls(despues).subList(2, 5);
        assertTrue(nuevas.get(0).endsWith(".png") && nuevas.get(1).endsWith(".webp") && nuevas.get(2).endsWith(".jpg"), "extensión según el contenido: " + nuevas);
        assertArrayEquals(ImagenesDePrueba.png(), descargar(nuevas.get(0)), "el archivo guardado es el que se subió");
        // la galería quedó guardada: una consulta nueva la devuelve igual
        assertEquals(urls(despues), urls(apartamento("APT-102")));
    }

    @Test
    void soloSeAdmitenJpgPngOWebpReconocidosPorSuContenido() throws Exception {
        String antes = apartamento("APT-203");
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0, 0, 0};

        es409(subir("APT-203", admin(), imagen("falsa.jpg", ImagenesDePrueba.texto())), "Solo se admiten imágenes JPG, PNG o WEBP: falsa.jpg no lo es");
        es409(subir("APT-203", admin(), imagen("animada.gif", gif)), "Solo se admiten imágenes JPG, PNG o WEBP: animada.gif no lo es");
        es409(subir("APT-203", admin(), imagen("vacia.png", new byte[0])), "La imagen vacia.png está vacía");
        // un lote con una sola mala se rechaza completo
        es409(subir("APT-203", admin(), imagen("buena.png", ImagenesDePrueba.png()), imagen("mala.jpg", ImagenesDePrueba.texto())),
                "Solo se admiten imágenes JPG, PNG o WEBP: mala.jpg no lo es");

        assertEquals(urls(antes), urls(apartamento("APT-203")), "ningún rechazo dejó imágenes a medias");
    }

    @Test
    void unaImagenDeMasDeCincoMegasSeRechazaPeroDeExactamenteCincoSeAcepta() throws Exception {
        es409(subir("APT-203", admin(), imagen("enorme.png", ImagenesDePrueba.pngDe(5 * MB + 1))),
                "La imagen enorme.png pesa más de 5 MB (el máximo permitido)");

        subir("APT-203", admin(), imagen("justa.png", ImagenesDePrueba.pngDe(5 * MB))).andExpect(status().isCreated());
    }

    @Test
    void elMaximoEsDiezImagenesPorApartamento() throws Exception {
        String inicial = apartamento("APT-201");
        int sembradas = ids(inicial).size();

        // un lote que no cabe se rechaza completo
        MockMultipartFile[] demasiadas = new MockMultipartFile[11 - sembradas];
        for (int i = 0; i < demasiadas.length; i++) {
            demasiadas[i] = imagen("i" + i + ".png", ImagenesDePrueba.png());
        }
        es409Con(subir("APT-201", admin(), demasiadas), "Un apartamento admite como máximo 10 imágenes");
        assertEquals(sembradas, ids(apartamento("APT-201")).size());

        // las que caben justo hasta 10 se aceptan
        MockMultipartFile[] justas = new MockMultipartFile[10 - sembradas];
        for (int i = 0; i < justas.length; i++) {
            justas[i] = imagen("j" + i + ".png", ImagenesDePrueba.png());
        }
        subir("APT-201", admin(), justas).andExpect(status().isCreated()).andExpect(jsonPath("$.imagenes", hasSize(10)));
        es409(subir("APT-201", admin(), imagen("once.png", ImagenesDePrueba.png())),
                "Un apartamento admite como máximo 10 imágenes: ya tiene 10 y se intentan agregar 1");

        // retirar una libera un cupo
        String retirada = ids(apartamento("APT-201")).get(9);
        actualizar("/api/apartamentos/APT-201/imagenes/" + retirada + "/retirar", admin()).andExpect(status().isOk());
        subir("APT-201", admin(), imagen("de-nuevo.png", ImagenesDePrueba.png())).andExpect(status().isCreated());
    }

    // ---------- principal y retirar ----------

    @Test
    void siempreHayExactamenteUnaPrincipalAlElegirlaOAlRetirarLa() throws Exception {
        String admin = admin();
        subir("APT-202", admin, imagen("a.png", ImagenesDePrueba.png()), imagen("b.png", ImagenesDePrueba.png())).andExpect(status().isCreated());
        String galeria = apartamento("APT-202");
        List<String> ids = ids(galeria);
        assertEquals(4, ids.size());

        // elegir la tercera como principal
        String tras = cuerpo(actualizar("/api/apartamentos/APT-202/imagenes/" + ids.get(2) + "/principal", admin).andExpect(status().isOk()));
        assertEquals(urls(galeria).get(2), texto(tras, "$.imagenPrincipal"));
        assertEquals(List.of(ids.get(2)), JsonPath.<List<String>>read(tras, "$.imagenes[?(@.principal==true)].id"));

        // retirar la principal: la primera de las que quedan lo es ahora
        String sinPrincipal = cuerpo(actualizar("/api/apartamentos/APT-202/imagenes/" + ids.get(2) + "/retirar", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenes", hasSize(3))));
        assertEquals(ids.get(0), JsonPath.<List<String>>read(sinPrincipal, "$.imagenes[?(@.principal==true)].id").get(0));
        assertFalse(ids(sinPrincipal).contains(ids.get(2)), "la retirada sale de la galería");
        // el archivo retirado sigue existiendo (retiro lógico): la URL todavía responde
        descargar(urls(galeria).get(2));

        // una retirada ya no se puede elegir ni retirar otra vez: 404
        actualizar("/api/apartamentos/APT-202/imagenes/" + ids.get(2) + "/principal", admin).andExpect(status().isNotFound());
        actualizar("/api/apartamentos/APT-202/imagenes/" + ids.get(2) + "/retirar", admin).andExpect(status().isNotFound());
    }

    @Test
    void unApartamentoActivoNoPuedeQuedarseSinSuUltimaImagen() throws Exception {
        String admin = admin();
        List<String> ids = ids(apartamento("APT-301"));
        for (int i = 1; i < ids.size(); i++) {
            actualizar("/api/apartamentos/APT-301/imagenes/" + ids.get(i) + "/retirar", admin).andExpect(status().isOk());
        }
        String ultima = ids(apartamento("APT-301")).get(0);

        es409Con(actualizar("/api/apartamentos/APT-301/imagenes/" + ultima + "/retirar", admin),
                "Un apartamento activo debe conservar al menos una imagen");

        assertEquals(1, ids(apartamento("APT-301")).size());
        // con otra imagen sí se puede retirar la primera
        subir("APT-301", admin, imagen("nueva.png", ImagenesDePrueba.png())).andExpect(status().isCreated());
        actualizar("/api/apartamentos/APT-301/imagenes/" + ultima + "/retirar", admin).andExpect(status().isOk());
    }

    @Test
    void unaImagenOUnApartamentoQueNoExistenSon404() throws Exception {
        String admin = admin();
        actualizar("/api/apartamentos/APT-101/imagenes/no-existe/principal", admin).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        actualizar("/api/apartamentos/APT-101/imagenes/no-existe/retirar", admin).andExpect(status().isNotFound());
        subir("APT-999", admin, imagen("a.png", ImagenesDePrueba.png())).andExpect(status().isNotFound());
    }

    // ---------- activar exige una imagen (7.3) ----------

    @Test
    void activarUnApartamentoExigeAlMenosUnaImagen() throws Exception {
        String admin = admin();
        crear("/api/apartamentos", admin, "{\"identificacion\":\"APT-777\",\"nombre\":\"Nuevo\",\"descripcion\":\"Sin fotos todavía\","
                + "\"dormitorios\":1,\"capacidad\":2,\"dotacion\":[],\"caracteristicas\":[]}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.imagenes", hasSize(0))).andExpect(jsonPath("$.imagenPrincipal").doesNotExist());

        // sin tarifas completas (7.4) no se activa; con ellas, ahora falta la imagen (7.3)
        es409Con(actualizar("/api/apartamentos/APT-777/activar", admin), "faltan tarifas");
        for (String temporada : List.of("BASE", "MEDIA", "ALTA")) {
            actualizar("/api/apartamentos/APT-777/tarifas/" + temporada, admin, "{\"valorPorOcupanteNoche\":90000}").andExpect(status().isOk());
        }
        es409(actualizar("/api/apartamentos/APT-777/activar", admin), "No se puede activar el apartamento: debe tener al menos una imagen");

        // con una imagen ya se puede activar
        subir("APT-777", admin, imagen("sala.png", ImagenesDePrueba.png())).andExpect(status().isCreated());
        actualizar("/api/apartamentos/APT-777/activar", admin).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));

        // y activo conserva al menos una
        String unica = ids(apartamento("APT-777")).get(0);
        es409Con(actualizar("/api/apartamentos/APT-777/imagenes/" + unica + "/retirar", admin), "Un apartamento activo debe conservar al menos una imagen");
    }

    // ---------- seguridad de los endpoints ----------

    @Test
    void soloElAdministradorGestionaLasImagenes() throws Exception {
        String id = ids(apartamento("APT-102")).get(0);
        for (String token : new String[] {recepcion(), servicio(), huespedNuevo()[0]}) {
            subir("APT-102", token, imagen("a.png", ImagenesDePrueba.png())).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            actualizar("/api/apartamentos/APT-102/imagenes/" + id + "/principal", token).andExpect(status().isForbidden());
            actualizar("/api/apartamentos/APT-102/imagenes/" + id + "/retirar", token).andExpect(status().isForbidden());
        }
        subir("APT-102", null, imagen("a.png", ImagenesDePrueba.png())).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/apartamentos/APT-102/imagenes/" + id + "/principal"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- foto de perfil (7.2) ----------

    @Test
    void cadaUsuarioCambiaSuFotoYSeVeEnSuPerfilYEnElListado() throws Exception {
        String[] huesped = huespedNuevo();   // {token, documento, nombre}
        assertNull(JsonPath.<Object>read(cuerpo(consultar("/api/usuarios/yo", huesped[0])), "$.fotoUrl"), "al principio no tiene foto");

        String respuesta = cuerpo(subirFoto(huesped[0], ImagenesDePrueba.png(), "yo.png").andExpect(status().isOk()));
        String foto = texto(respuesta, "$.fotoUrl");

        assertTrue(foto.startsWith(BASE_URL + "perfiles/") && foto.endsWith(".png"), foto);
        assertArrayEquals(ImagenesDePrueba.png(), descargar(foto));
        assertEquals(foto, texto(cuerpo(consultar("/api/usuarios/yo", huesped[0])), "$.fotoUrl"));
        assertFalse(respuesta.toLowerCase().contains("contrasena"));

        // cambiarla guarda la nueva (jpg) y devuelve otra URL
        String otra = texto(cuerpo(subirFoto(huesped[0], ImagenesDePrueba.jpg(), "otra.jpg").andExpect(status().isOk())), "$.fotoUrl");
        assertNotEquals(foto, otra);
        assertTrue(otra.endsWith(".jpg"));
        // el administrador la ve en el listado de usuarios
        String listado = cuerpo(consultar("/api/usuarios?rol=HUESPED", admin()));
        assertTrue(listado.contains(otra), "el listado de usuarios trae la foto");
    }

    @Test
    void todosLosRolesPuedenTenerFoto() throws Exception {
        for (String token : new String[] {admin(), recepcion(), servicio()}) {
            subirFoto(token, ImagenesDePrueba.webp(), "yo.webp").andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").isNotEmpty());
        }
    }

    @Test
    void laFotoDePerfilTieneUnLimiteDeDosMegasYSoloAdmiteImagenes() throws Exception {
        String[] huesped = huespedNuevo();
        String foto = texto(cuerpo(subirFoto(huesped[0], ImagenesDePrueba.png(), "yo.png").andExpect(status().isOk())), "$.fotoUrl");

        es409(subirFoto(huesped[0], ImagenesDePrueba.pngDe(2 * MB + 1), "grande.png"), "La imagen grande.png pesa más de 2 MB (el máximo permitido)");
        es409(subirFoto(huesped[0], ImagenesDePrueba.texto(), "falsa.jpg"), "Solo se admiten imágenes JPG, PNG o WEBP: falsa.jpg no lo es");
        subirFoto(huesped[0], ImagenesDePrueba.pngDe(2 * MB), "justa.png").andExpect(status().isOk());   // exactamente 2 MB sí

        // los rechazos no tocaron la foto anterior: se comprueba con otro usuario que nunca subió una buena
        String sinFoto = huespedNuevo()[0];
        es409(subirFoto(sinFoto, ImagenesDePrueba.pngDe(3 * MB), "grande.png"), "La imagen grande.png pesa más de 2 MB (el máximo permitido)");
        assertNull(JsonPath.<Object>read(cuerpo(consultar("/api/usuarios/yo", sinFoto)), "$.fotoUrl"));
        assertNotNull(foto);
    }

    @Test
    void cambiarLaFotoExigeSesionYSoloEsLaDelDuenoDelToken() throws Exception {
        subirFoto(null, ImagenesDePrueba.png(), "yo.png").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        // no hay forma de indicar de quién es la foto: siempre es la del token
        String uno = huespedNuevo()[0];
        String dos = huespedNuevo()[0];
        String fotoUno = texto(cuerpo(subirFoto(uno, ImagenesDePrueba.png(), "uno.png").andExpect(status().isOk())), "$.fotoUrl");
        assertNull(JsonPath.<Object>read(cuerpo(consultar("/api/usuarios/yo", dos)), "$.fotoUrl"), "la foto de uno no aparece en el otro");
        assertNotNull(fotoUno);
    }
}
