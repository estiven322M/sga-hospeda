package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ImagenApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;

/** Persistir no cambia el dominio: la galería (con su orden, la principal y las retiradas) viaja a la base y vuelve igual. */
@DataJpaTest
@Import({ApartamentoJpaRepository.class, ApartamentoPersistenceMapper.class})
class ApartamentoImagenesPersistenciaTest {

    @Autowired private ApartamentoJpaRepository repositorio;

    private static final IdentificacionApartamento ID = new IdentificacionApartamento("APT-101");

    private Apartamento conImagenes(int cuantas) {
        Apartamento apartamento = ApartamentoDePrueba.nuevo(ID, "Vista", 2, 4);
        if (cuantas > 0) {
            apartamento.agregarImagenes(IntStream.rangeClosed(1, cuantas).mapToObj(ImagenesDePrueba::referencia).toList());
        }
        return apartamento;
    }

    @Test
    void sinImagenesVuelveSinImagenes() {
        repositorio.guardar(conImagenes(0));

        Apartamento leido = repositorio.buscarPorIdentificacion(ID).orElseThrow();

        assertEquals(0, leido.cantidadImagenes());
        assertTrue(leido.getImagenPrincipal().isEmpty());
    }

    @Test
    void laGaleriaVuelveEnElMismoOrdenConSuPrincipalYSusDatos() {
        Apartamento original = conImagenes(4);
        repositorio.guardar(original);

        Apartamento leido = repositorio.buscarPorIdentificacion(ID).orElseThrow();

        assertEquals(original.getImagenes(), leido.getImagenes());
        assertEquals(original.getImagenPrincipal().orElseThrow(), leido.getImagenPrincipal().orElseThrow());
        ImagenApartamento primera = leido.getImagenes().get(0);
        assertEquals("http://localhost:8080/imagenes/apartamentos/APT-101/prueba-1.png", primera.url());
        assertEquals("apartamentos/APT-101/prueba-1.png", primera.referencia());
    }

    @Test
    void laImagenRetiradaSeConservaPeroNoApareceEnLaGaleria() {
        Apartamento apartamento = conImagenes(3);
        String retirada = apartamento.getImagenes().get(1).id();
        apartamento.retirarImagen(retirada);
        repositorio.guardar(apartamento);

        Apartamento leido = repositorio.buscarPorIdentificacion(ID).orElseThrow();

        assertEquals(2, leido.cantidadImagenes());
        assertEquals(3, leido.getTodasLasImagenes().size());
        assertTrue(leido.getTodasLasImagenes().stream().anyMatch(i -> i.id().equals(retirada) && i.retirada()));
    }

    @Test
    void cambiarLaPrincipalYAgregarMasSeActualizaSinDuplicar() {
        Apartamento apartamento = repositorio.guardar(conImagenes(2));
        apartamento.establecerImagenPrincipal(apartamento.getImagenes().get(1).id());
        apartamento.agregarImagenes(List.of(ImagenesDePrueba.referencia(3)));
        repositorio.guardar(apartamento);

        Apartamento leido = repositorio.buscarPorIdentificacion(ID).orElseThrow();

        assertEquals(3, leido.cantidadImagenes());
        assertEquals(1, leido.getImagenes().stream().filter(ImagenApartamento::principal).count());
        assertEquals(apartamento.getImagenes().get(1).id(), leido.getImagenPrincipal().orElseThrow().id());
    }

    @Test
    void elListadoTraeLaGaleriaDeCadaApartamento() {
        for (String id : List.of("APT-201", "APT-202")) {
            Apartamento apartamento = ApartamentoDePrueba.nuevo(new IdentificacionApartamento(id), "Apto " + id, 2, 4);
            apartamento.agregarImagenes(List.of(ImagenesDePrueba.referencia(1), ImagenesDePrueba.referencia(2)));
            apartamento.activar();
            repositorio.guardar(apartamento);
        }

        var pagina = repositorio.listarTodos(co.edu.uniquindio.sga.domain.repository.SolicitudPagina.primera());

        assertEquals(2, pagina.contenido().size());
        assertTrue(pagina.contenido().stream().allMatch(a -> a.cantidadImagenes() == 2 && a.getImagenPrincipal().isPresent()));
    }
}
