package co.edu.uniquindio.sga.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;

/**
 * CU-01 Cotizar estancia. Solo orquesta: verifica que el apartamento exista y delega el cálculo
 * (RN-05 noche por noche, RN-06 ocupantes facturables) en el servicio de dominio CotizadorEstancia.
 * No persiste nada: cotizar no crea ni reserva.
 */
@Service
public class CotizarEstanciaUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final CotizadorEstancia cotizador;

    public CotizarEstanciaUseCase(ApartamentoRepository apartamentoRepository, CotizadorEstancia cotizador) {
        this.apartamentoRepository = apartamentoRepository;
        this.cotizador = cotizador;
    }

    @Transactional(readOnly = true)
    public ValorCongelado ejecutar(IdentificacionApartamento apartamento, Estancia estancia,
                                   List<Ocupante> ocupantes) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        return cotizador.cotizar(apartamento, estancia, ocupantes);
    }
}
