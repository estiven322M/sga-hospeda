package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.CodigoRecuperacion;
import co.edu.uniquindio.sga.domain.repository.CodigoRecuperacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CodigoRecuperacionEntity;

/** Adaptador JPA del puerto CodigoRecuperacionRepository: mapea el código del dominio a su fila y de vuelta. */
@Repository
public class CodigoRecuperacionJpaRepository implements CodigoRecuperacionRepository {

    private final CodigoRecuperacionJpaDataRepository datos;

    public CodigoRecuperacionJpaRepository(CodigoRecuperacionJpaDataRepository datos) {
        this.datos = datos;
    }

    @Override
    @Transactional
    public void guardar(CodigoRecuperacion codigo) {
        datos.save(new CodigoRecuperacionEntity(codigo.getCorreo().valor(), codigo.getHash(), codigo.getExpiraEn(),
                codigo.isUsado(), codigo.getIntentosFallidos()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CodigoRecuperacion> buscarPorCorreo(CorreoElectronico correo) {
        return datos.findById(correo.valor()).map(e -> CodigoRecuperacion.reconstruir(
                new CorreoElectronico(e.getCorreo()), e.getHash(), e.getExpiraEn(), e.isUsado(), e.getIntentosFallidos()));
    }
}
