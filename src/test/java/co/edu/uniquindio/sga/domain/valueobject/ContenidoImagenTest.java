package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.service.ValidadorImagenes;
import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;

/** 7.3 y 7.2: solo JPG, PNG o WEBP (reconocidos por su contenido) y con un tamaño máximo configurable. */
class ContenidoImagenTest {

    @Test
    void reconoceUnPngPorSuFirma() {
        ContenidoImagen imagen = ContenidoImagen.de("fachada.png", ImagenesDePrueba.png());

        assertEquals(TipoImagen.PNG, imagen.tipo());
        assertEquals("image/png", imagen.tipo().tipoMime());
        assertEquals("fachada.png", imagen.nombre());
    }

    @Test
    void reconoceUnJpgYUnWebp() {
        assertEquals(TipoImagen.JPEG, ContenidoImagen.de("a.jpg", ImagenesDePrueba.jpg()).tipo());
        assertEquals(TipoImagen.WEBP, ContenidoImagen.de("a.webp", ImagenesDePrueba.webp()).tipo());
        assertEquals("jpg", TipoImagen.JPEG.extension());
        assertEquals("webp", TipoImagen.WEBP.extension());
    }

    @Test
    void elFormatoSeTomaDelContenidoNoDeLaExtension() {
        // un texto renombrado a .jpg se rechaza, y un PNG llamado .txt se acepta como PNG
        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> ContenidoImagen.de("foto.jpg", ImagenesDePrueba.texto()));
        assertEquals("Solo se admiten imágenes JPG, PNG o WEBP: foto.jpg no lo es", e.getMessage());
        assertEquals(TipoImagen.PNG, ContenidoImagen.de("foto.txt", ImagenesDePrueba.png()).tipo());
    }

    @Test
    void rechazaOtrosFormatosDeImagen() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0, 0, 0};
        byte[] bmp = {'B', 'M', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        byte[] riffNoWebp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};

        assertThrows(ReglaDominioException.class, () -> ContenidoImagen.de("a.gif", gif));
        assertThrows(ReglaDominioException.class, () -> ContenidoImagen.de("a.bmp", bmp));
        assertThrows(ReglaDominioException.class, () -> ContenidoImagen.de("a.wav", riffNoWebp));
    }

    @Test
    void rechazaUnArchivoVacioOMuyCorto() {
        assertThrows(ReglaDominioException.class, () -> ContenidoImagen.de("a.png", new byte[0]));
        assertThrows(ReglaDominioException.class, () -> ContenidoImagen.de("a.png", null));
        assertThrows(ReglaDominioException.class, () -> ContenidoImagen.de("a.png", new byte[] {(byte) 0xFF, (byte) 0xD8}));
    }

    @Test
    void sinNombreUsaUnoPorDefecto() {
        assertEquals("imagen", ContenidoImagen.de(null, ImagenesDePrueba.png()).nombre());
        assertEquals("imagen", ContenidoImagen.de("  ", ImagenesDePrueba.png()).nombre());
    }

    @Test
    void elContenidoEsInmutable() {
        byte[] original = ImagenesDePrueba.png();
        ContenidoImagen imagen = ContenidoImagen.de("a.png", original);
        original[0] = 0;

        assertEquals((byte) 0x89, imagen.bytes()[0]);
        assertNotSame(imagen.bytes(), imagen.bytes());
        assertEquals(imagen.bytes().length, imagen.tamanoBytes());
    }

    // ---------- tamaño máximo ----------

    private static final long MB = 1024L * 1024L;
    private final ValidadorImagenes validador = new ValidadorImagenes(5 * MB, 2 * MB);

    @Test
    void unaImagenDeApartamentoDeExactamenteCincoMegasEsValida() {
        validador.validarParaApartamento(ContenidoImagen.de("a.png", ImagenesDePrueba.pngDe((int) (5 * MB))));
    }

    @Test
    void unaImagenDeApartamentoDeMasDeCincoMegasSeRechaza() {
        ContenidoImagen grande = ContenidoImagen.de("grande.png", ImagenesDePrueba.pngDe((int) (5 * MB) + 1));

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> validador.validarParaApartamento(grande));

        assertEquals("La imagen grande.png pesa más de 5 MB (el máximo permitido)", e.getMessage());
    }

    @Test
    void laFotoDePerfilTieneUnLimiteMasBajo() {
        ContenidoImagen tresMegas = ContenidoImagen.de("yo.png", ImagenesDePrueba.pngDe((int) (3 * MB)));

        validador.validarParaApartamento(tresMegas);   // para un apartamento sí cabe
        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> validador.validarParaPerfil(tresMegas));

        assertTrue(e.getMessage().contains("2 MB"));
        validador.validarParaPerfil(ContenidoImagen.de("yo.png", ImagenesDePrueba.pngDe((int) (2 * MB))));
    }

    @Test
    void losLimitesDebenSerPositivos() {
        assertThrows(ReglaDominioException.class, () -> new ValidadorImagenes(0, MB));
        assertThrows(ReglaDominioException.class, () -> new ValidadorImagenes(MB, 0));
    }

    @Test
    void laReferenciaDeUnaImagenGuardadaExigeUrlYReferencia() {
        assertThrows(ReglaDominioException.class, () -> new ReferenciaImagen(null, "r"));
        assertThrows(ReglaDominioException.class, () -> new ReferenciaImagen("u", " "));
    }
}
