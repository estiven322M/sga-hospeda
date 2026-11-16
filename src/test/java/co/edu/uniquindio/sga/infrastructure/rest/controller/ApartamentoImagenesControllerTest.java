package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.exception.ImagenNoEncontradaException;
import co.edu.uniquindio.sga.application.usecase.ActivarApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.ActualizarApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.AgregarImagenesApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarCalendarioUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.DarDeBajaApartamentoUseCase;
import co.edu.uniquindio.sga.application.usecase.EstablecerImagenPrincipalUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarApartamentosUseCase;
import co.edu.uniquindio.sga.application.usecase.RetirarImagenApartamentoUseCase;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.AlmacenamientoImagenesException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.TipoImagen;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.CalendarioMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ImagenSubidaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapperImpl;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;

/** Contrato HTTP de las imágenes de un apartamento (7.3): multipart, códigos, Location y forma de cada error. */
class ApartamentoImagenesControllerTest {

    private static final IdentificacionApartamento ID = new IdentificacionApartamento("APT-101");

    private final AgregarImagenesApartamentoUseCase agregar = mock(AgregarImagenesApartamentoUseCase.class);
    private final EstablecerImagenPrincipalUseCase principal = mock(EstablecerImagenPrincipalUseCase.class);
    private final RetirarImagenApartamentoUseCase retirar = mock(RetirarImagenApartamentoUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ApartamentoController(mock(CrearApartamentoUseCase.class),
                        mock(ConsultarApartamentoUseCase.class), mock(ListarApartamentosUseCase.class),
                        mock(ActualizarApartamentoUseCase.class), mock(ActivarApartamentoUseCase.class),
                        mock(DarDeBajaApartamentoUseCase.class), mock(ConsultarCalendarioUseCase.class),
                        agregar, principal, retirar, new ImagenSubidaMapper(), new ApartamentoMapperImpl(),
                        new ReservaMapperImpl(), new CalendarioMapper(), new SolicitudMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static Apartamento conImagenes(int cuantas) {
        Apartamento apartamento = ApartamentoDePrueba.nuevo(ID, "Vista", 2, 4);
        apartamento.agregarImagenes(java.util.stream.IntStream.rangeClosed(1, cuantas).mapToObj(ImagenesDePrueba::referencia).toList());
        return apartamento;
    }

    private static MockMultipartFile archivo(String nombre, byte[] bytes, String tipo) {
        return new MockMultipartFile("imagenes", nombre, tipo, bytes);
    }

    @Test
    void subirResponde201ConLocationYLaGaleriaConSuPrincipal() throws Exception {
        when(agregar.ejecutar(eq(ID), any())).thenReturn(conImagenes(2));

        mvc.perform(multipart("/api/apartamentos/APT-101/imagenes")
                        .file(archivo("a.png", ImagenesDePrueba.png(), "image/png"))
                        .file(archivo("b.jpg", ImagenesDePrueba.jpg(), "image/jpeg")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/APT-101")))
                .andExpect(jsonPath("$.imagenes", hasSize(2)))
                .andExpect(jsonPath("$.imagenPrincipal").value("http://localhost:8080/imagenes/apartamentos/APT-101/prueba-1.png"))
                .andExpect(jsonPath("$.imagenes[0].principal").value(true))
                .andExpect(jsonPath("$.imagenes[1].principal").value(false))
                .andExpect(jsonPath("$.imagenes[0].id").isNotEmpty());
    }

    @Test
    void elControllerEntregaAlCasoDeUsoElContenidoReconocidoPorSuFirmaNoPorElTipoDeclarado() throws Exception {
        when(agregar.ejecutar(eq(ID), any())).thenReturn(conImagenes(1));

        mvc.perform(multipart("/api/apartamentos/APT-101/imagenes")
                        .file(archivo("foto.txt", ImagenesDePrueba.png(), "text/plain")))
                .andExpect(status().isCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContenidoImagen>> captura = ArgumentCaptor.forClass(List.class);
        verify(agregar).ejecutar(eq(ID), captura.capture());
        assertEquals(TipoImagen.PNG, captura.getValue().get(0).tipo());
        assertEquals("foto.txt", captura.getValue().get(0).nombre());
    }

    private static void assertEquals(Object esperado, Object real) {
        org.junit.jupiter.api.Assertions.assertEquals(esperado, real);
    }

    @Test
    void unArchivoQueNoEsUnaImagenResponde409SinLlamarAlCasoDeUso() throws Exception {
        mvc.perform(multipart("/api/apartamentos/APT-101/imagenes")
                        .file(archivo("falsa.jpg", ImagenesDePrueba.texto(), "image/jpeg")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Solo se admiten imágenes JPG, PNG o WEBP: falsa.jpg no lo es"))
                .andExpect(jsonPath("$.errores").doesNotExist());
        verifyNoInteractions(agregar);
    }

    @Test
    void sinNingunaParteResponde400() throws Exception {
        mvc.perform(multipart("/api/apartamentos/APT-101/imagenes"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(agregar);
    }

    @Test
    void unLoteQueNoCabeResponde409ConLaReglaDelDominio() throws Exception {
        when(agregar.ejecutar(eq(ID), any())).thenThrow(new ReglaDominioException(
                "Un apartamento admite como máximo 10 imágenes: ya tiene 9 y se intentan agregar 2"));

        mvc.perform(multipart("/api/apartamentos/APT-101/imagenes")
                        .file(archivo("a.png", ImagenesDePrueba.png(), "image/png"))
                        .file(archivo("b.png", ImagenesDePrueba.png(), "image/png")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("como máximo 10 imágenes")));
    }

    @Test
    void siElAlmacenFallaResponde503ConUnMensajeClaroYSinTrazas() throws Exception {
        when(agregar.ejecutar(eq(ID), any())).thenThrow(new AlmacenamientoImagenesException("tiempo agotado: connect timed out", new RuntimeException("causa")));

        mvc.perform(multipart("/api/apartamentos/APT-101/imagenes")
                        .file(archivo("a.png", ImagenesDePrueba.png(), "image/png")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.mensaje", containsString("El servicio de imágenes no está disponible")))
                .andExpect(jsonPath("$.mensaje", not(containsString("connect timed out"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void elegirLaPrincipalResponde200ConLaGaleria() throws Exception {
        Apartamento apartamento = conImagenes(3);
        String tercera = apartamento.getImagenes().get(2).id();
        apartamento.establecerImagenPrincipal(tercera);
        when(principal.ejecutar(ID, tercera)).thenReturn(apartamento);

        mvc.perform(put("/api/apartamentos/APT-101/imagenes/" + tercera + "/principal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenPrincipal").value(apartamento.getImagenes().get(2).url()))
                .andExpect(jsonPath("$.imagenes[2].principal").value(true));
    }

    @Test
    void retirarResponde200YLaImagenYaNoEstaEnLaGaleria() throws Exception {
        Apartamento apartamento = conImagenes(2);
        String segunda = apartamento.getImagenes().get(1).id();
        apartamento.retirarImagen(segunda);
        when(retirar.ejecutar(ID, segunda)).thenReturn(apartamento);

        mvc.perform(put("/api/apartamentos/APT-101/imagenes/" + segunda + "/retirar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenes", hasSize(1)));
    }

    @Test
    void unaImagenQueNoExisteResponde404() throws Exception {
        when(principal.ejecutar(ID, "no-existe")).thenThrow(new ImagenNoEncontradaException(ID, "no-existe"));
        when(retirar.ejecutar(ID, "no-existe")).thenThrow(new ImagenNoEncontradaException(ID, "no-existe"));

        mvc.perform(put("/api/apartamentos/APT-101/imagenes/no-existe/principal"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/apartamentos/APT-101/imagenes/no-existe/retirar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retirarLaUltimaImagenDeUnApartamentoActivoResponde409() throws Exception {
        when(retirar.ejecutar(eq(ID), any())).thenThrow(new ReglaDominioException(
                "Un apartamento activo debe conservar al menos una imagen: agregue otra antes de retirar la última, o dé de baja el apartamento"));

        mvc.perform(put("/api/apartamentos/APT-101/imagenes/abc/retirar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("conservar al menos una imagen")));
    }
}
