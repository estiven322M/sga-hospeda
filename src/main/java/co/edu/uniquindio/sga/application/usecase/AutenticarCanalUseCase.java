package co.edu.uniquindio.sga.application.usecase;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;

/**
 * Reconoce al canal por la credencial que llegó en {@code X-Canal-Credencial}: calcula su hash y busca el canal que lo tiene. Devuelve
 * el canal aunque esté desactivado, para que quien llama distinga "credencial desconocida" de "canal desactivado" (ambos son 401 para
 * el canal, pero solo el segundo queda en la bitácora). Una credencial vacía no corresponde a ningún canal.
 */
@Service
public class AutenticarCanalUseCase {

    private final CanalVentaRepository canales;

    public AutenticarCanalUseCase(CanalVentaRepository canales) {
        this.canales = canales;
    }

    @Transactional(readOnly = true)
    public Optional<CanalVenta> ejecutar(String credencial) {
        if (credencial == null || credencial.isBlank()) {
            return Optional.empty();
        }
        return canales.buscarPorCredencialHash(CredencialCanal.hashDe(credencial));
    }
}
