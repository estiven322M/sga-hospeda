package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.service.ActivacionApartamentoService;
import co.edu.uniquindio.sga.domain.service.RegistroApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Gestión de apartamentos: crear, consultar, listar, editar, activar y dar de baja (lógica). */
@ExtendWith(MockitoExtension.class)
class ApartamentoUseCasesTest {

    @Mock private ApartamentoRepository repositorio;
    @Mock private ActivacionApartamentoService activacionService;

    private final IdentificacionApartamento id = new IdentificacionApartamento("APT-101");
    private Apartamento apartamento;

    @BeforeEach
    void setUp() {
        apartamento = new Apartamento(id, "Vista", 2, 4);
        lenient().when(repositorio.buscarPorIdentificacion(id)).thenReturn(Optional.of(apartamento));
        lenient().when(repositorio.guardar(any(Apartamento.class))).then(returnsFirstArg());
    }

    private CrearApartamentoUseCase crear() {
        return new CrearApartamentoUseCase(repositorio, new RegistroApartamentoService(repositorio));
    }

    // ---------- Crear ----------

    @Test
    void creaUnApartamentoNuevoInactivoYLoGuarda() {
        IdentificacionApartamento nuevo = new IdentificacionApartamento("apt-202");

        Apartamento creado = crear().ejecutar(nuevo, "Patio", 1, 2);

        assertEquals("APT-202", creado.getIdentificacion().valor());
        assertFalse(creado.isActivo()); // 7.4
        verify(repositorio).guardar(creado);
    }

    @Test
    void rechazaUnaIdentificacionRepetidaL04YNoGuarda() {
        assertThrows(ReglaDominioException.class, () -> crear().ejecutar(id, "Otro", 1, 2));

        verify(repositorio, never()).guardar(any());
    }

    @Test
    void rechazaDatosInvalidosDelNegocio() {
        assertThrows(ReglaDominioException.class,
                () -> crear().ejecutar(new IdentificacionApartamento("APT-203"), "X", 1, 0));
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
        when(repositorio.listarTodos(SolicitudPagina.primera())).thenReturn(pagina);

        assertSame(pagina, new ListarApartamentosUseCase(repositorio).ejecutar(SolicitudPagina.primera()));
    }

    // ---------- Actualizar ----------

    @Test
    void actualizaLosDatosYGuarda() {
        Apartamento resultado = new ActualizarApartamentoUseCase(repositorio).ejecutar(id, "Nuevo", 3, 6);

        assertEquals("Nuevo", resultado.getNombre());
        assertEquals(6, resultado.getCapacidad());
        verify(repositorio).guardar(apartamento);
    }

    @Test
    void noActualizaConDatosInvalidosNiSiNoExiste() {
        assertThrows(ReglaDominioException.class,
                () -> new ActualizarApartamentoUseCase(repositorio).ejecutar(id, "X", 0, 4));

        IdentificacionApartamento otro = new IdentificacionApartamento("APT-999");
        when(repositorio.buscarPorIdentificacion(otro)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class,
                () -> new ActualizarApartamentoUseCase(repositorio).ejecutar(otro, "X", 1, 1));

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
