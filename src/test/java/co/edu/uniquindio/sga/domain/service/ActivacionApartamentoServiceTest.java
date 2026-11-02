package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

/** 7.4: no se activa un apartamento sin tarifas completas en todas las temporadas. */
class ActivacionApartamentoServiceTest {

    private final TarifarioRepository tarifario = mock(TarifarioRepository.class);
    private final TemporadaRepository temporadas = mock(TemporadaRepository.class);
    private final ActivacionApartamentoService servicio = new ActivacionApartamentoService(tarifario, temporadas);

    /** Un apartamento recién creado: nace inactivo (7.4). */
    private Apartamento apartamentoDadoDeBaja() {
        return new Apartamento(new IdentificacionApartamento("APT-101"), "Vista", 2, 4);
    }

    @Test
    void activaCuandoTieneTarifaEnTodasLasTemporadas() {
        Apartamento apt = apartamentoDadoDeBaja();
        when(temporadas.listarTodas()).thenReturn(List.of(
                new Temporada("BASE", true, List.of()),
                new Temporada("ALTA", false, List.of(new co.edu.uniquindio.sga.domain.valueobject.Periodo(
                        java.time.LocalDate.of(2026, 12, 15), java.time.LocalDate.of(2027, 1, 15))))));
        when(tarifario.listarTarifas(apt.getIdentificacion())).thenReturn(List.of(
                new Tarifa("BASE", Dinero.de(80_000)), new Tarifa("ALTA", Dinero.de(120_000))));

        servicio.activar(apt);

        assertTrue(apt.isActivo());
    }

    @Test
    void rechazaActivarSiFaltaLaTarifaDeUnaTemporada() {
        Apartamento apt = apartamentoDadoDeBaja();
        when(temporadas.listarTodas()).thenReturn(List.of(
                new Temporada("BASE", true, List.of()),
                new Temporada("ALTA", false, List.of(new co.edu.uniquindio.sga.domain.valueobject.Periodo(
                        java.time.LocalDate.of(2026, 12, 15), java.time.LocalDate.of(2027, 1, 15))))));
        when(tarifario.listarTarifas(apt.getIdentificacion()))
                .thenReturn(List.of(new Tarifa("BASE", Dinero.de(80_000))));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> servicio.activar(apt));

        assertTrue(ex.getMessage().contains("ALTA"));
        assertFalse(apt.isActivo());
    }
}
