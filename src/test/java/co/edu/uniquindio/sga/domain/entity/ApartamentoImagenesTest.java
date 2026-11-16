package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ImagenApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;

/** 7.3: entre 1 y 10 imágenes por apartamento, exactamente una principal y retiro lógico. */
class ApartamentoImagenesTest {

    private static Apartamento apartamento() {
        return ApartamentoDePrueba.nuevo(new IdentificacionApartamento("APT-101"), "Vista", 2, 4);
    }

    private static List<ReferenciaImagen> referencias(int cuantas) {
        return IntStream.rangeClosed(1, cuantas).mapToObj(ImagenesDePrueba::referencia).toList();
    }

    private static Apartamento conImagenes(int cuantas) {
        Apartamento apartamento = apartamento();
        apartamento.agregarImagenes(referencias(cuantas));
        return apartamento;
    }

    @Test
    void naceSinImagenes() {
        Apartamento apartamento = apartamento();

        assertEquals(0, apartamento.cantidadImagenes());
        assertTrue(apartamento.getImagenes().isEmpty());
        assertTrue(apartamento.getImagenPrincipal().isEmpty());
    }

    @Test
    void laPrimeraImagenQuedaComoPrincipalYLasSiguientesNo() {
        Apartamento apartamento = conImagenes(3);

        List<ImagenApartamento> galeria = apartamento.getImagenes();
        assertEquals(3, galeria.size());
        assertTrue(galeria.get(0).principal());
        assertFalse(galeria.get(1).principal());
        assertFalse(galeria.get(2).principal());
        assertEquals(galeria.get(0), apartamento.getImagenPrincipal().orElseThrow());
        assertEquals("http://localhost:8080/imagenes/apartamentos/APT-101/prueba-1.png", galeria.get(0).url());
    }

    @Test
    void agregarMasImagenesNoCambiaLaPrincipal() {
        Apartamento apartamento = conImagenes(1);
        String principal = apartamento.getImagenPrincipal().orElseThrow().id();

        apartamento.agregarImagenes(List.of(ImagenesDePrueba.referencia(2)));

        assertEquals(principal, apartamento.getImagenPrincipal().orElseThrow().id());
        assertEquals(1, apartamento.getImagenes().stream().filter(ImagenApartamento::principal).count());
    }

    @Test
    void cadaImagenTieneSuPropioIdentificador() {
        Apartamento apartamento = conImagenes(10);

        assertEquals(10, apartamento.getImagenes().stream().map(ImagenApartamento::id).distinct().count());
    }

    @Test
    void admiteHastaDiezImagenes() {
        Apartamento apartamento = conImagenes(10);

        assertEquals(10, apartamento.cantidadImagenes());
    }

    @Test
    void rechazaLaOnceavaImagen() {
        Apartamento apartamento = conImagenes(10);

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> apartamento.agregarImagenes(referencias(1)));

        assertEquals("Un apartamento admite como máximo 10 imágenes: ya tiene 10 y se intentan agregar 1", e.getMessage());
        assertEquals(10, apartamento.cantidadImagenes());
    }

    @Test
    void unLoteQueNoCabeSeRechazaCompletoSinAgregarNinguna() {
        Apartamento apartamento = conImagenes(6);

        assertThrows(ReglaDominioException.class, () -> apartamento.agregarImagenes(referencias(5)));

        assertEquals(6, apartamento.cantidadImagenes());
    }

    @Test
    void verificarElCupoNoModificaNadaYExigeAlMenosUna() {
        Apartamento apartamento = conImagenes(9);

        apartamento.verificarCupoImagenes(1);
        assertThrows(ReglaDominioException.class, () -> apartamento.verificarCupoImagenes(2));
        ReglaDominioException vacio = assertThrows(ReglaDominioException.class, () -> apartamento.verificarCupoImagenes(0));

        assertEquals("Debe enviar al menos una imagen", vacio.getMessage());
        assertEquals(9, apartamento.cantidadImagenes());
    }

    @Test
    void cambiarLaPrincipalDejaSiempreExactamenteUna() {
        Apartamento apartamento = conImagenes(3);
        String tercera = apartamento.getImagenes().get(2).id();

        apartamento.establecerImagenPrincipal(tercera);

        assertEquals(tercera, apartamento.getImagenPrincipal().orElseThrow().id());
        assertEquals(1, apartamento.getImagenes().stream().filter(ImagenApartamento::principal).count());
    }

    @Test
    void noSePuedeElegirComoPrincipalUnaImagenQueNoExiste() {
        Apartamento apartamento = conImagenes(2);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> apartamento.establecerImagenPrincipal("no-existe"));

        assertTrue(e.getMessage().contains("no-existe"));
    }

    @Test
    void retirarEsLogicoLaImagenSaleDeLaGaleriaPeroSeConserva() {
        Apartamento apartamento = conImagenes(3);
        String segunda = apartamento.getImagenes().get(1).id();

        apartamento.retirarImagen(segunda);

        assertEquals(2, apartamento.cantidadImagenes());
        assertTrue(apartamento.getImagenes().stream().noneMatch(i -> i.id().equals(segunda)));
        assertEquals(3, apartamento.getTodasLasImagenes().size());
        assertTrue(apartamento.getTodasLasImagenes().stream().anyMatch(i -> i.id().equals(segunda) && i.retirada() && !i.principal()));
    }

    @Test
    void retirarUnaImagenLiberaUnCupo() {
        Apartamento apartamento = conImagenes(10);
        apartamento.retirarImagen(apartamento.getImagenes().get(5).id());

        apartamento.agregarImagenes(List.of(ImagenesDePrueba.referencia(11)));

        assertEquals(10, apartamento.cantidadImagenes());
        assertEquals(11, apartamento.getTodasLasImagenes().size());
    }

    @Test
    void siSeRetiraLaPrincipalLaPrimeraQueQuedaPasaASerlo() {
        Apartamento apartamento = conImagenes(3);
        String principal = apartamento.getImagenPrincipal().orElseThrow().id();
        String siguiente = apartamento.getImagenes().get(1).id();

        apartamento.retirarImagen(principal);

        assertEquals(siguiente, apartamento.getImagenPrincipal().orElseThrow().id());
        assertEquals(1, apartamento.getImagenes().stream().filter(ImagenApartamento::principal).count());
    }

    @Test
    void retirarUnaImagenQueNoExisteOYaFueRetiradaSeRechaza() {
        Apartamento apartamento = conImagenes(2);
        String id = apartamento.getImagenes().get(1).id();
        apartamento.retirarImagen(id);

        assertThrows(ReglaDominioException.class, () -> apartamento.retirarImagen(id));
        assertThrows(ReglaDominioException.class, () -> apartamento.retirarImagen("no-existe"));
    }

    @Test
    void unApartamentoActivoNoPuedeQuedarseSinImagenes() {
        Apartamento apartamento = conImagenes(1);
        apartamento.activar();

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> apartamento.retirarImagen(apartamento.getImagenPrincipal().orElseThrow().id()));

        assertTrue(e.getMessage().startsWith("Un apartamento activo debe conservar al menos una imagen"));
        assertEquals(1, apartamento.cantidadImagenes());
    }

    @Test
    void unApartamentoActivoPuedeRetirarImagenesMientrasLeQuedeAlgunaOtra() {
        Apartamento apartamento = conImagenes(2);
        apartamento.activar();

        apartamento.retirarImagen(apartamento.getImagenPrincipal().orElseThrow().id());

        assertEquals(1, apartamento.cantidadImagenes());
        assertTrue(apartamento.getImagenPrincipal().isPresent());
    }

    @Test
    void unApartamentoInactivoSiPuedeQuedarseSinImagenes() {
        Apartamento apartamento = conImagenes(1);

        apartamento.retirarImagen(apartamento.getImagenPrincipal().orElseThrow().id());

        assertEquals(0, apartamento.cantidadImagenes());
        assertTrue(apartamento.getImagenPrincipal().isEmpty());
    }

    @Test
    void despuesDeQuedarSinImagenesLaSiguienteQueSeAgregaEsLaPrincipal() {
        Apartamento apartamento = conImagenes(1);
        apartamento.retirarImagen(apartamento.getImagenPrincipal().orElseThrow().id());

        apartamento.agregarImagenes(List.of(ImagenesDePrueba.referencia(2)));

        assertTrue(apartamento.getImagenPrincipal().orElseThrow().url().endsWith("prueba-2.png"));
    }

    @Test
    void laGaleriaQueSeEntregaNoSePuedeAlterarDesdeFuera() {
        Apartamento apartamento = conImagenes(2);

        assertThrows(UnsupportedOperationException.class, () -> apartamento.getImagenes().clear());
        assertThrows(UnsupportedOperationException.class, () -> apartamento.getTodasLasImagenes().clear());
        assertEquals(2, apartamento.cantidadImagenes());
    }

    // ---------- reconstruir: las imágenes guardadas deben ser coherentes ----------

    private static Apartamento reconstruir(List<ImagenApartamento> imagenes) {
        return Apartamento.reconstruir(new IdentificacionApartamento("APT-101"), "Vista", ApartamentoDePrueba.DESCRIPCION,
                2, 4, List.of(), List.of(), EstadoOperativo.PREPARADO, true, imagenes);
    }

    private static ImagenApartamento imagen(String id, boolean principal, boolean retirada) {
        return new ImagenApartamento(id, "http://x/" + id, "ref/" + id, principal, retirada);
    }

    @Test
    void reconstruirRestauraLasImagenesIncluidasLasRetiradas() {
        Apartamento apartamento = reconstruir(List.of(imagen("a", true, false), imagen("b", false, true), imagen("c", false, false)));

        assertEquals(2, apartamento.cantidadImagenes());
        assertEquals(3, apartamento.getTodasLasImagenes().size());
        assertEquals("a", apartamento.getImagenPrincipal().orElseThrow().id());
    }

    @Test
    void reconstruirSinImagenesEsValido() {
        assertEquals(0, reconstruir(List.of()).cantidadImagenes());
        assertEquals(0, reconstruir(null).cantidadImagenes());
    }

    @Test
    void reconstruirRechazaImagenesSinPrincipalOConDos() {
        assertThrows(ReglaDominioException.class, () -> reconstruir(List.of(imagen("a", false, false))));
        assertThrows(ReglaDominioException.class,
                () -> reconstruir(List.of(imagen("a", true, false), imagen("b", true, false))));
    }

    @Test
    void reconstruirRechazaMasDeDiezImagenesSinRetirar() {
        List<ImagenApartamento> once = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            once.add(imagen("i" + i, i == 0, false));
        }

        assertThrows(ReglaDominioException.class, () -> reconstruir(once));
    }

    @Test
    void reconstruirRechazaIdentificadoresRepetidos() {
        assertThrows(ReglaDominioException.class,
                () -> reconstruir(List.of(imagen("a", true, false), imagen("a", false, false))));
    }

    @Test
    void lasImagenesRetiradasNoCuentanParaElLimiteAlReconstruir() {
        List<ImagenApartamento> muchas = new ArrayList<>();
        muchas.add(imagen("p", true, false));
        for (int i = 0; i < 12; i++) {
            muchas.add(imagen("r" + i, false, true));
        }

        assertEquals(1, reconstruir(muchas).cantidadImagenes());
    }

    @Test
    void unaImagenRetiradaNoPuedeSerLaPrincipal() {
        assertThrows(ReglaDominioException.class, () -> new ImagenApartamento("a", "u", "r", true, true));
    }

    @Test
    void laImagenExigeIdentificadorUrlYReferencia() {
        assertThrows(ReglaDominioException.class, () -> new ImagenApartamento(" ", "u", "r", false, false));
        assertThrows(ReglaDominioException.class, () -> new ImagenApartamento("a", null, "r", false, false));
        assertThrows(ReglaDominioException.class, () -> new ImagenApartamento("a", "u", "", false, false));
    }
}
