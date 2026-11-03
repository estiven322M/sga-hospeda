package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;

/**
 * Conversiones de objetos de valor del dominio a tipos simples de la API. Los mappers de respuesta
 * la extienden para que MapStruct las use cada vez que necesite pasar de un tipo al otro.
 * No hay lógica de negocio aquí: el dinero viaja como entero de pesos COP, nunca como double.
 */
public interface ConversionesDominio {

    String MONEDA = "COP";

    default String aTexto(CodigoReserva codigo) {
        return codigo == null ? null : codigo.valor();
    }

    default String aTexto(IdentificacionApartamento identificacion) {
        return identificacion == null ? null : identificacion.valor();
    }

    default String aTexto(IdentificadorExterno identificador) {
        return identificador == null ? null : identificador.valor();
    }

    default String aTexto(DocumentoIdentidad documento) {
        return documento == null ? null : documento.numero();
    }

    default long aPesos(Dinero dinero) {
        return dinero.valor().longValueExact();
    }
}
