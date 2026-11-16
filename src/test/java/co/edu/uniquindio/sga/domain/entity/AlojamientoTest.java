package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;

/** L-01, L-02, L-12 y L-16: los datos del alojamiento y las reglas que los protegen. */
class AlojamientoTest {

    private static Alojamiento valido() {
        return new Alojamiento("Hospeda Salento", "Apartamentos con vista al valle", "Salento", "Carrera 6 # 3-45",
                new Ubicacion(4.6368, -75.57), LocalTime.of(15, 0), LocalTime.of(11, 0),
                "Silencio desde las 10 p. m.", "No se aceptan mascotas.");
    }

    @Test
    void seCreaConTodosSusDatosYLosLimpia() {
        Alojamiento a = new Alojamiento("  Hospeda  ", "Descripción", "Salento", "Carrera 6", new Ubicacion(4.6, -75.5),
                LocalTime.of(15, 0), LocalTime.of(11, 0), "Normas", "No se aceptan mascotas.");

        assertEquals("Hospeda", a.getNombre());
        assertEquals(4.6, a.getUbicacion().latitud());
        assertEquals(LocalTime.of(15, 0), a.getHoraEntrada());
        assertEquals("No se aceptan mascotas.", a.getPoliticaMascotas());
    }

    @Test
    void exigeNombreDescripcionCiudadDireccionNormasYPoliticaDeMascotas() {
        var u = new Ubicacion(4.6, -75.5);
        var entrada = LocalTime.of(15, 0);
        var salida = LocalTime.of(11, 0);
        assertThrows(ReglaDominioException.class, () -> new Alojamiento(" ", "d", "c", "dir", u, entrada, salida, "n", "m"));
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", null, "c", "dir", u, entrada, salida, "n", "m"));
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "", "dir", u, entrada, salida, "n", "m"));
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "c", " ", u, entrada, salida, "n", "m"));
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "c", "dir", u, entrada, salida, "", "m"));
        // L-16: la decisión sobre mascotas es explícita; no puede quedar sin definir
        ReglaDominioException sinMascotas = assertThrows(ReglaDominioException.class,
                () -> new Alojamiento("N", "d", "c", "dir", u, entrada, salida, "n", " "));
        assertEquals("El alojamiento debe definir su política de mascotas (aunque sea que no se aceptan)", sinMascotas.getMessage());
    }

    @Test
    void laHoraDeSalidaDebeSerAnteriorALaDeEntrada() {
        var u = new Ubicacion(4.6, -75.5);
        ReglaDominioException igual = assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "c", "dir", u,
                LocalTime.of(11, 0), LocalTime.of(11, 0), "n", "m"));
        assertEquals("La hora de salida debe ser anterior a la hora de entrada", igual.getMessage());
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "c", "dir", u,
                LocalTime.of(10, 0), LocalTime.of(15, 0), "n", "m"));
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "c", "dir", u, null, LocalTime.of(11, 0), "n", "m"));
    }

    @Test
    void laUbicacionDebeSerPosibleSobreLaTierra() {
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(91, 0));
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(-91, 0));
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(0, 181));
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(0, -181));
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(Double.NaN, 0));
        assertThrows(ReglaDominioException.class, () -> new Alojamiento("N", "d", "c", "dir", null,
                LocalTime.of(15, 0), LocalTime.of(11, 0), "n", "m"));
    }

    @Test
    void unTextoDemasiadoLargoSeRechaza() {
        var u = new Ubicacion(4.6, -75.5);
        String largo = "x".repeat(Alojamiento.MAX_NOMBRE + 1);
        assertThrows(ReglaDominioException.class, () -> new Alojamiento(largo, "d", "c", "dir", u,
                LocalTime.of(15, 0), LocalTime.of(11, 0), "n", "m"));
    }

    @Test
    void actualizarCambiaLosDatosYEsAtomicaSiAlgoEsInvalido() {
        Alojamiento a = valido();

        a.actualizarDatos("Nuevo nombre", "Nueva descripción", "Armenia", "Calle 1", new Ubicacion(4.5, -75.6),
                LocalTime.of(14, 0), LocalTime.of(10, 0), "Nuevas normas", "Se aceptan mascotas pequeñas.");
        assertEquals("Nuevo nombre", a.getNombre());
        assertEquals("Se aceptan mascotas pequeñas.", a.getPoliticaMascotas());
        assertEquals(LocalTime.of(10, 0), a.getHoraSalida());

        // con una hora inválida no se modifica NADA, ni siquiera lo que sí era válido
        assertThrows(ReglaDominioException.class, () -> a.actualizarDatos("Otro", "d", "c", "dir", new Ubicacion(1, 1),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "n", "m"));
        assertEquals("Nuevo nombre", a.getNombre());
        assertEquals(LocalTime.of(14, 0), a.getHoraEntrada());
    }
}
