package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.service.CalendarioTemporadas;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaId;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.TarifaPersistenceMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public class TarifarioJpaRepository implements TarifarioRepository {

    private final TarifaJpaDataRepository datos;
    private final TemporadaRepository temporadas;
    private final TarifaPersistenceMapper mapper;

    public TarifarioJpaRepository(TarifaJpaDataRepository datos, TemporadaRepository temporadas,
                                  TarifaPersistenceMapper mapper) {
        this.datos = datos;
        this.temporadas = temporadas;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Tarifa tarifaDe(IdentificacionApartamento apartamento, LocalDate noche) {
        String temporada = new CalendarioTemporadas(temporadas.listarTodas()).temporadaDe(noche).getNombre();
        return datos.findById(new TarifaId(apartamento.valor(), temporada)).map(mapper::aDominio).orElse(null);
    }

    /** Se une a la transacción del caso de uso; crea o reemplaza la tarifa de esa temporada. */
    @Override
    @Transactional
    public void guardarTarifa(IdentificacionApartamento apartamento, Tarifa tarifa) {
        datos.save(mapper.aEntidad(apartamento, tarifa));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tarifa> listarTarifas(IdentificacionApartamento apartamento) {
        return datos.findByIdApartamentoIdOrderByIdTemporada(apartamento.valor()).stream()
                .map(mapper::aDominio).toList();
    }
}
