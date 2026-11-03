package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;

/** Resultado de CU-02 por apartamento: el apartamento libre y lo que costaría la estancia pedida. */
public record ApartamentoDisponible(Apartamento apartamento, ValorCongelado cotizacion) {
}
