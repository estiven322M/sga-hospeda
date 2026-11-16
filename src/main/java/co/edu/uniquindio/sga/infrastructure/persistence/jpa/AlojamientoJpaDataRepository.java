package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AlojamientoEntity;

public interface AlojamientoJpaDataRepository extends JpaRepository<AlojamientoEntity, Long> {
}
