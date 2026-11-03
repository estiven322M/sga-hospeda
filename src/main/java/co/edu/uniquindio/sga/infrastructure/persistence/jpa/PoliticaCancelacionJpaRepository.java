package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.PoliticaCancelacionPersistenceMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PoliticaCancelacionJpaRepository implements PoliticaCancelacionRepository {

    private final PoliticaCancelacionJpaDataRepository datos;
    private final PoliticaCancelacionPersistenceMapper mapper;

    public PoliticaCancelacionJpaRepository(PoliticaCancelacionJpaDataRepository datos,
                                            PoliticaCancelacionPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PoliticaCancelacion guardar(PoliticaCancelacion politica) {
        return mapper.aDominio(datos.save(mapper.aEntidad(politica)));
    }

    @Override
    public Optional<PoliticaCancelacion> vigenteEn(LocalDate fecha) {
        return datos.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDescNumeroVersionDesc(fecha)
                .map(mapper::aDominio);
    }

    @Override
    public Optional<PoliticaCancelacion> ultimaVersion() {
        return datos.findFirstByOrderByNumeroVersionDesc().map(mapper::aDominio);
    }

    @Override
    public Optional<PoliticaCancelacion> porVersion(VersionPolitica version) {
        return datos.findById(version.numero()).map(mapper::aDominio);
    }
}
