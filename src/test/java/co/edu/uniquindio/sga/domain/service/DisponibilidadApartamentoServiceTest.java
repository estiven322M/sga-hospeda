package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DisponibilidadApartamentoServiceTest {
	
	// 1. ARRANGE: Creamos los Mocks de los puertos externos
    private final ReservaRepository reservaRepoMock = mock(ReservaRepository.class);
    private final BloqueoRepository bloqueoRepoMock = mock(BloqueoRepository.class);
    private final ConfiguracionAlojamiento configMock = mock(ConfiguracionAlojamiento.class);

    // 2. ARRANGE: Inyectamos los mocks al servicio real que vamos a probar
    private final DisponibilidadApartamentoService servicio =
            new DisponibilidadApartamentoService(reservaRepoMock, bloqueoRepoMock, configMock);

    // -- Métodos auxiliares --
    private Apartamento apartamentoBase() {
        return new Apartamento(new IdentificacionApartamento("APT-101"), "Vista", 2, 4);
    }

    private Ocupante titular() {
        return new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
                LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    }
    
 // Método auxiliar para evitar el error de ValorCongelado vacío
    private ValorCongelado valorValido() {
        return new ValorCongelado(List.of(
            new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100000), 1),
            new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100000), 1)
        ));
    }

    @Test
    void noDebeAdmitirMasOcupantesQueLaCapacidadRN02() {
        Apartamento apt = apartamentoBase(); // Capacidad 4
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));

        // Act & Assert
        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () ->
                servicio.verificarDisponible(apt, estancia, 5, null));
        
        assertEquals("El número de ocupantes excede la capacidad del apartamento", ex.getMessage());
        // ¡Ojo! Aquí no programamos mocks porque la regla salta ANTES de ir a la BD.
    }

    @Test
    void noDebePermitirSolapamientoConReservaActivaRN01() {
        Apartamento apt = apartamentoBase();
        Estancia nuevaEstancia = new Estancia(LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 15));
        
        Reserva reservaExistente = Reserva.crear(
                new CodigoReserva("RES-2026-00001"), apt.getIdentificacion(),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                titular(), List.of(titular()), CanalOrigen.PORTAL,
                valorValido(), // <-- Reemplazamos la lista vacía por un valor válido
                new VersionPolitica(1), LocalDateTime.of(2026, 1, 1, 10, 0)
        );

        when(reservaRepoMock.buscarActivasPorApartamento(apt.getIdentificacion()))
                .thenReturn(List.of(reservaExistente));
        when(configMock.tiempoPreparacion()).thenReturn(new TiempoPreparacion(0)); 

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () ->
                servicio.verificarDisponible(apt, nuevaEstancia, 2, null));
        
        assertEquals("El apartamento ya tiene una reserva activa que solapa esas noches", ex.getMessage());
    }

    @Test
    void noDebePermitirEntradaMismoDiaSiExigePreparacionRN20() {
        Apartamento apt = apartamentoBase();
        Estancia nuevaEstancia = new Estancia(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15));
        
        Reserva reservaExistente = Reserva.crear(
                new CodigoReserva("RES-2026-00001"), apt.getIdentificacion(),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                titular(), List.of(titular()), CanalOrigen.PORTAL,
                valorValido(), // <-- Reemplazamos la lista vacía por un valor válido
                new VersionPolitica(1), LocalDateTime.of(2026, 1, 1, 10, 0)
        );

        when(reservaRepoMock.buscarActivasPorApartamento(apt.getIdentificacion()))
                .thenReturn(List.of(reservaExistente));
        when(configMock.tiempoPreparacion()).thenReturn(new TiempoPreparacion(24)); 

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () ->
                servicio.verificarDisponible(apt, nuevaEstancia, 2, null));

        // Revisa que este mensaje coincida con el que pusiste en DisponibilidadApartamentoService
        assertEquals("No se respeta el tiempo de preparación configurado entre reservas", ex.getMessage());
    }

    @Test
    void debePermitirReservaSiTodoEstaLibre() {
        Apartamento apt = apartamentoBase();
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22));

        // Programamos los mocks para responder "No hay nada activo ni bloqueado"
        when(reservaRepoMock.buscarActivasPorApartamento(apt.getIdentificacion())).thenReturn(List.of());
        when(bloqueoRepoMock.buscarVigentesPorApartamento(apt.getIdentificacion())).thenReturn(List.of());
        when(configMock.tiempoPreparacion()).thenReturn(new TiempoPreparacion(0));

        // AssertDoesNotThrow: Garantiza que el método finaliza sin lanzar excepciones
        assertDoesNotThrow(() -> servicio.verificarDisponible(apt, estancia, 2, null));
    }

}
