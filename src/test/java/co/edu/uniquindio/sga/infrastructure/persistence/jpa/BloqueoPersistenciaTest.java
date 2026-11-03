package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.BloqueoPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.EstanciaPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({BloqueoJpaRepository.class, BloqueoPersistenceMapper.class, EstanciaPersistenceMapper.class})
class BloqueoPersistenciaTest {

    private static final IdentificacionApartamento APT = new IdentificacionApartamento("APT-101");
    private static final LocalDate BASE = LocalDate.of(2027, 3, 1);

    @Autowired
    private BloqueoJpaRepository repositorio;

    private static Bloqueo bloqueo(IdentificacionApartamento apartamento, int diaEntrada, String motivo) {
        return new Bloqueo(apartamento,
                new Estancia(BASE.plusDays(diaEntrada), BASE.plusDays(diaEntrada + 2)), motivo);
    }

    @Test
    void guardar_devuelve_el_bloqueo_con_su_rango_y_motivo() {
        Bloqueo guardado = repositorio.guardar(bloqueo(APT, 0, "Mantenimiento"));

        assertEquals(APT, guardado.getApartamento());
        assertEquals(new Estancia(BASE, BASE.plusDays(2)), guardado.getRango());
        assertEquals("Mantenimiento", guardado.getMotivo());
    }

    @Test
    void buscar_vigentes_devuelve_solo_los_del_apartamento_en_orden_cronologico() {
        repositorio.guardar(bloqueo(APT, 10, "Pintura"));
        repositorio.guardar(bloqueo(APT, 0, "Plomería"));
        repositorio.guardar(bloqueo(new IdentificacionApartamento("APT-102"), 5, "Otro apartamento"));

        List<String> motivos = repositorio.buscarVigentesPorApartamento(APT).stream()
                .map(Bloqueo::getMotivo).toList();

        assertEquals(List.of("Plomería", "Pintura"), motivos);
    }

    @Test
    void buscar_vigentes_de_un_apartamento_sin_bloqueos_devuelve_lista_vacia() {
        assertTrue(repositorio.buscarVigentesPorApartamento(APT).isEmpty());
    }

    @Test
    void listar_pagina_de_diez_con_total_sin_paginar_y_pagina_vacia_fuera_de_rango() {
        IntStream.range(0, 12).forEach(i -> repositorio.guardar(bloqueo(APT, i * 3, "Bloqueo " + i)));

        Pagina<Bloqueo> primera = repositorio.listarPorApartamento(APT, new SolicitudPagina(0));
        Pagina<Bloqueo> segunda = repositorio.listarPorApartamento(APT, new SolicitudPagina(1));
        Pagina<Bloqueo> vacia = repositorio.listarPorApartamento(APT, new SolicitudPagina(4));

        assertEquals(10, primera.contenido().size());
        assertEquals(2, segunda.contenido().size());
        assertEquals(12, primera.totalElementos());
        assertEquals("Bloqueo 0", primera.contenido().get(0).getMotivo());
        assertEquals("Bloqueo 11", segunda.contenido().get(1).getMotivo());
        assertTrue(vacia.contenido().isEmpty());
    }

    @Test
    void guardar_dos_veces_el_mismo_bloqueo_inserta_dos_filas_porque_el_dominio_no_le_da_identidad() {
        Bloqueo bloqueo = bloqueo(APT, 0, "Mantenimiento");

        repositorio.guardar(bloqueo);
        repositorio.guardar(bloqueo);

        assertEquals(2, repositorio.buscarVigentesPorApartamento(APT).size());
    }
}
