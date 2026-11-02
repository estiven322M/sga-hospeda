package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

public record OcupanteResponse(String documento, String nombre, LocalDate fechaNacimiento) {
}
