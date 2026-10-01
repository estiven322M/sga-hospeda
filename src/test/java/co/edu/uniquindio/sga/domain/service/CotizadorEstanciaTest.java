package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CotizadorEstanciaTest {
	
	private final TarifarioRepository tarifaMock = mock(TarifarioRepository.class);
    private final ConfiguracionAlojamiento configMock = mock(ConfiguracionAlojamiento.class);
    private final CotizadorEstancia cotizador = new CotizadorEstancia(tarifaMock, configMock);

    @Test
    void debeLiquidarNochePorNocheRN05() {
        IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        
        Ocupante adulto = new Ocupante(new DocumentoIdentidad("1"), "A", LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
        Ocupante menor = new Ocupante(new DocumentoIdentidad("2"), "M", LocalDate.of(2015, 1, 1), LocalDate.of(2026, 1, 1));

        // Programar mocks: el umbral es 18 años (solo el adulto paga)
        when(configMock.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(18));
        
        // Tarifa: 50.000 por persona facturable, por noche
        Tarifa tarifa = new Tarifa("ALTA", Dinero.de(50000));
        when(tarifaMock.tarifaDe(eq(apt), any(LocalDate.class))).thenReturn(tarifa);

        ValorCongelado valor = cotizador.cotizar(apt, estancia, List.of(adulto, menor));
        
        // 2 noches x 1 ocupante facturable x 50,000 = 100,000
        assertEquals(Dinero.de(100000), valor.total());
    }

}
