# Matriz de trazabilidad ampliada - SGA Hospeda (Entrega 2)

Relaciona cada regla de negocio (RN-xx) y cada decisión de la Ficha (L-xx) con el código que la implementa, el caso de uso
que la usa, el endpoint que la expone y la evidencia que la prueba. **Una regla solo figura como implementada si tiene
evidencia en la columna de pruebas.** Donde no la tiene, el estado lo dice.

**Estados**
- **Implementada**: regla en el dominio, con prueba unitaria (incluido el caso en que se viola) y, si la API la expone, escenario HTTP.
- **Parcial**: la regla existe en el dominio y está probada, pero falta el caso de uso o endpoint que la expone, o cubre solo una parte.
- **Pendiente**: no está implementada.

**Convenciones.** Las pruebas se citan como `Clase.método`. Los escenarios HTTP (`3.1`, `2.8`...) son los de
[`docs/http/sga.http`](http/sga.http). El contrato de los endpoints está en [`docs/api/openapi.yaml`](api/openapi.yaml).
Paquetes: `domain/entity|valueobject|service` (reglas), `application/usecase` (orquestación), `infrastructure/rest/controller` (endpoints).

---

## 1. Reglas de negocio invariantes (RN)

| ID | Regla (resumen) | Responsable en el dominio | Casos de uso | Endpoint que la expone | Pruebas unitarias (evidencia) | Prueba HTTP | Estado |
|---|---|---|---|---|---|---|---|
| RN-01 | Sin dos reservas activas solapadas | `DisponibilidadApartamentoService`, `Estancia.seSolapaCon` | CU-02, CU-03, CU-04 | `POST /api/disponibilidad`, `POST /api/reservas`, `PUT /api/reservas/{codigo}/modificar` | `DisponibilidadApartamentoServiceTest.noDebePermitirSolapamientoConReservaActivaRN01`; `EstanciaTest.debeDetectarSolapamientoConOtraEstancia` | 3.1, 3.11 | Implementada |
| RN-02 | Ocupantes no exceden la capacidad | `Apartamento.admite`, `DisponibilidadApartamentoService` | CU-02, CU-03, CU-04 | igual que RN-01 | `DisponibilidadApartamentoServiceTest.noDebeAdmitirMasOcupantesQueLaCapacidadRN02`; `ApartamentoTest.admiteOcupantesEnElLimiteExactoDeLaCapacidad` | 3.2 | Implementada |
| RN-03 | La salida es posterior a la entrada | `Estancia` (constructor) | todos los que reciben fechas | `POST /api/cotizaciones`, `POST /api/reservas`, `POST /api/disponibilidad`, `PUT .../modificar`, `POST .../bloqueos` | `EstanciaTest.debeTenerAlMenosUnaNocheRN03`; `ConsultasYBloqueosControllerTest.cotizarConFechasInvalidasParaElNegocioResponde409` | 3.3 | Implementada |
| RN-04 | No se reserva hacia el pasado | `Reserva.crear`, `Reserva.modificarEstancia` | CU-03, CU-04 | `POST /api/reservas`, `PUT .../modificar` | `CrearReservaUseCaseTest.noPermiteReservarHaciaElPasadoRN04YNoAbreFolio`; `ReservaModificacionTest.laNuevaFechaDeEntradaNoPuedeSerPasadaRN04` | 3.4 | Implementada |
| RN-05 | Valor = suma noche por noche de la tarifa de su temporada x ocupantes facturables, **menos el descuento por estancia larga de RP-02** (aplicado una sola vez sobre el total); redondeo al final de cada cargo y del descuento | `CotizadorEstancia`, `CargoNoche.subtotal`, `ValorCongelado` (`bruto`, `descuento`, `total`), `Dinero` (HALF_UP) | CU-01, CU-02, CU-03, CU-04 | `POST /api/cotizaciones` | `CotizadorEstanciaTest.debeLiquidarNochePorNocheRN05`; `CotizadorEstanciaReglasTest.cadaNocheTomaLaTarifaDeSuTemporadaRN05`, `.laNocheDeLaFechaDeSalidaNoSeCobraRN05`, `.rechazaCotizarSiFaltaLaTarifaDeUnaNoche`; `ValorCongeladoTest` (bruto, descuento y total; redondeo al final) | 2.1, 7.8, 7.9 | Implementada (el descuento es RP-02, ver sección 2.1) |
| RN-06 | Facturable si a la fecha de entrada alcanza el umbral; los no facturables cuentan para la capacidad | `Ocupante.esFacturableEn`, `CotizadorEstancia` | CU-01, CU-02, CU-03, CU-04 | `POST /api/cotizaciones` | `OcupanteTest.debeSerFacturableSiAlcanzaElUmbralALaFechaDeEntradaRN06`, `.noDebeSerFacturableSiAunNoAlcanzaElUmbralALaFechaDeEntradaRN06`; `CotizadorEstanciaReglasTest.soloLosOcupantesFacturablesGeneranCargoRN06`, `.unNinoQueNoAlcanzaElUmbralNoCuentaPeroSiCuentaParaLaCapacidadRN06`, `.rechazaUnGrupoSinNingunOcupanteFacturableRN06` | 2.1, 3.5 | Implementada |
| RN-07 | Con bloqueo vigente en una noche no se vende | `Bloqueo.afecta`, `DisponibilidadApartamentoService` | CU-02, CU-03, CU-04, CU-09 | `POST /api/reservas`, `POST /api/disponibilidad` | `DisponibilidadApartamentoServiceTest.noDebePermitirVenderNochesConBloqueoVigenteRN07`, `.unBloqueoQueEmpiezaElDiaDeSalidaNoAfectaLaEstanciaRN07` | 3.14 | Implementada |
| RN-08 | Solo transiciones de estado permitidas | `EstadoReserva.puedeTransicionarA`, `Reserva` (métodos de negocio) | confirmar, CU-04, CU-05 | `PUT .../confirmar`, `PUT .../cancelar`, `PUT .../modificar` | `EstadoReservaTest.soloSePermitenLasTransicionesDeLaSeccion8RN08`, `.losEstadosTerminalesNoTransicionanANingunLado`, `.unaReservaEnCursoNoPuedeCancelarseNiVolverAConfirmadaRN08`; `ReservaCicloDeVidaTest.laSalidaSoloEsPosibleDesdeEnCursoRN08`, `.unaReservaFinalizadaNoAdmiteNingunaOperacionRN08`; `CancelarReservaUseCaseTest.noPermiteCancelarUnaReservaYaCanceladaRN08YNoTocaElFolio`; `ConfirmacionReservaServiceTest.rechazaConfirmarUnaReservaQueNoEstaPendienteSinPedirAnticipoRN08` | 3.8, 3.9, 5.1 | Implementada (incluye llegada, salida y no-show: `ReservaCicloDeVidaTest`) |
| RN-09 | Hora estimada de llegada antes de confirmar | `Reserva.confirmar`, `ConfirmacionReservaService` | confirmar | `PUT /api/reservas/{codigo}/confirmar` | `ReservaTest.noDebeConfirmarSinHoraEstimadaDeLlegadaRN09`; `ConfirmacionReservaServiceTest.seguiraExigiendoLaHoraEstimadaDeLlegadaRN09AunConAnticipoPagado`; `ConfirmarYPagarUseCaseTest.rechazaConfirmarSinHoraEstimadaRN09AunConAnticipo` | 3.6 | Implementada |
| RN-10 | No hay registro antes de la fecha de entrada ni sobre una reserva no CONFIRMADA | `Reserva.registrarLlegada` | CU-06 | `PUT /api/reservas/{codigo}/registrar-llegada` | `ReservaCicloDeVidaTest.elGrupoSeRegistraElDiaDeEntradaDeUnaReservaConfirmadaRN10`, `.noSePermiteElRegistroAntesDeLaFechaDeEntradaRN10`, `.noSePermiteElRegistroSobreUnaReservaQueNoEstaConfirmadaRN10`; `OperacionDiariaUseCasesTest.laLlegadaAntesDeLaFechaDeEntradaRN10NoToqueElApartamento`, `.laLlegadaDeUnaReservaNoConfirmadaRN10EsRechazada`; `RecorridoOperacionDiariaIntegracionTest` (`recorridoDeLlegadaOcupacionCierreDeFolioSalidaYPreparacion`: 409 con el mensaje de la regla un día antes de la entrada, y 200 el día de entrada con el reloj fijo) | 5.1; recorrido con reloj fijo | Implementada |
| RN-11 | Solo un apartamento PREPARADO recibe un grupo | `EstadoOperativo.permiteRegistro`, `Apartamento.marcarOcupado` | CU-06 | `PUT /api/reservas/{codigo}/registrar-llegada` | `EstadoOperativoTest.soloPreparadoPermiteRegistroRN11`, `.noDebePermitirTransicionesIlegales`; `ApartamentoTest.debePermitirTransicionDePreparadoAOcupado`; `OperacionDiariaDominioTest.soloUnApartamentoPreparadoRecibeUnGrupoRN11`; `OperacionDiariaUseCasesTest.laLlegadaConElApartamentoSinPrepararRN11NoGuardaNinguno` | `RecorridoOperacionDiariaIntegracionTest`: la llegada responde 409 `No se puede pasar de X a OCUPADO` con el apartamento FUERA_DE_SERVICIO, PENDIENTE_PREPARACION y EN_PREPARACION (y la reserva sigue CONFIRMADA), y 200 cuando está PREPARADO; el apartamento queda OCUPADO. No es reproducible con `sga.http` (reservas de 2028), sí con el reloj fijo | Implementada |
| RN-12 | Una reserva que deja de estar activa libera sus noches de inmediato | `EstadoReserva.retieneDisponibilidad`; la disponibilidad solo consulta reservas activas | CU-05 | `PUT .../cancelar`, luego `POST /api/disponibilidad` | `EstadoReservaTest.soloLasReservasActivasRetienenDisponibilidadRN12`; `CancelarReservaUseCaseTest.cancelaLaReservaYLiquidaElFolioConLaPoliticaCongelada`; `RecorridoReservaIntegracionTest` (tras cancelar, el cuerpo de `POST /api/disponibilidad` vuelve a traer el apartamento con el mismo valor, y las 4 noches de la reserva modificada también); `RecorridoVencimientoIntegracionTest` y `RecorridoOperacionDiariaIntegracionTest` (no-show) | 2.12 (con `client.test` sobre el cuerpo); recorridos | Implementada. `ReservaJpaRepository.buscarActivasPorApartamento` filtra por `EstadoReserva::esActiva`; el paso 2.12 consulta la disponibilidad tras cancelar (ver sección 4) |
| RN-13 | La retención usa la versión de política congelada en la reserva | `RetencionCancelacionService`, `PoliticaCancelacion.retencion` | CU-05, no-show | `PUT .../cancelar`, `PUT .../declarar-no-show` | `RetencionCancelacionServiceTest` (4 pruebas: 72 h, 71 h 59 min, mismo día, política de otra versión); `CancelarReservaUseCaseTest.usaLaPoliticaDeLaVersionCongeladaNoLaVigente`; `PoliticaCancelacionTest` (tramos y redondeo); `OperacionDiariaDominioTest.elNoShowRetieneElTramoDeAntelacionCeroDeLaPoliticaCongelada`, `.elNoShowRechazaUnaPoliticaDistintaALaCongeladaRN13`; `OperacionDiariaUseCasesTest.elNoShowAplicaLaPoliticaCongeladaYLiberaLaReserva` | 2.10 (cancelación); 5.3 (no-show, por su 409); `RecorridoReservaIntegracionTest` (cancelación a 51 h: retiene el 50 % de la versión CONGELADA y no el 100 % de una política nueva, y el folio lo refleja); `RecorridoOperacionDiariaIntegracionTest` (no-show exitoso: penalidad del 100 %, tramo de antelación 0) | Implementada (cancelación y no-show), con evidencia por HTTP |
| RN-14 | Modificar revalida las condiciones de creación, recalcula y registra el ajuste | `Reserva.modificarEstancia`, `DisponibilidadApartamentoService` (con `reservaExcluida`) | CU-04 | `PUT .../modificar` | `ReservaModificacionTest` (15 pruebas, p. ej. `.noSePuedeModificarUnaReservaEnCursoRN14`); `ModificarReservaUseCaseTest` (11 pruebas) | 2.8, 3.10, 3.11, 3.22, 3.23, 3.24 | Implementada: cambia fechas, ocupantes y, opcionalmente, el apartamento (revalida y recotiza contra el destino) |
| RN-15 | Pago contra un folio con medio y fecha; saldo = cargos - pagos | `Folio.registrarPago`, `Folio.saldo`, `Pago` | CU-08 | `POST .../folio/pagos`, `GET .../folio` | `FolioTest.debeCalcularSaldoComoDiferenciaDeCargosYPagosRN15`; `ConfirmarYPagarUseCaseTest.registraElPagoYElSaldoBajaEnConsecuencia`, `.admiteVariosPagosConMediosDistintos` | 2.6, 2.9 | Implementada |
| RN-16 | Cargos y pagos no se editan ni borran; una corrección es un movimiento inverso | `Folio` (listas no modificables), `Ajuste` | CU-04, CU-05 | `PUT .../modificar`, `PUT .../cancelar` | `FolioTest.noDebePermitirModificarListaDeCargosDesdeAfueraRN16`; `FolioAjustesTest` (9 pruebas) | 2.9, 2.11 | Implementada |
| RN-17 | No se cierra un folio con saldo sin autorización registrada | `Folio.cerrar` | CU-11 | `PUT /api/reservas/{codigo}/folio/cerrar` | `FolioTest.noDebeCerrarConSaldoPendienteSinAutorizacionRN17`; `OperacionDiariaDominioTest.conSaldoPendienteElFolioSoloCierraConAutorizacionRN17`; `OperacionDiariaUseCasesTest.conSaldoPendienteExigeLaAutorizacionRN17YSiNoSeDaNoSeGuardaNada`, `.conSaldoPendienteYAutorizacionLaRegistraEnElFolioCU11` | 5.5, 5.6, 5.7 | Implementada |
| RN-18 | Reserva externa en conflicto: se rechaza y se registra; nunca sobrescribe la vigente | `RechazoReservaExterna` (constancia inmutable); la regla que viola la reserva sale de `DisponibilidadApartamentoService` y de `Reserva.crear` | CU-03 (`CrearReservaUseCase`, transacción sin rollback por la regla) | `POST /api/reservas` (409) y `GET /api/reservas-externas/rechazos` | `ReservaExternaTest` (constancia y sus datos obligatorios); `CrearReservaUseCaseTest` (`unaReservaExternaEnConflictoRN18...`, `cualquierReglaQueViola...`, el conflicto en PORTAL no se registra); `RechazoReservaExternaControllerTest`; `CanalExternoPersistenciaTest` (guardar, leer y listar de 10 en 10) | 3.28, 3.29; `RecorridoCanalExternoIntegracionTest` (el 409 de RN-01, la reserva vigente intacta y la constancia en `GET /api/reservas-externas/rechazos` con canal, identificador, apartamento, fechas, motivo y momento) | Implementada |
| RN-19 | Canal + identificador externo es único; solo los canales externos lo llevan | `IdentificadorExterno`, `Reserva.crear`/`reconstruir` (invariante canal ↔ identificador), `RecepcionReservaExternaService`; respaldo en BD: restricción única `uk_reserva_canal_identificador_externo` | CU-03 (`CrearReservaUseCase`, se evalúa antes que la disponibilidad) | `POST /api/reservas` (409) | `RecepcionReservaExternaServiceTest` (5 pruebas); `ReservaExternaTest`; `CrearReservaUseCaseTest` (mensaje repetido); `CanalExternoPersistenciaTest` (búsqueda por canal e identificador y la restricción única) | 3.26, 3.27, 3.30, 3.31, 3.32; `RecorridoCanalExternoIntegracionTest` (el mismo mensaje dos veces: 409 que nombra la reserva ya creada y sigue habiendo una sola) | Implementada |
| RN-20 | Tiempo de preparación entre la salida y la entrada del mismo día (3.3) | `TiempoPreparacion.permiteEntradaElMismoDia`, `DisponibilidadApartamentoService` (ambos sentidos) | CU-02, CU-03, CU-04 | igual que RN-01 | `TiempoPreparacionTest` (7 pruebas); `DisponibilidadApartamentoServiceTest.noDebePermitirEntradaMismoDiaSiExigePreparacionRN20`, `.debePermitirEntradaMismoDiaSiLaPreparacionCabeEnLaVentanaRN20`, `.noDebePermitirQueLaNuevaEstanciaTermineElDiaQueEmpiezaOtraSiNoHayPreparacionRN20` | `RecorridoOperacionDiariaIntegracionTest` (`elTiempoDePreparacionEntreReservasImpideQueDosEstanciasSeTocanElMismoDiaRN20`): con `sga.alojamiento.tiempo-preparacion-horas=6` la ventana de 4 h no alcanza y dos estancias que se tocan el mismo día dan 409 en ambos sentidos, la búsqueda no ofrece el apartamento y la rechazada no se guarda. Con el valor de `application.properties` (3 h) se permite, por eso `sga.http` no lo reproduce | Implementada |
| RN-21 | Reserva PENDIENTE que supera el plazo de confirmación se cancela automáticamente | `Reserva.haVencido`, `Reserva.vencer`, `PlazoConfirmacion`, `Folio.liquidarVencimiento` | `VencerReservasPendientesUseCase`, disparado por `VencimientoProgramado` | ninguno: es un proceso programado, sin intervención manual | `ReservaCicloDeVidaTest.unaReservaPendienteQueSuperaElPlazoDeConfirmacionSeCancelaRN21`, `.dentroDelPlazoDeConfirmacionNoVenceRN21`, `.soloVenceUnaReservaPendienteRN21`; `VencimientoReservaDominioTest.haVencidoSoloSiEstaPendienteYSuperoElPlazo`, `.unaReservaQueNoEstaPendienteNuncaHaVencido`, `.elVencimientoSinPagosDejaElFolioEnCeroSinPenalidad`, `.siHabiaUnPagoParcialElVencimientoLoDejaComoSaldoAFavor`; `VencerReservasPendientesUseCaseTest.cancelaCadaReservaVencidaRevierteSuFolioYCuentaCuantasFueron`, `.esIdempotenteUnaVezCanceladaYaNoVuelveAAparecer`, `.ignoraLasQueElPuertoDevuelvaPeroTodaviaEstanDentroDelPlazo`; `VencimientoProgramadoTest.ejecutaElCasoDeUsoConLaHoraDelServidor` | ninguno (proceso programado); `RecorridoVencimientoIntegracionTest`: el reloj avanza, se ejecuta el mismo caso de uso que dispara el programador y todo lo demás se comprueba por HTTP sobre el cuerpo (CANCELADA por "SISTEMA", reverso sin penalidad, anticipo como saldo a favor, noches libres, idempotente; justo a las 24 h aún no vence) | Implementada. Con la app en marcha, la reserva PENDIENTE sembrada (creada hoy) no se cancela al arrancar |
| RN-22 | Valor y política quedan congelados al crear; solo cambian por modificación explícita | `Reserva.crear`, `ValorCongelado`, `VersionPolitica` | CU-03, CU-04 | `POST /api/reservas`, `PUT .../modificar` | `CrearReservaUseCaseTest.creaLaReservaPendienteConValorYPoliticaCongeladosYAbreSuFolio`; `ReservaModificacionTest.devuelveLaDiferenciaPositivaAlAmpliarLaEstanciaYConservaLaPolitica` | 2.3, 2.8 | Implementada |

**Resumen RN:** 22 implementadas · 0 parciales · 0 pendientes.

---

## 2. Decisiones de la Ficha de Alojamiento (L)

Las L que son **datos** (nombres, valores de tarifas, fechas de temporadas) las siembra persistencia al arrancar la aplicación; la API
solo ofrece los endpoints para gestionarlas. Los **parámetros** viven en `application.properties` (regla 10), nunca en el código.

| ID | Decisión | Dónde vive / quién la hace cumplir | Endpoint | Pruebas (evidencia) | Estado |
|---|---|---|---|---|---|
| L-01, L-02 | Nombre, identidad y ubicación del alojamiento | **No hay entidad Alojamiento** en el dominio | - | - | Pendiente |
| L-03 | Entre 5 y 15 apartamentos | Datos sembrados por persistencia | `GET /api/apartamentos` | - | Dato de persistencia (no verificable desde este lado) |
| L-04 | Identificación única y estable | `RegistroApartamentoService` | `POST /api/apartamentos` | `ApartamentoUseCasesTest.rechazaUnaIdentificacionRepetidaL04YNoGuarda`; HTTP 3.16 | Implementada |
| L-05 | Dormitorios y capacidad (al menos dos capacidades distintas) | `Apartamento` | `POST/PUT /api/apartamentos` | `ApartamentoTest.actualizarDatosCambiaNombreDormitoriosYCapacidad`, `.actualizarDatosInvalidosNoModificaNada` | Implementada (la variedad de capacidades es dato sembrado) |
| L-06 | Descripción, dotación y características del apartamento, y su filtro de búsqueda | `Apartamento` (descripción obligatoria ≤ 500; dotación: lista de textos sin repetidos, ≤ 30 de ≤ 60; características: `Set` del enum cerrado `Caracteristica`; `actualizarDatos` atómico y `tieneTodas` para el filtro). Filtro AND en `ConsultarDisponibilidadUseCase` (`Apartamento.tieneTodas`) y en el listado (`FiltroApartamentos` + `ApartamentoEspecificaciones`). Persistencia: columna `descripcion` y `@ElementCollection` (`apartamento_dotacion`, `apartamento_caracteristica`). Datos sembrados: los 7 apartamentos, con ≥ 1 característica diferenciadora | `POST/PUT /api/apartamentos`, `GET /api/apartamentos?caracteristicas=`, `POST /api/disponibilidad` (campo `caracteristicas`), `GET /api/caracteristicas` | **Unitarias:** `ApartamentoTest` (`laDescripcionEsObligatoriaYTieneUnTopeL06`, `laDotacionNoAdmiteElementosEnBlancoRepetidosNiDemasiadosL06`, `unaCaracteristicaNulaSeRechazaL06`, `tieneTodasExigeCadaUnaDeLasCaracteristicasPedidasL06`, `siLaDotacionDeLaActualizacionEsInvalidaNoSeModificaNingunDatoL06`, `losGettersDevuelvenColeccionesInmodificables...`); `ReconstruccionTest`; `ApartamentoUseCasesTest` (`rechazaUnaDescripcionVaciaOUnaDotacionRepetidaL06YNoGuarda`, `listaConFiltroDeCaracteristicas...`); `ConsultarDisponibilidadUseCaseTest` (3 pruebas L-06). **Persistencia:** `ApartamentoCaracteristicasJpaTest` (8: ida y vuelta leyendo de la base, reemplazo sin restos, filtro simple, filtro AND, sin coincidencias, paginado sin duplicados), `ApartamentoPersistenceMapperTest`. **Controller:** `ApartamentoControllerTest`, `CaracteristicaControllerTest`, `ConsultasYBloqueosControllerTest`. **Integración (cuerpos):** `ApartamentoCaracteristicasIntegracionTest` (17) y `ApartamentoEquipamientoIntegracionTest` (6); `OpenApiCoherenciaTest`. **HTTP:** 1.2, 1.7 a 1.15, 3.15a a 3.16f, con `client.test` sobre el cuerpo | Implementada. La dotación repetida es 409 (regla del dominio); la forma (descripción vacía o larga, característica fuera del catálogo) es 400. El catálogo es un vocabulario cerrado del negocio, no un parámetro de la Ficha. `GET /api/caracteristicas` devuelve la lista completa sin paginar porque no es un listado de recursos. El catálogo y las características sembradas por apartamento son los acordados por el equipo para la columna "Características" de la Ficha A.2 |
| L-07 | Valores de las tarifas | `RegistroTarifaService`, `TarifarioRepository` | `PUT /api/apartamentos/{id}/tarifas/{temporada}` | `RegistroPoliticaYTarifaServiceTest` (3 pruebas de tarifa); HTTP 3.17 | Implementada (los valores son datos sembrados) |
| L-08 | Temporadas (mínimo 2 además de la base) | `Temporada`, `CalendarioTemporadas` | `POST/GET /api/temporadas` | `CalendarioTemporadasTest` (7 pruebas); `TarifarioUseCasesTest` (temporadas); HTTP 3.18, 3.19 | Implementada |
| L-09 | Umbral de edad facturable | `UmbralEdadFacturable`; propiedad `sga.alojamiento.umbral-edad-facturable-anios` | (afecta cotización) | `UmbralEdadFacturableTest`; `DomainServiceConfigTest.exponeLosParametrosDeLaFicha` | Implementada |
| L-10 | Tramos de la política de cancelación (mínimo 2) | `PoliticaCancelacion`, `TramoRetencion` | `POST /api/politicas-cancelacion` | `PoliticaCancelacionTest.exigeAlMenosDosTramos`, `.exigeUnTramoDeAntelacionCeroYSinRepetidos`; HTTP 3.20, 3.21 | Implementada |
| L-11 | Exigencia y monto del anticipo | `ConfirmacionReservaService`; propiedad `sga.alojamiento.anticipo-porcentaje` | `PUT .../confirmar` | `ConfirmacionReservaServiceTest` (9 pruebas) | Implementada |
| L-12 | Horas de entrada y salida | propiedades `sga.alojamiento.hora-entrada` / `hora-salida`; usadas por RN-13 y RN-20 | (afecta disponibilidad y cancelación) | `DomainServiceConfigTest.exponeLosParametrosDeLaFicha`; `RetencionCancelacionServiceTest` | Implementada |
| L-13 | Tiempo de preparación | `TiempoPreparacion`; propiedad `sga.alojamiento.tiempo-preparacion-horas` | (afecta disponibilidad) | `TiempoPreparacionTest` | Implementada |
| L-14 | Plazo de confirmación | `PlazoConfirmacion`; propiedad `sga.alojamiento.plazo-confirmacion-horas` | ninguno: lo usa el proceso programado (ver RN-21) | `ReservaCicloDeVidaTest` (vencimiento); `VencerReservasPendientesUseCaseTest.consultaLasPendientesCreadasAntesDelLimiteQueCalculaElPlazo` | Implementada |
| L-15 | Hora límite de no-show | `Reserva.declararNoShow`; propiedad `sga.alojamiento.hora-limite-no-show` | `PUT /api/reservas/{codigo}/declarar-no-show` | `ReservaCicloDeVidaTest.elNoShowSoloSeDeclaraVencidaLaHoraLimiteDelDiaDeEntrada`, `.elNoShowSoloAplicaAReservasConfirmadasRN08`; `OperacionDiariaUseCasesTest.antesDeLaHoraLimiteNoSePuedeDeclararNoShowYNoSeLiquidaNada` (HTTP 5.3); `RecorridoOperacionDiariaIntegracionTest` (`elNoShowSoloSeDeclaraDesdeLaHoraLimiteYAplicaLaPoliticaCongelada`: 409 a las 12:00 y a las 21:59, 200 a las 22:00 en punto) | Implementada |
| L-16 | Mascotas | - | - | - | Pendiente |
| L-17 | Servicios adicionales y su cargo | `Folio.registrarCargo` existe | **ninguno** | - | Pendiente (sin caso de uso ni endpoint) |
| L-18 | Medios de pago (mínimo 2) | `Pago.medio` es texto libre; no hay lista configurada | `POST .../folio/pagos` | `ConfirmarYPagarUseCaseTest.admiteVariosPagosConMediosDistintos` | Parcial: no se valida contra una lista de medios aceptados |
| L-19 | Reportes adicionales | - | - | - | Pendiente |
| L-20 | Tres reglas propias aprobadas por el docente | RP-01 `EstanciaMinimaPorTemporada` + `CotizadorEstancia`; RP-02 `PoliticaDescuentoEstanciaLarga` + `ValorCongelado`; RP-03 `EdadAdultoResponsable` + `Ocupante.esAdultoEn` + `Reserva`. Detalle en la sección 2.1 | `POST /api/cotizaciones`, `POST /api/reservas`, `PUT /api/reservas/{codigo}/modificar`, `POST /api/disponibilidad` | Sección 2.1 | **Implementada - aprobación del docente solicitada el 15-nov-2026** |
| L-21 | Caso de uso del modelo de IA (no bloqueante) | - | - | - | Pendiente |
| L-22 | Integración externa | `RecepcionReservaExternaService`, `RechazoReservaExterna` | `POST /api/reservas`, `GET /api/reservas-externas/rechazos` | 3.26 a 3.32 | Implementada (ver RN-18, RN-19) |
| L-23 | Funcionalidad opcional elegida | - | - | - | Pendiente |

---

## 2.1 Reglas propias (L-20)

Tres reglas adicionales, coherentes con el alojamiento y con consecuencia verificable (10.1): dos rechazan (409) y una calcula
distinto. **Aprobación del docente solicitada el 15-nov-2026**; este documento queda como "Implementada - aprobación del
docente solicitada el 15-nov-2026" hasta que llegue su respuesta. Los parámetros viven en `application.properties` (regla 10).

| Parámetro | Propiedad | Valor |
|---|---|---|
| RP-01: noches mínimas de ALTA y de MEDIA | `sga.alojamiento.estancia-minima.ALTA` / `.MEDIA` | 3 / 2 |
| RP-02: desde cuántas noches se descuenta y cuánto | `sga.alojamiento.descuento-estancia-larga-noches` / `-porcentaje` | 7 / 10 % |
| RP-03: edad del adulto responsable | `sga.alojamiento.edad-adulto-responsable-anios` | 18 |

La clave de RP-01 es el nombre de la temporada (un dato, L-08): se pueden agregar mínimos para otras temporadas sin tocar el código.

| Regla | Artefacto | Razón | Endpoint y código | Pruebas (evidencia) | Estado |
|---|---|---|---|---|---|
| **RP-01** Estancia mínima por temporada: si alguna noche cae en una temporada con mínimo, la estancia debe tener al menos ese número de noches | Objeto de valor `EstanciaMinimaPorTemporada` (mapa temporada → noches, con `verificar`), evaluado en el servicio de dominio `CotizadorEstancia` | Ningún agregado la evalúa solo: necesita la temporada de cada noche, que viene del tarifario. El cotizador ya la conoce en cada `CargoNoche`; el objeto de valor guarda el dato y su comportamiento. Como cotizar es el paso común, aplica sola al cotizar, crear, modificar y buscar | `POST /api/cotizaciones`, `POST /api/reservas`, `PUT /api/reservas/{codigo}/modificar` y `POST /api/disponibilidad` -> **409** `En temporada ALTA la estancia mínima es de 3 noches` | `EstanciaMinimaPorTemporadaTest` (13: límites 2/3 en ALTA y 1/2 en MEDIA, cruce BASE→ALTA, mayor mínimo, configuración inválida); `CotizadorEstanciaReglasTest` (`altaConDosNochesSeRechazaRP01`, `altaConTresNochesSeCotizaRP01`, `mediaExigeDosNochesRP01`, `unaEstanciaQueCruzaDeBaseAAltaConDosNochesSeRechazaPorLaNocheEnAltaRP01`, `sinEstanciasMinimasConfiguradas...`); `DomainServiceConfigTest`; `ReglasPropiasIntegracionTest.rp01_*` (9, sobre el cuerpo: `$.status`=409, `$.mensaje` exacto, sin trazas); `SembradoEnJunioTest` y `SembradoEnDiciembreTest`. HTTP 7.1 a 7.4, 7.15 | Implementada |
| **RP-02** Descuento por estancia larga: desde 7 noches, 10 % sobre el total, una sola vez y con redondeo al final | Objeto de valor `PoliticaDescuentoEstanciaLarga` (decide el porcentaje) y `ValorCongelado` ampliado con `porcentajeDescuento`, `bruto()`, `descuento()`, `total()` | El descuento es parte del valor que se congela (RN-22): vive donde nace y se protege el valor. El cotizador solo le pasa el porcentaje. El constructor anterior de `ValorCongelado` se conserva y equivale a 0 % | `POST /api/cotizaciones`, `POST /api/disponibilidad` y `POST /api/reservas` exponen `subtotalBruto`, `porcentajeDescuento`, `descuento` y `total` (la reserva: `valorTotal`, que es el neto). **No rechaza**: calcula distinto, por eso no tiene 409 | `ValorCongeladoTest` (12: 6 contra 7 noches, el redondeo 35.003,5 → 35.004, una sola vez y no noche a noche, límites 0 y 100 %); `PoliticaDescuentoEstanciaLargaTest` (6); `CotizadorEstanciaReglasTest` (`conSeisNochesNoHayDescuentoRP02`, `conSieteNochesSeDescuentaElDiezPorCientoRP02`, `elDescuentoSeAplicaUnaSolaVezSobreElTotalRP02`); `ReservaPersistenciaTest.el_porcentaje_de_descuento_se_guarda_y_se_lee_de_la_base_de_datos_rp02`; `ReglasPropiasIntegracionTest.rp02_*` (6: la reserva congela el mismo total que la cotización, el folio y el ajuste al modificar). HTTP 7.8 a 7.14, 7.16 | Implementada |
| **RP-03** Adulto responsable: toda reserva incluye al menos un ocupante con 18 años o más a la fecha de entrada | Objeto de valor `EdadAdultoResponsable`, `Ocupante.esAdultoEn` y la invariante en el agregado `Reserva` (`crear` y `modificarEstancia`) | Es una invariante del grupo de ocupantes, que el agregado posee. La edad llega como parámetro, igual que `ahora` (RN-04) y el umbral facturable (RN-06). Es distinta de L-09: un ocupante de 12 a 17 años paga, pero no responde por el grupo | `POST /api/reservas` y `PUT /api/reservas/{codigo}/modificar` -> **409** `La reserva debe incluir al menos un ocupante mayor de 18 años` | `EdadAdultoResponsableTest`; `OcupanteTest` (17 años y 364 días no es adulto, exactamente 18 sí); `ReservaTest` (7 de creación); `ReservaModificacionTest` (3 de modificación, y no deja rastro); `CrearReservaUseCaseTest` y `ModificarReservaUseCaseTest` (no guardan nada); `ReglasPropiasIntegracionTest.rp03_*` (5). HTTP 7.5 a 7.7, 7.17 a 7.19 | Implementada |

**Decisiones de diseño que conviene poder sustentar**
- **RP-03 no se valida al cotizar.** La cotización calcula un precio y no crea nada; la composición del grupo se exige donde se
  compromete la reserva (al crear y al modificar). Así un cliente puede explorar precios con un grupo provisional sin que el
  sistema lo rechace, y la regla se aplica una sola vez, en el agregado que protege el grupo. El paso 7.7 de `sga.http` y
  `rp03_laCotizacionNoValidaLaComposicionDelGrupoSoloElPrecio` lo demuestran.
- **El descuento se recalcula al modificar con los parámetros vigentes**, igual que las tarifas (7.5); la política de cancelación
  congelada no cambia. Pasar de 7 a 6 noches pierde el descuento y el ajuste del folio es la diferencia entre los totales
  (`rp02_alModificar...`).
- **Una búsqueda de disponibilidad que viola RP-01 responde 409**, no una lista vacía: el mínimo depende de la temporada y no
  del apartamento, así que ninguno podría alojar esa estancia, y el mensaje le dice al cliente qué ajustar.
- **Orden de validación:** disponibilidad (RN-01, RN-02, RN-07, RN-20) → cotización (RN-06 y RP-01) → `Reserva.crear` (RP-03,
  RN-04, titular). Por eso 3.14 y 3.23 de `sga.http` usan 3 noches: el 409 que prueban es el del bloqueo (RN-07).
- **Datos sembrados:** todas las estancias de `SembradorReservasPrueba` duran 3 noches (el mayor mínimo) y cada reserva incluye
  adultos, para que el arranque no dependa del día. `SembradoEnJunioTest` (5 de junio) y `SembradoEnDiciembreTest` (10 de
  diciembre) arrancan la aplicación con un reloj fijo en temporada ALTA; con estancias de 2 noches el contexto no levanta.
- `Reserva.reconstruir` no valida RP-03: restaurar una reserva guardada no es una operación de negocio (igual que RN-04).

---

## 3. Casos de uso (CU) y su endpoint

| CU | Caso de uso | Clase | Endpoint | Estado |
|---|---|---|---|---|
| CU-01 | Cotizar estancia | `CotizarEstanciaUseCase` | `POST /api/cotizaciones` | Implementado |
| CU-02 | Consultar disponibilidad | `ConsultarDisponibilidadUseCase` | `POST /api/disponibilidad` | Implementado |
| CU-03 | Crear reserva | `CrearReservaUseCase` | `POST /api/reservas` | Implementado |
| CU-04 | Modificar reserva | `ModificarReservaUseCase` | `PUT /api/reservas/{codigo}/modificar` | Implementado (fechas, ocupantes y, opcionalmente, apartamento) |
| CU-05 | Cancelar reserva | `CancelarReservaUseCase` | `PUT /api/reservas/{codigo}/cancelar` | Implementado |
| CU-06 | Registrar llegada | `RegistrarLlegadaUseCase` | `PUT /api/reservas/{codigo}/registrar-llegada` | Implementado |
| CU-07 | Registrar salida | `RegistrarSalidaUseCase` | `PUT /api/reservas/{codigo}/registrar-salida` | Implementado (exige el folio cerrado, 7.6) |
| CU-08 | Registrar pago en folio | `RegistrarPagoUseCase` | `POST /api/reservas/{codigo}/folio/pagos` | Implementado |
| CU-09 | Registrar bloqueo | `RegistrarBloqueoUseCase` | `POST /api/apartamentos/{identificacion}/bloqueos` | Implementado |
| CU-10 | Preparar apartamento | `IniciarPreparacionUseCase`, `MarcarPreparadoUseCase` | `PUT /api/apartamentos/{id}/estado-operativo/iniciar-preparacion` y `/marcar-preparado` | Implementado |
| CU-11 | Autorizar cierre de folio con saldo | `CerrarFolioUseCase` | `PUT /api/reservas/{codigo}/folio/cerrar` | Implementado |
| - | Declarar no-show | `DeclararNoShowUseCase` | `PUT /api/reservas/{codigo}/declarar-no-show` | Implementado |
| - | Vencimiento automático de reservas pendientes (RN-21) | `VencerReservasPendientesUseCase` + `VencimientoProgramado` (cada 5 min) | ninguno (proceso programado) | Implementado |
| - | Fuera de servicio y reincorporación | `DeclararFueraDeServicioUseCase`, `ReincorporarApartamentoUseCase` | `PUT /api/apartamentos/{id}/estado-operativo/declarar-fuera-de-servicio` y `/reincorporar` | Implementado |
| - | Confirmar reserva | `ConfirmarReservaUseCase` | `PUT /api/reservas/{codigo}/confirmar` | Implementado |
| - | Rechazos de reservas externas (RN-18) | `ListarRechazosReservasExternasUseCase` | `GET /api/reservas-externas/rechazos` | Implementado |
| - | Consultas de reserva y folio | `ConsultarReservaUseCase`, `ListarReservasUseCase`, `ConsultarFolioUseCase` | `GET /api/reservas` (con `salidaDesde`, `salidaHasta` y `nombreTitular`), `GET /api/reservas/{codigo}`, `GET .../folio` | Implementado |
| - | Mis reservas del huésped | `ListarMisReservasUseCase` | `GET /api/reservas/mias` (solo HUESPED; el titular sale del claim `documento` del token) | Implementado |
| - | Gestión de apartamentos | `CrearApartamentoUseCase`, `ConsultarApartamentoUseCase`, `ListarApartamentosUseCase`, `ActualizarApartamentoUseCase`, `ActivarApartamentoUseCase`, `DarDeBajaApartamentoUseCase` | `/api/apartamentos` (6 endpoints, con filtros por características y por `estadoOperativo` en el listado) y `GET /api/caracteristicas` (`ListarCaracteristicasUseCase`) | Implementado |
| - | Temporadas, tarifas y política | `RegistrarTemporadaUseCase`, `ListarTemporadasUseCase`, `RegistrarTarifaUseCase`, `ListarTarifasUseCase`, `RegistrarPoliticaCancelacionUseCase`, `ConsultarPoliticaVigenteUseCase`, `ConsultarPoliticaPorVersionUseCase` | `/api/temporadas`, `/api/apartamentos/{id}/tarifas`, `/api/politicas-cancelacion` | Implementado |

---

## 4. Verificaciones

Las verificaciones de la Entrega 2 son de **cuerpo y regla de negocio**, no solo de código HTTP (observación del profesor).

**Suite automática (`./gradlew test`, también con `--rerun-tasks`): 739 pruebas, 0 fallos, 0 omitidas.** Incluye:
- Unitarias del dominio y de los casos de uso, con el caso en que cada regla se viola; de persistencia (cada lectura después de vaciar la sesión
  de Hibernate) y de controllers.
- **Recorridos de extremo a extremo** con `@SpringBootTest` + MockMvc, login real, base H2 propia por clase y un reloj que la prueba mueve
  (`RelojMutable`), verificando el CUERPO de cada respuesta: `RecorridoReservaIntegracionTest` (2), `RecorridoOperacionDiariaIntegracionTest` (3),
  `RecorridoVencimientoIntegracionTest` (1) y `RecorridoCanalExternoIntegracionTest` (2). Toda respuesta 409 cumple la aserción común: `$.status` = 409,
  `$.mensaje` con el texto exacto de la regla, `$.errores` nulo y sin "trace", "exception" ni nombres de clase Java.
- `ReglasPropiasIntegracionTest` (20, L-20), `ApartamentoCaracteristicasIntegracionTest` (17) y `ApartamentoEquipamientoIntegracionTest` (6, L-06),
  `SoporteFrontendIntegracionTest` (14) y `CorsIntegracionTest` (7).
- `SembradoEnJunioTest` y `SembradoEnDiciembreTest`: la aplicación arranca con el reloj en temporada ALTA.
- `OpenApiCoherenciaTest`: las operaciones de `docs/api/openapi.yaml` son exactamente las de la aplicación y todo endpoint protegido documenta 401 y 403.

**Colección `docs/http/sga.http`** (detalle y reproducción en [`docs/http/resultado-corrida.md`](http/resultado-corrida.md)): se ejecutó completa contra la
aplicación recién arrancada: **168 solicitudes, 168 `client.test` y 168 en verde**, con cero fallos. Cada solicitud verifica el cuerpo de su
respuesta, y cada 409 verifica que el mensaje incluya el texto de la regla. La corrida de verificación encontró y corrigió dos defectos previos del
propio `.http`: el paso 6.18 reservaba las mismas noches que la reserva externa de 3.26 (409 por RN-01) y las URL con espacios de la PARTE 8 debían
ir codificadas. **Paso manual:** 6.14 (token vencido), que exige arrancar con la expiración en 1 minuto; lo verifica `JwtConfigTest`.

**Lo que antes no tenía evidencia por HTTP y ahora sí**, con el reloj fijo: la llegada y su rechazo (RN-10), el apartamento que no está preparado (RN-11),
la liberación inmediata tras cancelar (RN-12), el tiempo de preparación (RN-20), el vencimiento automático (RN-21) y la hora límite del no-show (L-15).
RN-12 ya no se comprueba solo con el código 200: el cuerpo de la disponibilidad trae de nuevo el apartamento con su valor.

Decisiones que se dejaron anotadas:
- La llegada, el no-show y el vencimiento no se pueden reproducir con `sga.http` porque las reservas de la colección son de 2028 y el servidor decide
  "ahora"; por eso viven en los recorridos con reloj movido. RN-20 depende de los parámetros: el recorrido sube el tiempo de preparación a 6 horas.
- **RP-03 no se valida al cotizar** (ver sección 2.1): la cotización calcula precio y la composición del grupo se exige al reservar y al modificar.
- Las aserciones de los pasos de `sga.http` anteriores a L-06 y L-20 se generaron a partir de las respuestas de una corrida verificada: comprueban la forma
  del cuerpo, los valores estables y el mensaje de cada regla; los importes exactos se verifican en la PARTE 7 y en los recorridos de Java.

Sigue pendiente (acotado, sin bloquear el frontend):
- Verificar automáticamente que los **esquemas** del yaml (campos de cada request y response) coincidan con los records del código; hoy se
  comprueban las operaciones y los 401/403, y los esquemas se revisan a mano.
- Los pendientes de la Ficha que no son del backend actual: L-01/L-02, L-16, L-17, L-19, L-21 y L-23 (ver la sección 2).

---

## Seguridad (Guía 12, proyecto 7.1 y 7.2)

Las reglas de acceso **no son reglas de negocio**: no viven en el dominio (el usuario es un concepto de acceso, 3.6),
se evalúan en `@PreAuthorize` y su incumplimiento es 401/403, no 409. El detalle está en
[`docs/seguridad.md`](seguridad.md).

| ID | Regla de acceso | Responsable | Endpoint que la expone | Pruebas (evidencia) | Prueba HTTP | Estado |
|---|---|---|---|---|---|---|
| S-01 | Autenticación con JWT (HS256, emisor y expiración validados; algoritmo fijo) | `JwtConfig`, `JwtTokenProvider`, `SecurityConfig` | todos menos login y registro | `JwtConfigTest` (8 pruebas); `SeguridadApiIntegracionTest.caso6`, `caso13` | 6.6, 6.13, 6.13b, 6.14 | Implementada |
| S-02 | Login con BCrypt y mensaje genérico ante credenciales inválidas o usuario inactivo | `SecurityServiceImpl`, `UserConfig`, `CustomUserDetails`, `GlobalExceptionHandler` | `POST /api/auth/login` | `SecurityServiceImplTest`; `SecurityControllerTest`; `SeguridadApiIntegracionTest.caso3`, `caso4` | 0.1–0.7, 6.3–6.5 | Implementada |
| S-03 | Registro público: siempre HUESPED, correo y documento únicos, contraseña segura | `RegistroUsuarioRequest`, `SecurityServiceImpl` | `POST /api/auth/registro` | `SecurityServiceImplTest`; `SecurityControllerTest`; `SeguridadApiIntegracionTest.elRegistroPublico...` | 0.6, 6.19–6.21 | Implementada |
| S-04 | Cada endpoint protegido según el rol (matriz 7.1) | `@PreAuthorize` en cada controller | todos | `SeguridadApiIntegracionTest.caso7`, `caso8`, `caso12` | 6.7, 6.8, 6.12–6.12d | Implementada |
| S-05 | Un huésped solo accede a sus reservas y folios (propietario del recurso) | `PropietarioReservaVerificador` | `GET /api/reservas/{codigo}`, `PUT .../cancelar`, `GET .../folio` | `PropietarioReservaVerificadorTest` (6 pruebas); `SeguridadApiIntegracionTest.caso9y10` | 6.9–6.10d | Implementada |
| S-06 | Rol y canal: huésped por PORTAL a su nombre; recepción por DIRECTO; administrador DIRECTO o EXTERNO | `CanalReservaVerificador` | `POST /api/reservas` | `CanalReservaVerificadorTest` (7 pruebas); `SeguridadApiIntegracionTest.elHuespedNoReserva...` | 6.15–6.18 | Implementada |
| S-07 | Solo el administrador autoriza el cierre de un folio con saldo (CU-11) | `CierreFolioVerificador` | `PUT /api/reservas/{codigo}/folio/cerrar` | `CierreFolioVerificadorTest` | 5.6, 5.7, 6.12d | Implementada |
| S-09 | CORS: solo los orígenes de `sga.cors.origenes-permitidos` (por defecto `http://localhost:5173`); GET, POST, PUT y OPTIONS; encabezados `Authorization` y `Content-Type`; `Location` expuesta; sin comodín ni credenciales | `SecurityConfig.corsConfigurationSource` | todos los `/api/**` | `CorsIntegracionTest` (7: preflight del origen autorizado sin token, DELETE y origen ajeno rechazados con 403 sin `Access-Control-Allow-Origin`, `Location` expuesta, la configuración no usa `*`) | 8.1 a 8.3 | Implementada |
| S-10 | Mis reservas: solo HUESPED y solo las suyas, por el claim `documento` (nunca un parámetro) | `@PreAuthorize("hasAuthority('HUESPED')")`, `ListarMisReservasUseCase` | `GET /api/reservas/mias` | `SoporteFrontendIntegracionTest.mias_*` (un huésped no ve las de otro, 403 para los demás roles, 401 sin token, paginado de 10 en 10 y de la más reciente a la más antigua); `ReservaControllerTest` (usa el documento del token aunque la petición pase otro) | 8.4 a 8.10 | Implementada |
| S-08 | La clave del JWT no se versiona | `.gitignore`, `application-secrets.properties.example`, `SGA_JWT_SECRET` | — | `JwtConfigTest.unaClaveDeMenosDe32CaracteresImpideArrancar` | `git status --ignored` | Implementada |
