package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ApartamentoTest {
	
	// Método auxiliar (Arrange) para no repetir la creación del objeto en cada test
    private Apartamento apartamentoBase() {
        return new Apartamento(
            new IdentificacionApartamento("APT-101"),
            "Vista a la Montaña",
            2, // dormitorios
            4  // capacidad máxima
        );
    }

    @Test
    void dosApartamentosConElMismoIdentificadorSonElMismo() {
        // La identidad depende del ID, no de los demás atributos
        Apartamento apt1 = new Apartamento(new IdentificacionApartamento("APT-101"), "A", 1, 2);
        Apartamento apt2 = new Apartamento(new IdentificacionApartamento("APT-101"), "B", 3, 5);

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

        apt.actualizarDatos("Nuevo nombre", 3, 6);

        assertEquals("Nuevo nombre", apt.getNombre());
        assertEquals(3, apt.getDormitorios());
        assertEquals(6, apt.getCapacidad());
    }

    @Test
    void actualizarDatosInvalidosNoModificaNada() {
        Apartamento apt = apartamentoBase();
        String nombre = apt.getNombre();
        int capacidad = apt.getCapacidad();

        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos(" ", 2, 4));
        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos("X", 0, 4));
        assertThrows(ReglaDominioException.class, () -> apt.actualizarDatos("X", 2, 0));

        assertEquals(nombre, apt.getNombre());
        assertEquals(capacidad, apt.getCapacidad());
    }

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

        // Ajusta este texto si tu ReglaDominioException en Apartamento lanza un mensaje distinto
        assertEquals("No se puede pasar de OCUPADO a FUERA_DE_SERVICIO", ex.getMessage());
        
        // Aserción de estado intacto: El estado NO debió cambiar tras el fallo
        assertEquals(EstadoOperativo.OCUPADO, apt.getEstadoOperativo());
    }

}
