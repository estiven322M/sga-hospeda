package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ContactoReservaEntity;

/** Repositorio Spring Data del correo de contacto de una reserva (la clave es el código de la reserva). */
public interface ContactoReservaJpaDataRepository extends JpaRepository<ContactoReservaEntity, String> {
}
