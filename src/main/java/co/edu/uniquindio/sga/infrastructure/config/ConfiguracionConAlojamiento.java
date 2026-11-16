package co.edu.uniquindio.sga.infrastructure.config;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

/**
 * La configuración del sistema con una excepción: las horas de entrada y de salida salen del {@code Alojamiento} que el
 * administrador edita (L-12), de modo que cambiarlas se refleja en las reglas sin reiniciar. Mientras el alojamiento no
 * exista (por ejemplo, antes de sembrarlo) se usan los valores de application.properties. Todo lo demás se delega.
 */
class ConfiguracionConAlojamiento implements ConfiguracionAlojamiento {

    private final ConfiguracionAlojamiento base;
    private final AlojamientoRepository alojamiento;

    ConfiguracionConAlojamiento(ConfiguracionAlojamiento base, AlojamientoRepository alojamiento) {
        this.base = base;
        this.alojamiento = alojamiento;
    }

    @Override
    public LocalTime horaEntrada() {
        return alojamiento.obtener().map(a -> a.getHoraEntrada()).orElseGet(base::horaEntrada);
    }

    @Override
    public LocalTime horaSalida() {
        return alojamiento.obtener().map(a -> a.getHoraSalida()).orElseGet(base::horaSalida);
    }

    @Override
    public TiempoPreparacion tiempoPreparacion() { return base.tiempoPreparacion(); }

    @Override
    public PlazoConfirmacion plazoConfirmacion() { return base.plazoConfirmacion(); }

    @Override
    public UmbralEdadFacturable umbralEdadFacturable() { return base.umbralEdadFacturable(); }

    @Override
    public LocalTime horaLimiteNoShow() { return base.horaLimiteNoShow(); }

    @Override
    public ZoneId zonaHoraria() { return base.zonaHoraria(); }

    @Override
    public int anticipoPorcentaje() { return base.anticipoPorcentaje(); }

    @Override
    public Map<String, Integer> estanciaMinimaPorTemporada() { return base.estanciaMinimaPorTemporada(); }

    @Override
    public int nochesDescuentoEstanciaLarga() { return base.nochesDescuentoEstanciaLarga(); }

    @Override
    public int porcentajeDescuentoEstanciaLarga() { return base.porcentajeDescuentoEstanciaLarga(); }

    @Override
    public int edadAdultoResponsableAnios() { return base.edadAdultoResponsableAnios(); }

    @Override
    public List<String> mediosPagoAceptados() { return base.mediosPagoAceptados(); }

    @Override
    public int calendarioMaximoNoches() { return base.calendarioMaximoNoches(); }
}
