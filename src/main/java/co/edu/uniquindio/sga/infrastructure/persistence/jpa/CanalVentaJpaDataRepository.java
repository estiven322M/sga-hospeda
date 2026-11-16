package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CanalVentaEntity;

/** Repositorio Spring Data de los canales de venta (la clave es el código). */
public interface CanalVentaJpaDataRepository extends JpaRepository<CanalVentaEntity, String> {

    Optional<CanalVentaEntity> findByCredencialHash(String credencialHash);
}
