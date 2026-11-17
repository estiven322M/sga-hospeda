package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import co.edu.uniquindio.sga.testsupport.RelojMutableTestConfig;

/**
 * Preparación de apartamentos y novedades del personal de servicio (7.1, 7.6) por HTTP, comprobando el CUERPO: el personal de
 * servicio consulta los pendientes, los prepara y registra novedades (el autor sale del token y la fecha del reloj); el historial
 * llega de 10 en 10, de la más reciente a la más antigua; y NO puede declarar fuera de servicio ni registrar bloqueos.
 *
 * Datos sembrados con el reloj en 10-mar-2027: APT-302 está FUERA_DE_SERVICIO y tiene dos novedades (7 y 8 de marzo).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recorridonovedades",
        "test.reloj-inicial=2027-03-10T10:00",
        "sga.alojamiento.intervalo-vencimiento-minutos=1440"})
@Import(RelojMutableTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecorridoNovedadesIntegracionTest extends RecorridoBase {

    private static String novedad(String descripcion, String gravedad) {
        return "{\"descripcion\":\"" + descripcion + "\",\"gravedad\":\"" + gravedad + "\"}";
    }

    @Test
    void elPersonalDeServicioPreparaUnApartamentoPendienteYRegistraUnaNovedad() throws Exception {
        String servicio = servicio();
        // el administrador reincorpora APT-302: queda pendiente de preparación
        actualizar("/api/apartamentos/APT-302/estado-operativo/reincorporar", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"));

        // el personal de servicio consulta los pendientes
        consultar("/api/apartamentos?estadoOperativo=PENDIENTE_PREPARACION", servicio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-302")));

        // inicia la preparación, registra una novedad y lo marca preparado
        actualizar("/api/apartamentos/APT-302/estado-operativo/iniciar-preparacion", servicio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("EN_PREPARACION"));
        consultar("/api/apartamentos?estadoOperativo=EN_PREPARACION", servicio)
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-302")));

        crear("/api/apartamentos/APT-302/novedades", servicio, novedad("  Se cambió el sifón; ya no hay fuga  ", "MEDIA"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/APT-302/novedades")))
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-302"))
                .andExpect(jsonPath("$.fecha").value("2027-03-10T10:00:00"))
                .andExpect(jsonPath("$.autor").value("Personal de Servicio"))
                .andExpect(jsonPath("$.descripcion").value("Se cambió el sifón; ya no hay fuga"))
                .andExpect(jsonPath("$.gravedad").value("MEDIA"));

        // registrar una novedad no cambia el estado operativo
        consultar("/api/apartamentos/APT-302", servicio).andExpect(jsonPath("$.estadoOperativo").value("EN_PREPARACION"));

        actualizar("/api/apartamentos/APT-302/estado-operativo/marcar-preparado", servicio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
        // una transición que no existe la rechaza el dominio
        es409(actualizar("/api/apartamentos/APT-302/estado-operativo/marcar-preparado", servicio),
                "No se puede pasar de PREPARADO a PREPARADO");

        // historial: la más reciente primero (la de hoy, luego las sembradas del 8 y del 7 de marzo)
        consultar("/api/apartamentos/APT-302/novedades", servicio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[*].gravedad", contains("MEDIA", "ALTA", "CRITICA")))
                .andExpect(jsonPath("$.contenido[*].fecha",
                        contains("2027-03-10T10:00:00", "2027-03-08T09:00:00", "2027-03-07T09:00:00")));
    }

    @Test
    void elHistorialSePaginaDe10YCadaRolAutorizadoFirmaConSuNombre() throws Exception {
        String recepcion = recepcion();
        String admin = admin();
        for (int i = 1; i <= 11; i++) {
            reloj.fijarEn(INICIO.plusMinutes(i));
            crear("/api/apartamentos/APT-201/novedades", i % 2 == 0 ? admin : recepcion, novedad("Novedad " + i, "BAJA"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.autor").value(i % 2 == 0 ? "Administrador SGA" : "Recepcion Hospeda"));
        }

        consultar("/api/apartamentos/APT-201/novedades", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido", hasSize(10)))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("Novedad 11"))
                .andExpect(jsonPath("$.contenido[9].descripcion").value("Novedad 2"));
        consultar("/api/apartamentos/APT-201/novedades?pagina=1", recepcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.contenido[*].descripcion", contains("Novedad 1")));
        // el historial es por apartamento: el de APT-202 sigue vacío
        consultar("/api/apartamentos/APT-202/novedades", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void unaNovedadMalFormadaEs400YUnApartamentoInexistenteEs404() throws Exception {
        String servicio = servicio();

        crear("/api/apartamentos/APT-101/novedades", servicio, novedad("   ", "ALTA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.descripcion").value("La descripción de la novedad es obligatoria"));
        crear("/api/apartamentos/APT-101/novedades", servicio, novedad("a".repeat(501), "ALTA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").value("La descripción no puede superar 500 caracteres"));
        crear("/api/apartamentos/APT-101/novedades", servicio, "{\"descripcion\":\"Fuga de agua\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.gravedad").value("La gravedad es obligatoria"));
        crear("/api/apartamentos/APT-101/novedades", servicio, novedad("Fuga de agua", "URGENTE"))
                .andExpect(status().isBadRequest());
        // nada de lo anterior quedó registrado: sigue solo la novedad sembrada
        consultar("/api/apartamentos/APT-101/novedades", servicio).andExpect(jsonPath("$.totalElementos").value(1));

        crear("/api/apartamentos/NO-EXISTE/novedades", servicio, novedad("Fuga de agua", "ALTA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        consultar("/api/apartamentos/NO-EXISTE/novedades", servicio).andExpect(status().isNotFound());
        consultar("/api/apartamentos/APT-101/novedades?pagina=-1", servicio).andExpect(status().isBadRequest());
    }

    @Test
    void soloElPersonalAutorizadoRegistraYConsultaNovedades() throws Exception {
        String huesped = huespedNuevo()[0];

        crear("/api/apartamentos/APT-101/novedades", huesped, novedad("Fuga de agua", "ALTA"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        consultar("/api/apartamentos/APT-101/novedades", huesped).andExpect(status().isForbidden());
        mvc.perform(get("/api/apartamentos/APT-101/novedades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void elPersonalDeServicioNoDeclaraFueraDeServicioNiRegistraBloqueos() throws Exception {
        String servicio = servicio();
        String estadoAntes = texto(cuerpo(consultar("/api/apartamentos/APT-203", servicio)), "$.estadoOperativo");

        actualizar("/api/apartamentos/APT-203/estado-operativo/declarar-fuera-de-servicio", servicio)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        actualizar("/api/apartamentos/APT-302/estado-operativo/reincorporar", servicio).andExpect(status().isForbidden());
        crear("/api/apartamentos/APT-203/bloqueos", servicio,
                "{\"fechaInicio\":\"2027-06-01\",\"fechaFin\":\"2027-06-03\",\"motivo\":\"Pintura\"}")
                .andExpect(status().isForbidden());
        // nada cambió
        consultar("/api/apartamentos/APT-203", servicio).andExpect(jsonPath("$.estadoOperativo").value(estadoAntes));
    }
}
