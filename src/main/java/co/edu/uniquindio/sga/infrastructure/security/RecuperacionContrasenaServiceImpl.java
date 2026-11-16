package co.edu.uniquindio.sga.infrastructure.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.dto.request.ConfirmarRecuperacionRequest;
import co.edu.uniquindio.sga.application.security.RecuperacionContrasenaService;
import co.edu.uniquindio.sga.application.usecase.EmitirNotificacionUseCase;
import co.edu.uniquindio.sga.domain.entity.CodigoRecuperacion;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.CodigoRecuperacionRepository;
import co.edu.uniquindio.sga.domain.service.RedactorNotificaciones;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;

/**
 * Recuperación de contraseña (7.2). Las reglas del código (6 dígitos, vigencia, un solo uso, tope de intentos) son de la
 * entidad de dominio CodigoRecuperacion; aquí solo se orquesta: generar el código, cifrarlo, enviarlo por el puerto
 * Notificador y, al confirmar, comparar el hash y cambiar la contraseña. Nunca se revela si el correo existe.
 */
@Service
public class RecuperacionContrasenaServiceImpl implements RecuperacionContrasenaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaServiceImpl.class);

    private final UsuarioJpaDataRepository usuarios;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder encoder;
    private final RedactorNotificaciones redactor;
    private final EmitirNotificacionUseCase emitir;
    private final Clock reloj;
    private final int vigenciaMinutos;
    private final int intentosMaximos;
    private final SecureRandom azar = new SecureRandom();

    public RecuperacionContrasenaServiceImpl(UsuarioJpaDataRepository usuarios, CodigoRecuperacionRepository codigos,
                                             PasswordEncoder encoder, RedactorNotificaciones redactor,
                                             EmitirNotificacionUseCase emitir, Clock reloj,
                                             @Value("${sga.recuperacion.vigencia-minutos}") int vigenciaMinutos,
                                             @Value("${sga.recuperacion.intentos-maximos}") int intentosMaximos) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.encoder = encoder;
        this.redactor = redactor;
        this.emitir = emitir;
        this.reloj = reloj;
        this.vigenciaMinutos = vigenciaMinutos;
        this.intentosMaximos = intentosMaximos;
    }

    @Override
    public void solicitar(String correoTexto) {
        try {
            CorreoElectronico correo = new CorreoElectronico(correoTexto);
            Optional<UsuarioEntity> usuario = usuarios.findByCorreo(correo.valor()).filter(UsuarioEntity::isActivo);
            if (usuario.isEmpty()) {
                return;   // misma respuesta que si existiera: no se revela qué correos están registrados
            }
            LocalDateTime ahora = LocalDateTime.now(reloj);
            String codigo = CodigoRecuperacion.generarTexto(azar);
            codigos.guardar(CodigoRecuperacion.emitir(correo, encoder.encode(codigo), ahora, vigenciaMinutos));
            // El envío no es parte de la transacción del código y su fallo no se propaga (I-05): ver EmitirNotificacionUseCase
            emitir.ejecutar(redactor.codigoRecuperacion(correo, codigo, vigenciaMinutos, ahora));
        } catch (RuntimeException e) {
            log.warn("No se pudo procesar la solicitud de recuperación: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(noRollbackFor = ReglaDominioException.class)   // los intentos fallidos deben quedar guardados
    public void confirmar(ConfirmarRecuperacionRequest solicitud) {
        CorreoElectronico correo = parsear(solicitud.correo());
        Optional<CodigoRecuperacion> guardado = codigos.buscarPorCorreo(correo);
        Optional<UsuarioEntity> usuario = usuarios.findByCorreo(correo.valor()).filter(UsuarioEntity::isActivo);
        if (guardado.isEmpty() || usuario.isEmpty()) {
            throw new ReglaDominioException(CodigoRecuperacion.MENSAJE_RECHAZO);
        }
        CodigoRecuperacion codigo = guardado.get();
        boolean coincide = encoder.matches(solicitud.codigo(), codigo.getHash());
        try {
            codigo.consumir(coincide, LocalDateTime.now(reloj), intentosMaximos);
        } catch (ReglaDominioException rechazo) {
            codigos.guardar(codigo);   // deja registrado el intento fallido
            throw rechazo;
        }
        codigos.guardar(codigo);       // queda usado: no sirve otra vez
        usuario.get().cambiarContrasena(encoder.encode(solicitud.contrasenaNueva()));
    }

    /** Un correo mal formado recibe el mismo rechazo que un código incorrecto. */
    private static CorreoElectronico parsear(String texto) {
        try {
            return new CorreoElectronico(texto);
        } catch (ReglaDominioException e) {
            throw new ReglaDominioException(CodigoRecuperacion.MENSAJE_RECHAZO);
        }
    }
}
