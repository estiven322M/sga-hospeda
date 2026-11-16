package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/**
 * 7.8: el código de un canal es único (es la parte "canal" de "canal + identificador externo", RN-19). Un canal no puede saber si
 * otro ya usa su código (es otro agregado), por eso la regla vive en un servicio de dominio. Crea el canal con el HASH de la
 * credencial; el texto de la credencial lo conserva quien la generó, para mostrarla una sola vez.
 */
public class RegistroCanalVentaService {

    private final CanalVentaRepository canales;

    public RegistroCanalVentaService(CanalVentaRepository canales) {
        this.canales = canales;
    }

    public CanalVenta crear(String codigo, String nombre, TipoCanalVenta tipo, CredencialCanal credencial) {
        CanalVenta nuevo = new CanalVenta(codigo, nombre, tipo, credencial.hash());
        if (canales.buscarPorCodigo(nuevo.getCodigo()).isPresent()) {
            throw new ReglaDominioException("Ya existe un canal con el código " + nuevo.getCodigo());
        }
        return nuevo;
    }
}
