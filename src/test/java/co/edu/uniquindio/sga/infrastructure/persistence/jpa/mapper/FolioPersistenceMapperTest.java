package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.Ajuste;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolioPersistenceMapperTest {

    private static final CodigoReserva RESERVA = new CodigoReserva("RES-2026-00001");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    private final FolioPersistenceMapper mapper = new FolioPersistenceMapper();

    private static Folio folioAbierto() {
        return new Folio(RESERVA, new Cargo("Alojamiento", Dinero.de(400_000)));
    }

    @Test
    void ida_y_vuelta_conserva_cargos_ajustes_y_pagos_en_el_orden_de_registro() {
        Folio original = folioAbierto();
        original.registrarCargo(new Cargo("Minibar", Dinero.de(25_000)));
        original.registrarCargo(new Cargo("Lavandería", Dinero.de(18_000)));
        original.registrarAjuste(new Ajuste("Descuento", Dinero.de(-30_000)));
        original.registrarAjuste(new Ajuste("Recargo", Dinero.de(10_000)));
        original.registrarPago(new Pago("EFECTIVO", FECHA, Dinero.de(100_000)));
        original.registrarPago(new Pago("TARJETA", FECHA.plusDays(1), Dinero.de(50_000)));

        Folio vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertEquals(original.obtenerCargos(), vuelta.obtenerCargos());
        assertEquals(original.obtenerAjustes(), vuelta.obtenerAjustes());
        assertEquals(original.obtenerPagos(), vuelta.obtenerPagos());
        assertEquals(original.saldo(), vuelta.saldo());
        assertFalse(vuelta.estaCerrado());
    }

    @Test
    void folio_solo_con_el_cargo_de_alojamiento_vuelve_sin_ajustes_ni_pagos() {
        Folio vuelta = mapper.aDominio(mapper.aEntidad(folioAbierto()));

        assertEquals(1, vuelta.obtenerCargos().size());
        assertTrue(vuelta.obtenerAjustes().isEmpty());
        assertTrue(vuelta.obtenerPagos().isEmpty());
        assertEquals(RESERVA, vuelta.getReserva());
    }

    @Test
    void folio_cerrado_con_saldo_conserva_la_autorizacion() {
        Folio original = folioAbierto();
        original.cerrar("Autoriza gerencia: pagará mañana");

        Folio vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertTrue(vuelta.estaCerrado());
        assertEquals("Autoriza gerencia: pagará mañana", vuelta.getAutorizacionCierre());
        assertEquals(Dinero.de(400_000), vuelta.saldo());
    }

    @Test
    void folio_cerrado_sin_saldo_conserva_la_autorizacion_nula() {
        Folio original = folioAbierto();
        original.registrarPago(new Pago("EFECTIVO", FECHA, Dinero.de(400_000)));
        original.cerrar(null);

        Folio vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertTrue(vuelta.estaCerrado());
        assertNull(vuelta.getAutorizacionCierre());
    }

    @Test
    void folio_de_una_cancelacion_conserva_el_reverso_y_la_penalidad() {
        Folio original = folioAbierto();
        original.registrarPago(new Pago("TARJETA", FECHA, Dinero.de(120_000)));
        original.liquidarCancelacion(Dinero.de(40_000));

        Folio vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertEquals(original.obtenerAjustes(), vuelta.obtenerAjustes());
        assertEquals(original.obtenerCargos(), vuelta.obtenerCargos());
        assertEquals(Dinero.de(-80_000), vuelta.saldo());
    }

    @Test
    void un_folio_guardado_sin_cargos_es_un_dato_inconsistente() {
        FolioEntity sinCargos = new FolioEntity(RESERVA.valor(), false, null, List.of(), List.of(), List.of());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> mapper.aDominio(sinCargos));

        assertTrue(error.getMessage().contains("RES-2026-00001"));
    }
}
