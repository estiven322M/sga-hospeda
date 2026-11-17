package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * Reportes (7.9, L-19) y su exportación a Excel (L-23), de extremo a extremo con login real y comprobando el CUERPO:
 * solo el administrador los consulta (401 sin token, 403 con otro rol), el rango de fechas es obligatorio (400) y un
 * rango invertido responde 409 con el mensaje de la regla.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reportes")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportesIntegracionTest {

    private static final String RANGO = "?desde=2020-01-01&hasta=2035-12-31";

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

    private String administrador() throws Exception { return token("admin@sga.local", "Admin2026"); }
    private String recepcion() throws Exception { return token("recepcion@sga.local", "Recepcion2026"); }

    @Test
    void ocupacion_respondeLasNochesVendidasYElPorcentajeDelPeriodo() throws Exception {
        mvc.perform(get("/api/reportes/ocupacion?desde=2026-01-01&hasta=2026-01-10")
                        .header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-01-01"))
                .andExpect(jsonPath("$.hasta").value("2026-01-10"))
                .andExpect(jsonPath("$.nochesPeriodo").value(10))
                .andExpect(jsonPath("$.apartamentosActivos").isNumber())
                .andExpect(jsonPath("$.nochesDisponibles").isNumber())
                .andExpect(jsonPath("$.nochesVendidas").isNumber())
                .andExpect(jsonPath("$.porcentajeOcupacion").isNumber());
    }

    @Test
    void ingresos_sumaLosPagosSembradosPorMedioDePago() throws Exception {
        String cuerpo = mvc.perform(get("/api/reportes/ingresos" + RANGO)
                        .header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.porMedio").isArray())
                .andReturn().getResponse().getContentAsString();
        // el total es la suma de las filas por medio de pago
        java.util.List<Number> porMedio = JsonPath.read(cuerpo, "$.porMedio[*].total");
        Number total = JsonPath.read(cuerpo, "$.total");
        assertEquals(total.longValue(), porMedio.stream().mapToLong(Number::longValue).sum());
    }

    @Test
    void reservasPorCanal_traeUnaFilaPorCanalEIncluyeCanceladasYNoShow() throws Exception {
        // los datos de prueba traen reservas en todos los estados y una por cada canal
        mvc.perform(get("/api/reportes/reservas-por-canal" + RANGO)
                        .header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canales.length()").value(3))
                .andExpect(jsonPath("$.canales[*].canal").value(org.hamcrest.Matchers.contains("PORTAL", "DIRECTO", "EXTERNO")))
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.canceladas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.noShow").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void unRangoInvertidoResponde409ConElMensajeDeLaRegla() throws Exception {
        for (String reporte : new String[]{"ocupacion", "ingresos", "reservas-por-canal", "ingresos/exportar"}) {
            mvc.perform(get("/api/reportes/" + reporte + "?desde=2026-02-10&hasta=2026-02-01")
                            .header(HttpHeaders.AUTHORIZATION, administrador()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.mensaje").value("El fin del periodo no puede ser anterior a su inicio"))
                    .andExpect(jsonPath("$.errores").doesNotExist())
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }

    @Test
    void sinRangoDeFechasResponde400() throws Exception {
        mvc.perform(get("/api/reportes/ocupacion?desde=2026-02-10").header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void soloElAdministradorConsultaLosReportes() throws Exception {
        mvc.perform(get("/api/reportes/ocupacion" + RANGO))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/reportes/ingresos" + RANGO).header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/reportes/ingresos/exportar" + RANGO).header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void exportar_entregaUnExcelConEncabezadosYTotales() throws Exception {
        byte[] archivo = mvc.perform(get("/api/reportes/reservas-por-canal/exportar" + RANGO + "&formato=xlsx")
                        .header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("reporte-reservas-por-canal-2020-01-01-a-2035-12-31.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("Canal de origen", hoja.getRow(2).getCell(0).getStringCellValue());
            assertEquals("Canceladas", hoja.getRow(2).getCell(2).getStringCellValue());
            assertEquals("PORTAL", hoja.getRow(3).getCell(0).getStringCellValue());
            // tres canales (filas 3 a 5) y la fila de totales
            assertEquals("Total", hoja.getRow(6).getCell(0).getStringCellValue());
            double suma = hoja.getRow(3).getCell(1).getNumericCellValue() + hoja.getRow(4).getCell(1).getNumericCellValue()
                    + hoja.getRow(5).getCell(1).getNumericCellValue();
            assertEquals(suma, hoja.getRow(6).getCell(1).getNumericCellValue());
            assertTrue(suma >= 3);
        }
    }

    @Test
    void exportar_unTipoOUnFormatoDesconocidoResponde400() throws Exception {
        mvc.perform(get("/api/reportes/huespedes/exportar" + RANGO).header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reportes/ingresos/exportar" + RANGO + "&formato=pdf")
                        .header(HttpHeaders.AUTHORIZATION, administrador()))
                .andExpect(status().isBadRequest());
    }
}
