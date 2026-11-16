package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AlojamientoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;

/** Hay un solo alojamiento: siempre se lee y se escribe la fila con el identificador único. */
@Repository
public class AlojamientoJpaRepository implements AlojamientoRepository {

    private final AlojamientoJpaDataRepository datos;
    private final AlojamientoPersistenceMapper mapper;

    public AlojamientoJpaRepository(AlojamientoJpaDataRepository datos, AlojamientoPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alojamiento> obtener() {
        return datos.findById(AlojamientoEntity.ID_UNICO).map(mapper::aDominio);
    }

    @Override
    @Transactional
    public Alojamiento guardar(Alojamiento alojamiento) {
        // El identificador de la entidad es fijo, así que guardar inserta la primera vez y reemplaza después
        return mapper.aDominio(datos.save(mapper.aEntidad(alojamiento)));
    }
}
