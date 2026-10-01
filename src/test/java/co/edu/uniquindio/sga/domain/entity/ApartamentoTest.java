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
    void debeNacerActivoYPreparado() {
        Apartamento apt = apartamentoBase();

        assertTrue(apt.isActivo());
        assertEquals(EstadoOperativo.PREPARADO, apt.getEstadoOperativo());
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
