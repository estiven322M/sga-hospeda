package co.edu.uniquindio.sga.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.AlmacenImagenes;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.ValidadorImagenes;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;

/**
 * 7.3: subir imágenes a la galería de un apartamento (de 1 a 10 por apartamento, la primera queda como principal). Orquesta:
 * <ol>
 *   <li>El dominio decide ANTES de subir nada: el cupo (máximo 10, 409) y el tamaño de cada imagen (409). Así un lote que no
 *       cabe o que trae una imagen inválida no deja archivos huérfanos en el almacén.</li>
 *   <li>El puerto AlmacenImagenes guarda cada archivo. Si el almacén falla se lanza AlmacenamientoImagenesException (la API
 *       responde 503) y NO se registra ninguna imagen del lote: nada queda a medias en el apartamento.</li>
 *   <li>El apartamento registra las referencias (el dominio vuelve a revisar el cupo) y se guarda.</li>
 * </ol>
 * Deliberadamente NO es transaccional: la subida es una llamada externa que puede tardar y no debe retener una conexión de la
 * base de datos; cada lectura y cada guardado del repositorio tiene su propia transacción.
 */
@Service
public class AgregarImagenesApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final ValidadorImagenes validador;
    private final AlmacenImagenes almacen;

    public AgregarImagenesApartamentoUseCase(ApartamentoRepository apartamentoRepository, ValidadorImagenes validador,
                                             AlmacenImagenes almacen) {
        this.apartamentoRepository = apartamentoRepository;
        this.validador = validador;
        this.almacen = almacen;
    }

    public Apartamento ejecutar(IdentificacionApartamento identificacion, List<ContenidoImagen> imagenes) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));

        apartamento.verificarCupoImagenes(imagenes.size());
        imagenes.forEach(validador::validarParaApartamento);

        List<ReferenciaImagen> guardadas = new ArrayList<>();
        for (ContenidoImagen imagen : imagenes) {
            guardadas.add(almacen.guardar(imagen, "apartamentos/" + identificacion.valor()));
        }

        apartamento.agregarImagenes(guardadas);
        return apartamentoRepository.guardar(apartamento);
    }
}
