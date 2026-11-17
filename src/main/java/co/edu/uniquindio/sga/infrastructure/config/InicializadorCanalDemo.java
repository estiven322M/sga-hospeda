package co.edu.uniquindio.sga.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/**
 * Siembra el canal externo de DEMOSTRACIÓN (regla 12 y 10.3.1) con una credencial pública y documentada en el README, para poder
 * ejecutar el simulador de canal sin pasos previos. Solo corre con los datos de prueba habilitados
 * ({@code sga.datos-prueba.habilitado=true}); un despliegue real los desactiva y no queda ninguna credencial conocida. Con {@code sga.canal-demo.habilitado=false} (el valor en prod) no siembra nada. Es idempotente:
 * si el canal ya existe no lo toca (ni su estado ni su credencial). En la base solo se guarda el HASH de la credencial.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "sga.datos-prueba.habilitado", havingValue = "true", matchIfMissing = true)
class InicializadorCanalDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorCanalDemo.class);

    private final CanalVentaRepository canales;
    private final String codigo;
    private final String nombre;
    private final String credencial;
    private final boolean habilitado;

    InicializadorCanalDemo(CanalVentaRepository canales, @Value("${sga.canal-demo.codigo}") String codigo,
                           @Value("${sga.canal-demo.nombre}") String nombre, @Value("${sga.canal-demo.credencial}") String credencial,
                           @Value("${sga.canal-demo.habilitado:true}") boolean habilitado) {
        this.habilitado = habilitado;
        this.canales = canales;
        this.codigo = codigo;
        this.nombre = nombre;
        this.credencial = credencial;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (!habilitado) {
            log.info("Canal externo de demostración desactivado (sga.canal-demo.habilitado=false).");
            return;
        }
        if (canales.buscarPorCodigo(codigo).isPresent()) {
            return;
        }
        canales.guardar(new CanalVenta(codigo, nombre, TipoCanalVenta.PLATAFORMA_EN_LINEA, CredencialCanal.de(credencial).hash()));
        log.info("Canal externo de demostración {} sembrado (credencial en el README).", codigo);
    }
}
