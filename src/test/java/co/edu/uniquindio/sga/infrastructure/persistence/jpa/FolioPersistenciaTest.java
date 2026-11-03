package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.Ajuste;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.FolioPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({FolioJpaRepository.class, FolioPersistenceMapper.class})
class FolioPersistenciaTest {

    private static final CodigoReserva RESERVA = new CodigoReserva("RES-2026-00001");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private FolioJpaRepository repositorio;
    @Autowired
    private EntityManager em;

    private static Folio folioAbierto(CodigoReserva reserva) {
        return new Folio(reserva, new Cargo("Alojamiento", Dinero.de(400_000)));
    }

    private void sincronizarYLimpiar() {
        em.flush();
        em.clear();
    }

    @Test
    void guardar_y_buscar_por_reserva_devuelve_el_folio_con_sus_movimientos() {
        Folio folio = folioAbierto(RESERVA);
        folio.registrarPago(new Pago("EFECTIVO", FECHA, Dinero.de(120_000)));
        repositorio.guardar(folio);
        sincronizarYLimpiar();

        Folio leido = repositorio.buscarPorReserva(RESERVA).orElseThrow();

        assertEquals(folio.obtenerCargos(), leido.obtenerCargos());
        assertEquals(folio.obtenerPagos(), leido.obtenerPagos());
        assertEquals(Dinero.de(280_000), leido.saldo());
    }

    @Test
    void buscar_el_folio_de_una_reserva_sin_folio_devuelve_vacio() {
        assertTrue(repositorio.buscarPorReserva(RESERVA).isEmpty());
    }

    @Test
    void guardar_un_folio_existente_agrega_movimientos_sin_duplicar_y_conserva_el_orden() {
        Folio folio = repositorio.guardar(folioAbierto(RESERVA));
        folio.registrarCargo(new Cargo("Minibar", Dinero.de(25_000)));
        folio.registrarAjuste(new Ajuste("Descuento", Dinero.de(-5_000)));
        folio.registrarPago(new Pago("EFECTIVO", FECHA, Dinero.de(100_000)));
        repositorio.guardar(folio);
        folio.registrarPago(new Pago("TARJETA", FECHA.plusDays(1), Dinero.de(50_000)));
        repositorio.guardar(folio);
        sincronizarYLimpiar();

        Folio leido = repositorio.buscarPorReserva(RESERVA).orElseThrow();

        assertEquals(2, leido.obtenerCargos().size());
        assertEquals("Minibar", leido.obtenerCargos().get(1).concepto());
        assertEquals(1, leido.obtenerAjustes().size());
        assertEquals(java.util.List.of("EFECTIVO", "TARJETA"),
                leido.obtenerPagos().stream().map(Pago::medio).toList());
        assertEquals(1L, em.createQuery("select count(f) from FolioEntity f", Long.class).getSingleResult());
    }

    @Test
    void cerrar_el_folio_persiste_el_cierre_y_su_autorizacion() {
        Folio folio = repositorio.guardar(folioAbierto(RESERVA));
        folio.cerrar("Autoriza gerencia");
        repositorio.guardar(folio);
        sincronizarYLimpiar();

        Folio leido = repositorio.buscarPorReserva(RESERVA).orElseThrow();

        assertTrue(leido.estaCerrado());
        assertEquals("Autoriza gerencia", leido.getAutorizacionCierre());
    }

    @Test
    void cada_reserva_tiene_su_propio_folio() {
        CodigoReserva otra = new CodigoReserva("RES-2026-00002");
        repositorio.guardar(folioAbierto(RESERVA));
        repositorio.guardar(new Folio(otra, new Cargo("Alojamiento", Dinero.de(900_000))));
        sincronizarYLimpiar();

        assertEquals(Dinero.de(900_000), repositorio.buscarPorReserva(otra).orElseThrow().saldo());
        assertEquals(Dinero.de(400_000), repositorio.buscarPorReserva(RESERVA).orElseThrow().saldo());
    }
}
