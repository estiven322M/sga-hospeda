package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
 * I-04 e I-05 con el adaptador REAL: el proveedor es {@code cloudinary} pero apunta a un puerto cerrado de esta misma máquina (nunca
 * al servicio externo, I-08), así que cada subida falla de verdad. El administrador recibe un 503 con un mensaje claro y NO queda
 * nada registrado; las imágenes ya guardadas se siguen mostrando y reservar, confirmar y cobrar siguen funcionando.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridoimagenesfallo",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440",
        "sga.imagenes.proveedor=cloudinary",
        "sga.imagenes.tiempo-espera-ms=1500",
        "sga.imagenes.cloudinary.url-api=http://127.0.0.1:1",
        "sga.imagenes.cloudinary.cloud-name=nube-de-prueba",
        "sga.imagenes.cloudinary.api-key=clave-de-prueba",
        "sga.imagenes.cloudinary.api-secret=secreto-de-prueba"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoImagenesFalloIntegracionTest extends RecorridoBase {

    private static final String MENSAJE = "El servicio de imágenes no está disponible en este momento; intente de nuevo más tarde. El resto del sistema sigue funcionando";

    private ResultActions subir(String token, String apartamento) throws Exception {
        return mvc.perform(multipart("/api/apartamentos/" + apartamento + "/imagenes")
                .file(new MockMultipartFile("imagenes", "a.png", "image/png", ImagenesDePrueba.png()))
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private String apartamento(String id) throws Exception {
        return cuerpo(consultar("/api/apartamentos/" + id, admin()).andExpect(status().isOk()));
    }

    @Test
    void siElAlmacenFallaLaSubidaDa503SinCausasTecnicasYNoDejaNadaRegistrado() throws Exception {
        String antes = apartamento("APT-101");

        String respuesta = cuerpo(subir(admin(), "APT-101").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503)).andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE)).andExpect(jsonPath("$.errores").doesNotExist()));

        assertFalse(respuesta.contains("127.0.0.1") || respuesta.contains("secreto-de-prueba") || respuesta.contains("Exception"),
                "ni la dirección, ni las credenciales, ni trazas: " + respuesta);
        assertEquals(JsonPath.<List<String>>read(antes, "$.imagenes[*].id"), JsonPath.<List<String>>read(apartamento("APT-101"), "$.imagenes[*].id"));
    }

    @Test
    void laFotoDePerfilTambienDa503YNoCambiaNada() throws Exception {
        String token = huespedNuevo()[0];

        mvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/yo/foto")
                        .file(new MockMultipartFile("foto", "yo.png", "image/png", ImagenesDePrueba.png()))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.mensaje").value(MENSAJE));

        assertNull(JsonPath.<Object>read(cuerpo(consultar("/api/usuarios/yo", token)), "$.fotoUrl"));
    }

    @Test
    void lasReglasDelDominioSeEvaluanAntesDeTocarElAlmacen() throws Exception {
        // un archivo inválido es 409 (no 503): ni siquiera se intenta subir
        es409(mvc.perform(multipart("/api/apartamentos/APT-101/imagenes")
                        .file(new MockMultipartFile("imagenes", "falsa.jpg", "image/jpeg", ImagenesDePrueba.texto()))
                        .header(HttpHeaders.AUTHORIZATION, admin())),
                "Solo se admiten imágenes JPG, PNG o WEBP: falsa.jpg no lo es");
    }

    @Test
    void conElAlmacenCaidoLasImagenesSembradasSeSiguenMostrandoYSeReservaSeCobraYSeConfirma() throws Exception {
        // las imágenes de ejemplo viven en el almacén local, que siempre se sirve
        String detalle = apartamento("APT-102");
        String url = JsonPath.read(detalle, "$.imagenPrincipal");
        mvc.perform(get("/" + url.substring("http://localhost:8080/".length()))).andExpect(status().isOk());

        // reservar, cobrar y confirmar no dependen del almacén
        String recepcion = recepcion();
        String creada = cuerpo(crear("/api/reservas", recepcion, reserva("APT-102", "2027-09-06", "2027-09-09", "DIRECTO", ADULTO_1, ADULTO_1))
                .andExpect(status().isCreated()));
        String codigo = texto(creada, "$.codigo");
        crear("/api/reservas/" + codigo + "/folio/pagos", recepcion, pago("EFECTIVO", HOY, numero(creada, "$.valorTotal"))).andExpect(status().isCreated());
        actualizar("/api/reservas/" + codigo + "/confirmar", recepcion, confirmacion("16:00", "Recepción"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        assertTrue(true);
    }
}
