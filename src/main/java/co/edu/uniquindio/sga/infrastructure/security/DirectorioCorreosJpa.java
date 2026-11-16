package co.edu.uniquindio.sga.infrastructure.security;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.security.DirectorioCorreos;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;

/** Adaptador del puente titular → cuenta (3.6): busca por documento al usuario ACTIVO y devuelve su correo. */
@Component
public class DirectorioCorreosJpa implements DirectorioCorreos {

    private final UsuarioJpaDataRepository usuarios;

    public DirectorioCorreosJpa(UsuarioJpaDataRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorreoElectronico> correoDeCuenta(DocumentoIdentidad documento) {
        return usuarios.findByDocumentoAndActivoTrue(documento.numero())
                .map(usuario -> new CorreoElectronico(usuario.getCorreo()));
    }
}
