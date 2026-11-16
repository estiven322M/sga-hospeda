package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.service.RegistroCanalVentaService;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/**
 * 7.8: registrar un canal de venta. El dominio decide que el código sea único; aquí se genera la credencial (256 bits aleatorios), se
 * guarda SOLO su hash y se devuelve el texto en claro para que el administrador lo vea esta única vez.
 */
@Service
public class CrearCanalVentaUseCase {

    /** El canal creado y su credencial en claro (la única vez que existe fuera del canal). */
    public record Resultado(CanalVenta canal, String credencial) {
    }

    private final CanalVentaRepository canales;
    private final RegistroCanalVentaService registro;

    public CrearCanalVentaUseCase(CanalVentaRepository canales, RegistroCanalVentaService registro) {
        this.canales = canales;
        this.registro = registro;
    }

    @Transactional
    public Resultado ejecutar(String codigo, String nombre, TipoCanalVenta tipo) {
        CredencialCanal credencial = CredencialCanal.generar();
        CanalVenta guardado = canales.guardar(registro.crear(codigo, nombre, tipo, credencial));
        return new Resultado(guardado, credencial.valor());
    }
}
