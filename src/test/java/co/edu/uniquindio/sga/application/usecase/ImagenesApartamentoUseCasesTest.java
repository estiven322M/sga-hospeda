package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ImagenNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.AlmacenamientoImagenesException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlmacenImagenes;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.ValidadorImagenes;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;

/**
 * 7.3: subir, elegir la principal y retirar imágenes. Se comprueba el ORDEN de lo que orquesta el caso de uso: el dominio decide
 * (cupo y tamaño) antes de tocar el almacén, y un fallo del almacén no deja nada registrado en el apartamento.
 */
class ImagenesApartamentoUseCasesTest {

    private static final long MB = 1024L * 1024L;
    private static final IdentificacionApartamento ID = new IdentificacionApartamento("APT-101");

    private final ApartamentoRepository repositorio = mock(ApartamentoRepository.class);
    private final AlmacenImagenes almacen = mock(AlmacenImagenes.class);
    private final ValidadorImagenes validador = new ValidadorImagenes(5 * MB, 2 * MB);
    private final AgregarImagenesApartamentoUseCase agregar = new AgregarImagenesApartamentoUseCase(repositorio, validador, almacen);
    private final EstablecerImagenPrincipalUseCase principal = new EstablecerImagenPrincipalUseCase(repositorio);
    private final RetirarImagenApartamentoUseCase retirar = new RetirarImagenApartamentoUseCase(repositorio);

    private Apartamento apartamentoConImagenes(int cuantas) {
        Apartamento apartamento = ApartamentoDePrueba.nuevo(ID, "Vista", 2, 4);
        if (cuantas > 0) {
            apartamento.agregarImagenes(IntStream.rangeClosed(1, cuantas).mapToObj(ImagenesDePrueba::referencia).toList());
        }
        when(repositorio.buscarPorIdentificacion(ID)).thenReturn(Optional.of(apartamento));
        when(repositorio.guardar(any(Apartamento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        return apartamento;
    }

    private static List<ContenidoImagen> imagenes(int cuantas) {
        return IntStream.rangeClosed(1, cuantas).mapToObj(i -> ImagenesDePrueba.contenidoPng("foto" + i + ".png")).toList();
    }

    // ---------- subir ----------

    @Test
    void subeCadaImagenAlAlmacenEnLaCarpetaDelApartamentoYLasRegistra() {
        apartamentoConImagenes(0);
        when(almacen.guardar(any(), eq("apartamentos/APT-101"))).thenReturn(ImagenesDePrueba.referencia(1), ImagenesDePrueba.referencia(2));

        Apartamento resultado = agregar.ejecutar(ID, imagenes(2));

        assertEquals(2, resultado.cantidadImagenes());
        assertTrue(resultado.getImagenes().get(0).principal(), "la primera de un apartamento sin imágenes queda como principal");
        var orden = inOrder(almacen, repositorio);
        orden.verify(almacen, org.mockito.Mockito.times(2)).guardar(any(), eq("apartamentos/APT-101"));
        orden.verify(repositorio).guardar(resultado);
    }

    @Test
    void siElLoteNoCabeNoSeSubeNadaAlAlmacen() {
        apartamentoConImagenes(8);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> agregar.ejecutar(ID, imagenes(3)));

        assertTrue(e.getMessage().contains("como máximo 10"));
        verify(almacen, never()).guardar(any(), any());
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void siUnaImagenPesaDeMasNoSeSubeNingunaDelLote() {
        apartamentoConImagenes(0);
        ContenidoImagen grande = ContenidoImagen.de("grande.png", ImagenesDePrueba.pngDe((int) (5 * MB) + 1));

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> agregar.ejecutar(ID, List.of(ImagenesDePrueba.contenidoPng("buena.png"), grande)));

        assertTrue(e.getMessage().contains("grande.png pesa más de 5 MB"));
        verify(almacen, never()).guardar(any(), any());
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void siElAlmacenFallaNoSeRegistraNingunaImagenDelLote() {
        Apartamento apartamento = apartamentoConImagenes(1);
        when(almacen.guardar(any(), any())).thenReturn(ImagenesDePrueba.referencia(2))
                .thenThrow(new AlmacenamientoImagenesException("tiempo agotado"));

        assertThrows(AlmacenamientoImagenesException.class, () -> agregar.ejecutar(ID, imagenes(2)));

        assertEquals(1, apartamento.cantidadImagenes(), "el apartamento no quedó a medias");
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void sinImagenesEnElLoteSeRechaza() {
        apartamentoConImagenes(0);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> agregar.ejecutar(ID, List.of()));

        assertEquals("Debe enviar al menos una imagen", e.getMessage());
    }

    @Test
    void unApartamentoQueNoExisteEsUn404() {
        when(repositorio.buscarPorIdentificacion(ID)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class, () -> agregar.ejecutar(ID, imagenes(1)));
        assertThrows(ApartamentoNoEncontradoException.class, () -> principal.ejecutar(ID, "x"));
        assertThrows(ApartamentoNoEncontradoException.class, () -> retirar.ejecutar(ID, "x"));
        verify(almacen, never()).guardar(any(), any());
    }

    // ---------- principal y retirar ----------

    @Test
    void elegirLaPrincipalGuardaElApartamento() {
        Apartamento apartamento = apartamentoConImagenes(3);
        String tercera = apartamento.getImagenes().get(2).id();

        Apartamento resultado = principal.ejecutar(ID, tercera);

        assertEquals(tercera, resultado.getImagenPrincipal().orElseThrow().id());
        verify(repositorio).guardar(apartamento);
    }

    @Test
    void unaImagenQueNoExisteEsUn404AlElegirlaOAlRetirarla() {
        apartamentoConImagenes(2);

        assertThrows(ImagenNoEncontradaException.class, () -> principal.ejecutar(ID, "no-existe"));
        assertThrows(ImagenNoEncontradaException.class, () -> retirar.ejecutar(ID, "no-existe"));
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void retirarEsLogicoYGuardaElApartamento() {
        Apartamento apartamento = apartamentoConImagenes(2);
        String id = apartamento.getImagenes().get(1).id();

        Apartamento resultado = retirar.ejecutar(ID, id);

        assertEquals(1, resultado.cantidadImagenes());
        assertEquals(2, resultado.getTodasLasImagenes().size());
        verify(repositorio).guardar(apartamento);
    }

    @Test
    void noSePuedeRetirarLaUltimaImagenDeUnApartamentoActivo() {
        Apartamento apartamento = apartamentoConImagenes(1);
        apartamento.activar();

        assertThrows(ReglaDominioException.class, () -> retirar.ejecutar(ID, apartamento.getImagenPrincipal().orElseThrow().id()));

        verify(repositorio, never()).guardar(any());
    }

    @Test
    void unaImagenYaRetiradaYaNoSePuedeElegirNiRetirar() {
        Apartamento apartamento = apartamentoConImagenes(2);
        String id = apartamento.getImagenes().get(1).id();
        retirar.ejecutar(ID, id);

        assertThrows(ImagenNoEncontradaException.class, () -> principal.ejecutar(ID, id));
        assertThrows(ImagenNoEncontradaException.class, () -> retirar.ejecutar(ID, id));
    }

    @Test
    void laReferenciaQueDevuelveElAlmacenSeGuardaTalCual() {
        apartamentoConImagenes(0);
        ReferenciaImagen referencia = new ReferenciaImagen("https://res.cloudinary.com/demo/image/upload/v1/sga/x.png", "sga/apartamentos/APT-101/x");
        when(almacen.guardar(any(), any())).thenReturn(referencia);

        Apartamento resultado = agregar.ejecutar(ID, imagenes(1));

        assertEquals(referencia.url(), resultado.getImagenPrincipal().orElseThrow().url());
        assertEquals(referencia.referencia(), resultado.getImagenPrincipal().orElseThrow().referencia());
    }
}
