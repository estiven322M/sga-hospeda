package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ApartamentoTest {

    private static final IdentificacionApartamento ID = new IdentificacionApartamento("APT-101");

    // Método auxiliar (Arrange) para no repetir la creación del objeto en cada test
    private Apartamento apartamentoBase() {
        return ApartamentoDePrueba.nuevo(ID, "Vista a la Montaña", 2, 4);
    }

    private Apartamento conEquipamiento() {
        return new Apartamento(ID, "Terraza", "Con terraza privada", 3, 6,
                List.of("Nevera", "Smart TV"), Set.of(Caracteristica.TERRAZA, Caracteristica.PARQUEADERO));
    }

    @Test
    void dosApartamentosConElMismoIdentificadorSonElMismo() {
        // La identidad depende del ID, no de los demás atributos
        Apartamento apt1 = ApartamentoDePrueba.nuevo(ID, "A", 1, 2);
        Apartamento apt2 = ApartamentoDePrueba.nuevo(ID, "B", 3, 5);

        assertEquals(apt1, apt2);
    }

    @Test
    void admiteOcupantesEnElLimiteExactoDeLaCapacidad() {
        Apartamento apt = apartamentoBase(); // Capacidad es 4

        // Act & Assert en los límites exactos (RN-02)
        assertTrue(apt.admite(4));  // Límite exacto permitido
        assertFalse(apt.admite(5)); // Excede
        assertFalse(apt.admite(0)); // Inválido
    }

    @Test
    void debeNacerInactivoYPreparado() {
        Apartamento apt = apartamentoBase();

        // 7.4: nace inactivo y solo se activa con tarifas completas (ActivacionApartamentoService)
        assertFalse(apt.isActivo());
        assertEquals(EstadoOperativo.PREPARADO, apt.getEstadoOperativo());
    }

    @Test
    void actualizarDatosCambiaNombreDormitoriosYCapacidad() {
        Apartamento apt = apartamentoBase();

        apt.actualizarDatos("Nuevo nombre", "Nueva descripción", 3, 6, List.of(), Set.of());

        assertEquals("Nuevo nombre", apt.getNombre());
        assertEquals(3, apt.getDormitorios());
        assertEquals(6, apt.getCapacidad());
    }

    @Test
    void actualizarDatosInvalidosNoModificaNada() {
        Apartamento apt = conEquipamiento();

        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos(" ", "D", 2, 4, List.of(), Set.of()));
        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos("X", "D", 0, 4, List.of(), Set.of()));
        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos("X", "D", 2, 0, List.of(), Set.of()));

        assertEquals("Terraza", apt.getNombre());
        assertEquals(6, apt.getCapacidad());
    }

    // ---------- L-06: descripción, dotación y características ----------

    @Test
    void guardaLaDescripcionLaDotacionYLasCaracteristicasL06() {
        Apartamento apt = conEquipamiento();

        assertEquals("Con terraza privada", apt.getDescripcion());
        assertEquals(List.of("Nevera", "Smart TV"), apt.getDotacion());
        assertEquals(Set.of(Caracteristica.TERRAZA, Caracteristica.PARQUEADERO), apt.getCaracteristicas());
    }

    @Test
    void laDescripcionEsObligatoriaYTieneUnTopeL06() {
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", null, 1, 2, List.of(), Set.of()));
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", "   ", 1, 2, List.of(), Set.of()));
        String exacta = "d".repeat(Apartamento.MAX_DESCRIPCION);
        assertEquals(Apartamento.MAX_DESCRIPCION,
                new Apartamento(ID, "X", exacta, 1, 2, List.of(), Set.of()).getDescripcion().length());
        String excedida = "d".repeat(Apartamento.MAX_DESCRIPCION + 1);
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", excedida, 1, 2, List.of(), Set.of()));
    }

    @Test
    void laDotacionYLasCaracteristicasPuedenEstarVaciasONulasL06() {
        Apartamento vacio = new Apartamento(ID, "X", "D", 1, 2, List.of(), Set.of());
        Apartamento nulos = new Apartamento(ID, "X", "D", 1, 2, null, null);

        assertTrue(vacio.getDotacion().isEmpty());
        assertTrue(nulos.getDotacion().isEmpty());
        assertTrue(nulos.getCaracteristicas().isEmpty());
    }

    @Test
    void laDotacionNoAdmiteElementosEnBlancoRepetidosNiDemasiadosL06() {
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", "D", 1, 2, Arrays.asList("Nevera", " "), Set.of()));
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", "D", 1, 2, Arrays.asList("Nevera", null), Set.of()));
        // repetido sin importar mayúsculas ni espacios sobrantes
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", "D", 1, 2, List.of("Nevera", " NEVERA "), Set.of()));
        assertThrows(ReglaDominioException.class, () -> new Apartamento(ID, "X", "D", 1, 2,
                List.of("e".repeat(Apartamento.MAX_LONGITUD_ITEM_DOTACION + 1)), Set.of()));

        List<String> treintaYUno = new ArrayList<>();
        for (int i = 0; i <= Apartamento.MAX_ITEMS_DOTACION; i++) {
            treintaYUno.add("Elemento " + i);
        }
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", "D", 1, 2, treintaYUno, Set.of()));
        // el tope exacto sí se admite
        assertEquals(Apartamento.MAX_ITEMS_DOTACION, new Apartamento(ID, "X", "D", 1, 2,
                treintaYUno.subList(0, Apartamento.MAX_ITEMS_DOTACION), Set.of()).getDotacion().size());
    }

    @Test
    void unaCaracteristicaNulaSeRechazaL06() {
        assertThrows(ReglaDominioException.class,
                () -> new Apartamento(ID, "X", "D", 1, 2, List.of(), Arrays.asList(Caracteristica.BALCON, null)));
    }

    @Test
    void tieneTodasExigeCadaUnaDeLasCaracteristicasPedidasL06() {
        Apartamento apt = conEquipamiento(); // TERRAZA y PARQUEADERO

        assertTrue(apt.tieneTodas(Set.of(Caracteristica.TERRAZA)));
        assertTrue(apt.tieneTodas(Set.of(Caracteristica.TERRAZA, Caracteristica.PARQUEADERO)));
        // falla: una pedida no la tiene, aunque tenga las otras
        assertFalse(apt.tieneTodas(Set.of(Caracteristica.TERRAZA, Caracteristica.JACUZZI)));
        assertFalse(apt.tieneTodas(Set.of(Caracteristica.JACUZZI)));
    }

    @Test
    void sinCaracteristicasPedidasCualquierApartamentoCumpleElFiltroL06() {
        assertTrue(apartamentoBase().tieneTodas(Set.of()));
        assertTrue(apartamentoBase().tieneTodas(null));
    }

    @Test
    void actualizarDatosReemplazaDescripcionDotacionYCaracteristicasL06() {
        Apartamento apt = conEquipamiento();

        apt.actualizarDatos("Nuevo", "Otra descripción", 2, 4, List.of("Lavadora"), Set.of(Caracteristica.JACUZZI));

        assertEquals("Otra descripción", apt.getDescripcion());
        assertEquals(List.of("Lavadora"), apt.getDotacion());
        assertEquals(Set.of(Caracteristica.JACUZZI), apt.getCaracteristicas());
    }

    @Test
    void siLaDotacionDeLaActualizacionEsInvalidaNoSeModificaNingunDatoL06() {
        Apartamento apt = conEquipamiento();

        // el nombre, la capacidad y la descripción son válidos; solo la dotación repite un elemento
        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos("Otro nombre", "Otra descripción", 1, 2,
                List.of("Nevera", "nevera"), Set.of(Caracteristica.JACUZZI)));

        assertEquals("Terraza", apt.getNombre());
        assertEquals("Con terraza privada", apt.getDescripcion());
        assertEquals(3, apt.getDormitorios());
        assertEquals(6, apt.getCapacidad());
        assertEquals(List.of("Nevera", "Smart TV"), apt.getDotacion());
        assertEquals(Set.of(Caracteristica.TERRAZA, Caracteristica.PARQUEADERO), apt.getCaracteristicas());
    }

    @Test
    void siLaDescripcionDeLaActualizacionEstaVaciaNoSeModificaNadaL06() {
        Apartamento apt = conEquipamiento();

        assertThrows(ReglaDominioException.class,
                () -> apt.actualizarDatos("Otro nombre", " ", 1, 2, List.of("X"), Set.of(Caracteristica.BALCON)));

        assertEquals("Terraza", apt.getNombre());
        assertEquals(List.of("Nevera", "Smart TV"), apt.getDotacion());
    }

    @Test
    void losGettersDevuelvenColeccionesInmodificablesYElAgregadoNoCambiaPorLaListaOriginalL06() {
        List<String> original = new ArrayList<>(List.of("Nevera"));
        Apartamento apt = new Apartamento(ID, "X", "D", 1, 2, original, Set.of(Caracteristica.BALCON));

        original.add("Intruso"); // cambiar la lista que se entregó no altera el agregado

        assertEquals(List.of("Nevera"), apt.getDotacion());
        assertThrows(UnsupportedOperationException.class, () -> apt.getDotacion().add("Otro"));
        assertThrows(UnsupportedOperationException.class, () -> apt.getCaracteristicas().add(Caracteristica.JACUZZI));
    }

    // ---------- Estado operativo ----------

    @Test
    void debePermitirTransicionDePreparadoAOcupado() {
        Apartamento apt = apartamentoBase();

        apt.marcarOcupado();

        assertEquals(EstadoOperativo.OCUPADO, apt.getEstadoOperativo());
    }

    @Test
    void noDebePermitirDeclararFueraDeServicioUnApartamentoOcupado() {
        // Arrange
        Apartamento apt = apartamentoBase();
        apt.marcarOcupado(); // Lo dejamos en OCUPADO

        // Act & Assert (Intentamos transición prohibida por el negocio)
        ReglaDominioException ex = assertThrows(ReglaDominioException.class, apt::declararFueraDeServicio);

        assertEquals("No se puede pasar de OCUPADO a FUERA_DE_SERVICIO", ex.getMessage());

        // Aserción de estado intacto: El estado NO debió cambiar tras el fallo
        assertEquals(EstadoOperativo.OCUPADO, apt.getEstadoOperativo());
    }

}
