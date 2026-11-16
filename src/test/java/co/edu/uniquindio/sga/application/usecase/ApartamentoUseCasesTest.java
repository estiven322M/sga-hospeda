package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FiltroApartamentos;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.service.ActivacionApartamentoService;
import co.edu.uniquindio.sga.domain.service.ControlCapacidadService;
import co.edu.uniquindio.sga.domain.service.RegistroApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Gestión de apartamentos: crear, consultar, listar, editar, activar y dar de baja (lógica). */
@ExtendWith(MockitoExtension.class)
class ApartamentoUseCasesTest {

    @Mock private ApartamentoRepository repositorio;
    @Mock private ActivacionApartamentoService activacionService;
    @Mock private ControlCapacidadService controlCapacidad;

    private final IdentificacionApartamento id = new IdentificacionApartamento("APT-101");
    private Apartamento apartamento;

    @BeforeEach
    void setUp() {
        apartamento = ApartamentoDePrueba.nuevo(id, "Vista", 2, 4);
        lenient().when(repositorio.buscarPorIdentificacion(id)).thenReturn(Optional.of(apartamento));
        lenient().when(repositorio.guardar(any(Apartamento.class))).then(returnsFirstArg());
    }

    private ActualizarApartamentoUseCase actualizar() {
        return new ActualizarApartamentoUseCase(repositorio, controlCapacidad,
                Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Bogota")));
    }

    private CrearApartamentoUseCase crear() {
        return new CrearApartamentoUseCase(repositorio, new RegistroApartamentoService(repositorio));
    }

    // ---------- Crear ----------

    @Test
    void creaUnApartamentoNuevoInactivoYLoGuarda() {
        IdentificacionApartamento nuevo = new IdentificacionApartamento("apt-202");

        Apartamento creado = crear().ejecutar(nuevo, "Patio", "Patio interior con luz natural", 1, 2,
                List.of("Nevera", "Wi-Fi"), Set.of(Caracteristica.BALCON));

        assertEquals("APT-202", creado.getIdentificacion().valor());
        assertEquals("Patio interior con luz natural", creado.getDescripcion());
        assertEquals(List.of("Nevera", "Wi-Fi"), creado.getDotacion());
        assertEquals(Set.of(Caracteristica.BALCON), creado.getCaracteristicas());
        assertFalse(creado.isActivo()); // 7.4
        verify(repositorio).guardar(creado);
    }

    @Test
    void rechazaUnaIdentificacionRepetidaL04YNoGuarda() {
        assertThrows(ReglaDominioException.class, () -> crear().ejecutar(id, "Otro", "Descripción", 1, 2, List.of(), Set.of()));

        verify(repositorio, never()).guardar(any());
    }

    @Test
    void rechazaDatosInvalidosDelNegocio() {
        assertThrows(ReglaDominioException.class,
                () -> crear().ejecutar(new IdentificacionApartamento("APT-203"), "X", "Descripción", 1, 0, List.of(), Set.of()));
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void rechazaUnaDescripcionVaciaOUnaDotacionRepetidaL06YNoGuarda() {
        var nuevo = new IdentificacionApartamento("APT-204");
        assertThrows(ReglaDominioException.class,
                () -> crear().ejecutar(nuevo, "X", "  ", 1, 2, List.of(), Set.of()));
        assertThrows(ReglaDominioException.class,
                () -> crear().ejecutar(nuevo, "X", "Descripción", 1, 2, List.of("Nevera", "nevera"), Set.of()));
        verify(repositorio, never()).guardar(any());
    }

    // ---------- Consultar y listar ----------

    @Test
    void consultaUnApartamentoYLanzaNoEncontradoSiNoExiste() {
        assertSame(apartamento, new ConsultarApartamentoUseCase(repositorio).ejecutar(id));

        IdentificacionApartamento otro = new IdentificacionApartamento("APT-999");
        when(repositorio.buscarPorIdentificacion(otro)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class,
                () -> new ConsultarApartamentoUseCase(repositorio).ejecutar(otro));
    }

    @Test
    void listaTodosLosApartamentosPaginados() {
        Pagina<Apartamento> pagina = new Pagina<>(List.of(apartamento), 0, 1);
        when(repositorio.listarTodos(FiltroApartamentos.SIN_FILTRO, SolicitudPagina.primera())).thenReturn(pagina);

        assertSame(pagina, new ListarApartamentosUseCase(repositorio)
                .ejecutar(FiltroApartamentos.SIN_FILTRO, SolicitudPagina.primera()));
    }

    @Test
    void listaConFiltroDeCaracteristicasDelegandoElFiltroEnElPuertoL06() {
        var filtro = FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA));
        Pagina<Apartamento> pagina = new Pagina<>(List.of(apartamento), 0, 1);
        when(repositorio.listarTodos(filtro, SolicitudPagina.primera())).thenReturn(pagina);

        assertSame(pagina, new ListarApartamentosUseCase(repositorio).ejecutar(filtro, SolicitudPagina.primera()));
    }

    // ---------- Actualizar ----------

    @Test
    void actualizaLosDatosYGuarda() {
        Apartamento resultado = actualizar().ejecutar(id, "Nuevo",
                "Descripción nueva", 3, 6, List.of("Lavadora"), Set.of(Caracteristica.JACUZZI)).apartamento();

        assertEquals("Nuevo", resultado.getNombre());
        assertEquals("Descripción nueva", resultado.getDescripcion());
        assertEquals(List.of("Lavadora"), resultado.getDotacion());
        assertEquals(Set.of(Caracteristica.JACUZZI), resultado.getCaracteristicas());
        assertEquals(6, resultado.getCapacidad());
        verify(repositorio).guardar(apartamento);
    }

    /** 7.3: al BAJAR la capacidad se avisa de las reservas que quedan por encima, y la actualización no se bloquea. */
    @Test
    void alBajarLaCapacidadAvisaDeLasReservasQueQuedanPorEncimaSinBloquear() {
        Reserva afectada = mock(Reserva.class);
        when(controlCapacidad.reservasPorEncimaDeLaCapacidad(any(Apartamento.class), eq(java.time.LocalDate.of(2026, 11, 16))))
                .thenReturn(List.of(afectada));

        var resultado = actualizar().ejecutar(id, "Vista", "Descripción", 2, 2, List.of(), Set.of());

        assertEquals(2, resultado.apartamento().getCapacidad()); // se actualizó: no se bloquea
        assertEquals(List.of(afectada), resultado.reservasPorEncimaDeLaCapacidad());
        verify(repositorio).guardar(apartamento);
    }

    @Test
    void siLaCapacidadNoBajaNoHayAvisoNiSeConsultaLasReservas() {
        var resultado = actualizar().ejecutar(id, "Vista", "Descripción", 2, 6, List.of(), Set.of());

        assertTrue(resultado.reservasPorEncimaDeLaCapacidad().isEmpty());
        verifyNoInteractions(controlCapacidad);
    }

    @Test
    void noActualizaConDatosInvalidosNiSiNoExiste() {
        assertThrows(ReglaDominioException.class,
                () -> actualizar().ejecutar(id, "X", "Descripción", 0, 4, List.of(), Set.of()));

        IdentificacionApartamento otro = new IdentificacionApartamento("APT-999");
        when(repositorio.buscarPorIdentificacion(otro)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class,
                () -> actualizar().ejecutar(otro, "X", "Descripción", 1, 1, List.of(), Set.of()));

        verify(repositorio, never()).guardar(any());
    }

    // ---------- Activar ----------

    @Test
    void activaDelegandoEnElServicioDeDominioYGuarda() {
        doAnswer(inv -> { ((Apartamento) inv.getArgument(0)).activar(); return null; })
                .when(activacionService).activar(apartamento);

        Apartamento resultado = new ActivarApartamentoUseCase(repositorio, activacionService).ejecutar(id);

        assertTrue(resultado.isActivo());
        verify(repositorio).guardar(apartamento);
    }

    @Test
    void noGuardaSiFaltanTarifasOElApartamentoNoExiste() {
        doThrow(new ReglaDominioException("faltan tarifas para las temporadas ALTA"))
                .when(activacionService).activar(apartamento);
        var useCase = new ActivarApartamentoUseCase(repositorio, activacionService);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id));

        IdentificacionApartamento otro = new IdentificacionApartamento("APT-999");
        when(repositorio.buscarPorIdentificacion(otro)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class, () -> useCase.ejecutar(otro));

        verify(repositorio, never()).guardar(any());
    }

    // ---------- Baja lógica ----------

    @Test
    void daDeBajaLogicamenteYNuncaEliminaFisicamente() {
        apartamento.activar();

        Apartamento resultado = new DarDeBajaApartamentoUseCase(repositorio).ejecutar(id);

        assertFalse(resultado.isActivo());
        verify(repositorio).guardar(apartamento);
    }

    @Test
    void noSePuedeDarDeBajaUnApartamentoYaDadoDeBaja() {
        // nace inactivo: ya está "dado de baja" hasta que se active
        assertThrows(ReglaDominioException.class, () -> new DarDeBajaApartamentoUseCase(repositorio).ejecutar(id));

        verify(repositorio, never()).guardar(any());
    }
}
