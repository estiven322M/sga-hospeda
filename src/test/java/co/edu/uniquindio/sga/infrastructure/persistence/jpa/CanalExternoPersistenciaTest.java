package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.AHORA;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.RELOJ;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;


import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.EstanciaPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.OcupantePersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.RechazoReservaExternaPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/** Persistencia del canal externo: identificador de la reserva (RN-19) y constancia de los rechazos (RN-18). */
@DataJpaTest
@Import({ReservaJpaRepository.class, ReservaPersistenceMapper.class, EstanciaPersistenceMapper.class,
        OcupantePersistenceMapper.class, RechazoReservaExternaJpaRepository.class,
        RechazoReservaExternaPersistenceMapper.class, CanalExternoPersistenciaTest.RelojFijo.class})
class CanalExternoPersistenciaTest {

    @TestConfiguration
    static class RelojFijo {
        @Bean
        Clock reloj() {
            return RELOJ;
        }
    }

    private static final LocalDate ENTRADA = LocalDate.of(2027, 3, 10);
    private static final IdentificadorExterno MENSAJE = new IdentificadorExterno("BKG-1");

    @Autowired
    private ReservaJpaRepository reservas;
    @Autowired
    private RechazoReservaExternaJpaRepository rechazos;
    @Autowired
    private EntityManager em;

    private Reserva externa(int n, String apartamento) {
        return reserva(String.format("RES-2026-%05d", n), apartamento, ENTRADA, 2, "100", CanalOrigen.EXTERNO, AHORA);
    }

    // ---------- RN-19 ----------

    @Test
    void elIdentificadorExternoSobreviveAlGuardarYLeer() {
        Reserva original = reservas.guardar(externa(1, "APT-101"));
        em.flush();
        em.clear();

        Reserva leida = reservas.buscarPorCodigo(original.getCodigo()).orElseThrow();

        assertEquals(original.getIdentificadorExterno(), leida.getIdentificadorExterno());
        assertEquals(CanalOrigen.EXTERNO, leida.getCanalOrigen());
    }

    @Test
    void lasReservasSinCanalExternoGuardanIdentificadorNulo() {
        Reserva portal = reservas.guardar(reserva("RES-2026-00002", "APT-101", ENTRADA));
        em.flush();
        em.clear();

        assertNull(reservas.buscarPorCodigo(portal.getCodigo()).orElseThrow().getIdentificadorExterno());
    }

    @Test
    void seEncuentraLaReservaPorCanalEIdentificador() {
        Reserva guardada = reservas.guardar(externa(3, "APT-101"));
        em.flush();
        em.clear();

        var encontrada = reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO,
                guardada.getIdentificadorExterno());

        assertTrue(encontrada.isPresent());
        assertEquals(guardada.getCodigo(), encontrada.get().getCodigo());
        assertFalse(reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, MENSAJE).isPresent());
        assertFalse(reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.PORTAL,
                guardada.getIdentificadorExterno()).isPresent());
    }

    @Test
    void laBaseDeDatosImpideDosReservasConElMismoCanalEIdentificador() {
        Reserva primera = reservas.guardar(externa(4, "APT-101"));
        em.flush();
        // Mismo canal e identificador que la primera, con otro código y otro apartamento
        Reserva repetida = Reserva.crear(new co.edu.uniquindio.sga.domain.valueobject.CodigoReserva("RES-2026-00005"),
                new IdentificacionApartamento("APT-102"), primera.getEstancia(), primera.getTitular(),
                primera.getOcupantes(), CanalOrigen.EXTERNO, primera.getIdentificadorExterno(), primera.getValor(),
                primera.getPolitica(), AHORA);

        reservas.guardar(repetida);

        // em.flush() lanza la excepción de JPA, sin la traducción de Spring que hacen los repositorios
        assertThrows(PersistenceException.class, () -> em.flush());
    }

    // ---------- RN-18 ----------

    private RechazoReservaExterna rechazo(int n, LocalDateTime cuando) {
        return new RechazoReservaExterna(CanalOrigen.EXTERNO, new IdentificadorExterno("BKG-" + n),
                new IdentificacionApartamento("APT-101"), new Estancia(ENTRADA, ENTRADA.plusDays(2)),
                "El apartamento ya tiene una reserva activa que solapa esas noches", cuando);
    }

    @Test
    void elRechazoSobreviveAlGuardarYLeer() {
        RechazoReservaExterna original = rechazo(1, AHORA);
        rechazos.guardar(original);
        em.flush();
        em.clear();

        Pagina<RechazoReservaExterna> pagina = rechazos.listar(SolicitudPagina.primera());

        assertEquals(1, pagina.totalElementos());
        RechazoReservaExterna leido = pagina.contenido().get(0);
        assertEquals(original.getCanalOrigen(), leido.getCanalOrigen());
        assertEquals(original.getIdentificadorExterno(), leido.getIdentificadorExterno());
        assertEquals(original.getApartamento(), leido.getApartamento());
        assertEquals(original.getEstancia(), leido.getEstancia());
        assertEquals(original.getMotivo(), leido.getMotivo());
        assertEquals(original.getRechazadaEn(), leido.getRechazadaEn());
    }

    @Test
    void losRechazosSeListanDeDiezEnDiezDelMasRecienteAlMasAntiguo() {
        IntStream.rangeClosed(1, 12).forEach(n -> rechazos.guardar(rechazo(n, AHORA.plusMinutes(n))));
        em.flush();
        em.clear();

        Pagina<RechazoReservaExterna> primera = rechazos.listar(new SolicitudPagina(0));
        Pagina<RechazoReservaExterna> segunda = rechazos.listar(new SolicitudPagina(1));

        assertEquals(12, primera.totalElementos());
        assertEquals(10, primera.contenido().size());
        assertEquals(2, segunda.contenido().size());
        assertEquals("BKG-12", primera.contenido().get(0).getIdentificadorExterno().valor());
        assertEquals("BKG-1", segunda.contenido().get(1).getIdentificadorExterno().valor());
    }

    @Test
    void sinRechazosElListadoVieneVacio() {
        Pagina<RechazoReservaExterna> pagina = rechazos.listar(SolicitudPagina.primera());

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(0, pagina.totalElementos());
    }
}
