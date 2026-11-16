package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** RN-05, RN-22 y RP-02 (L-20): bruto, descuento por estancia larga y total del valor congelado. */
class ValorCongeladoTest {

    private static List<CargoNoche> noches(int cantidad, long tarifa, int facturables) {
        return IntStream.range(0, cantidad)
                .mapToObj(i -> new CargoNoche(LocalDate.of(2028, 3, 10).plusDays(i), "BASE", Dinero.de(tarifa), facturables))
                .toList();
    }

    @Test
    void elConstructorAnteriorEquivaleAUnDescuentoDelCeroPorCiento() {
        ValorCongelado valor = new ValorCongelado(noches(2, 100_000, 1));

        assertEquals(0, valor.porcentajeDescuento());
        assertEquals(Dinero.de(200_000), valor.bruto());
        assertEquals(Dinero.CERO, valor.descuento());
        assertEquals(Dinero.de(200_000), valor.total());
    }

    @Test
    void conDiezPorCientoElTotalEsElBrutoMenosElDescuento() {
        ValorCongelado valor = new ValorCongelado(noches(7, 100_000, 1), 10);

        assertEquals(Dinero.de(700_000), valor.bruto());
        assertEquals(Dinero.de(70_000), valor.descuento());
        assertEquals(Dinero.de(630_000), valor.total());
        assertEquals(10, valor.porcentajeDescuento());
    }

    @Test
    void elDescuentoSeCalculaSobreElTotalDeTodosLosOcupantesFacturables() {
        ValorCongelado valor = new ValorCongelado(noches(7, 100_000, 2), 10);

        assertEquals(Dinero.de(1_400_000), valor.bruto());
        assertEquals(Dinero.de(140_000), valor.descuento());
        assertEquals(Dinero.de(1_260_000), valor.total());
    }

    @Test
    void elRedondeoEsDeLaMitadHaciaArribaYSeHaceAlFinalUnaSolaVez() {
        // 7 noches x 50.005 = 350.035; el 10 % es 35.003,5 y se redondea a 35.004
        ValorCongelado valor = new ValorCongelado(noches(7, 50_005, 1), 10);

        assertEquals(Dinero.de(350_035), valor.bruto());
        assertEquals(Dinero.de(35_004), valor.descuento());
        assertEquals(Dinero.de(315_031), valor.total());
    }

    @Test
    void unaFraccionPorDebajoDeLaMitadSeRedondeaHaciaAbajo() {
        // 100.004 x 10 % = 10.000,4 -> 10.000
        ValorCongelado valor = new ValorCongelado(noches(1, 100_004, 1), 10);

        assertEquals(Dinero.de(10_000), valor.descuento());
        assertEquals(Dinero.de(90_004), valor.total());
    }

    @Test
    void elDescuentoSeAplicaUnaSolaVezSobreElTotalYNoNocheANoche() {
        // 7 noches de 15 pesos: el 10 % del total (105) es 10,5 -> 11. Redondear noche a noche (1,5 -> 2) daría 14.
        ValorCongelado valor = new ValorCongelado(noches(7, 15, 1), 10);

        assertEquals(Dinero.de(105), valor.bruto());
        assertEquals(Dinero.de(11), valor.descuento());
        assertEquals(Dinero.de(94), valor.total());
    }

    @Test
    void elDesgloseNocheANocheNoCambiaConElDescuento() {
        ValorCongelado conDescuento = new ValorCongelado(noches(7, 100_000, 1), 10);

        assertEquals(7, conDescuento.noches());
        assertTrue(conDescuento.detalle().stream().allMatch(c -> c.subtotal().equals(Dinero.de(100_000))));
    }

    @Test
    void elPorcentajeDeCienDejaElTotalEnCero() {
        ValorCongelado valor = new ValorCongelado(noches(2, 100_000, 1), 100);

        assertEquals(Dinero.CERO, valor.total());
    }

    @Test
    void unPorcentajeFueraDeRangoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> new ValorCongelado(noches(2, 100_000, 1), 101));
        assertThrows(ReglaDominioException.class, () -> new ValorCongelado(noches(2, 100_000, 1), -1));
    }

    @Test
    void sinDesglosePorNocheNoHayValor() {
        assertThrows(ReglaDominioException.class, () -> new ValorCongelado(List.of(), 10));
        assertThrows(ReglaDominioException.class, () -> new ValorCongelado(null));
    }

    @Test
    void elDesgloseEsUnaCopiaInmutable() {
        List<CargoNoche> original = new ArrayList<>(noches(2, 100_000, 1));
        ValorCongelado valor = new ValorCongelado(original, 0);

        original.clear();

        assertEquals(2, valor.noches());
        assertThrows(UnsupportedOperationException.class, () -> valor.detalle().clear());
    }

    @Test
    void doValoresConElMismoDesgloseYPorcentajeSonIguales() {
        assertEquals(new ValorCongelado(noches(7, 100_000, 1), 10), new ValorCongelado(noches(7, 100_000, 1), 10));
    }
}
