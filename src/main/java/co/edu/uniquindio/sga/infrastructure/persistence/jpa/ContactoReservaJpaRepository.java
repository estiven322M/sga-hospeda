package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.repository.ContactoReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ContactoReservaEntity;

/** Adaptador JPA del puerto ContactoReservaRepository: traduce el código y el correo del dominio a texto. */
@Repository
public class ContactoReservaJpaRepository implements ContactoReservaRepository {

    private final ContactoReservaJpaDataRepository datos;

    public ContactoReservaJpaRepository(ContactoReservaJpaDataRepository datos) {
        this.datos = datos;
    }

    @Override
    @Transactional
    public void guardar(CodigoReserva reserva, CorreoElectronico correo) {
        datos.save(new ContactoReservaEntity(reserva.valor(), correo.valor()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorreoElectronico> buscar(CodigoReserva reserva) {
        return datos.findById(reserva.valor()).map(entidad -> new CorreoElectronico(entidad.getCorreo()));
    }
}
