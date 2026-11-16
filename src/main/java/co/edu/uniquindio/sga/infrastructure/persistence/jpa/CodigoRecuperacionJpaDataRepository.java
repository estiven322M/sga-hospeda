package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CodigoRecuperacionEntity;

/** Repositorio Spring Data del código de recuperación (la clave es el correo). */
public interface CodigoRecuperacionJpaDataRepository extends JpaRepository<CodigoRecuperacionEntity, String> {
}
