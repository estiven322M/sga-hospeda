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
| RN-05 | Valor = suma noche por noche de la tarifa de su temporada x ocupantes facturables; redondeo al final de cada cargo | `CotizadorEstancia`, `CargoNoche.subtotal`, `Dinero` (HALF_UP) | CU-01, CU-02, CU-03, CU-04 | `POST /api/cotizaciones` | `CotizadorEstanciaTest.debeLiquidarNochePorNocheRN05`; `CotizadorEstanciaReglasTest.cadaNocheTomaLaTarifaDeSuTemporadaRN05`, `.laNocheDeLaFechaDeSalidaNoSeCobraRN05`, `.rechazaCotizarSiFaltaLaTarifaDeUnaNoche` | 2.1 | Implementada |
| RN-06 | Facturable si a la fecha de entrada alcanza el umbral; los no facturables cuentan para la capacidad | `Ocupante.esFacturableEn`, `CotizadorEstancia` | CU-01, CU-02, CU-03, CU-04 | `POST /api/cotizaciones` | `OcupanteTest.debeSerFacturableSiAlcanzaElUmbralALaFechaDeEntradaRN06`, `.noDebeSerFacturableSiAunNoAlcanzaElUmbralALaFechaDeEntradaRN06`; `CotizadorEstanciaReglasTest.soloLosOcupantesFacturablesGeneranCargoRN06`, `.unNinoQueNoAlcanzaElUmbralNoCuentaPeroSiCuentaParaLaCapacidadRN06`, `.rechazaUnGrupoSinNingunOcupanteFacturableRN06` | 2.1, 3.5 | Implementada |
| RN-07 | Con bloqueo vigente en una noche no se vende | `Bloqueo.afecta`, `DisponibilidadApartamentoService` | CU-02, CU-03, CU-04, CU-09 | `POST /api/reservas`, `POST /api/disponibilidad` | `DisponibilidadApartamentoServiceTest.noDebePermitirVenderNochesConBloqueoVigenteRN07`, `.unBloqueoQueEmpiezaElDiaDeSalidaNoAfectaLaEstanciaRN07` | 3.14 | Implementada |
| RN-08 | Solo transiciones de estado permitidas | `EstadoReserva.puedeTransicionarA`, `Reserva` (métodos de negocio) | confirmar, CU-04, CU-05 | `PUT .../confirmar`, `PUT .../cancelar`, `PUT .../modificar` | `EstadoReservaTest.soloSePermitenLasTransicionesDeLaSeccion8RN08`, `.losEstadosTerminalesNoTransicionanANingunLado`, `.unaReservaEnCursoNoPuedeCancelarseNiVolverAConfirmadaRN08`; `ReservaCicloDeVidaTest.laSalidaSoloEsPosibleDesdeEnCursoRN08`, `.unaReservaFinalizadaNoAdmiteNingunaOperacionRN08`; `CancelarReservaUseCaseTest.noPermiteCancelarUnaReservaYaCanceladaRN08YNoTocaElFolio`; `ConfirmacionReservaServiceTest.rechazaConfirmarUnaReservaQueNoEstaPendienteSinPedirAnticipoRN08` | 3.8, 3.9, 5.1 | Implementada (incluye llegada, salida y no-show: `ReservaCicloDeVidaTest`) |
| RN-09 | Hora estimada de llegada antes de confirmar | `Reserva.confirmar`, `ConfirmacionReservaService` | confirmar | `PUT /api/reservas/{codigo}/confirmar` | `ReservaTest.noDebeConfirmarSinHoraEstimadaDeLlegadaRN09`; `ConfirmacionReservaServiceTest.seguiraExigiendoLaHoraEstimadaDeLlegadaRN09AunConAnticipoPagado`; `ConfirmarYPagarUseCaseTest.rechazaConfirmarSinHoraEstimadaRN09AunConAnticipo` | 3.6 | Implementada |
| RN-10 | No hay registro antes de la fecha de entrada ni sobre una reserva no CONFIRMADA | `Reserva.registrarLlegada` | CU-06 | `PUT /api/reservas/{codigo}/registrar-llegada` | `ReservaCicloDeVidaTest.elGrupoSeRegistraElDiaDeEntradaDeUnaReservaConfirmadaRN10`, `.noSePermiteElRegistroAntesDeLaFechaDeEntradaRN10`, `.noSePermiteElRegistroSobreUnaReservaQueNoEstaConfirmadaRN10`; `OperacionDiariaUseCasesTest.laLlegadaAntesDeLaFechaDeEntradaRN10NoToqueElApartamento`, `.laLlegadaDeUnaReservaNoConfirmadaRN10EsRechazada` | 5.1 | Implementada |
| RN-11 | Solo un apartamento PREPARADO recibe un grupo | `EstadoOperativo.permiteRegistro`, `Apartamento.marcarOcupado` | CU-06 | `PUT /api/reservas/{codigo}/registrar-llegada` | `EstadoOperativoTest.soloPreparadoPermiteRegistroRN11`, `.noDebePermitirTransicionesIlegales`; `ApartamentoTest.debePermitirTransicionDePreparadoAOcupado`; `OperacionDiariaDominioTest.soloUnApartamentoPreparadoRecibeUnGrupoRN11`; `OperacionDiariaUseCasesTest.laLlegadaConElApartamentoSinPrepararRN11NoGuardaNinguno` | no reproducible por HTTP: las reservas del `.http` son de 2028 y la llegada exige haber alcanzado la fecha de entrada | Implementada |
| RN-12 | Una reserva que deja de estar activa libera sus noches de inmediato | `EstadoReserva.retieneDisponibilidad`; la disponibilidad solo consulta reservas activas | CU-05 | `PUT .../cancelar`, luego `POST /api/disponibilidad` | `EstadoReservaTest.soloLasReservasActivasRetienenDisponibilidadRN12`; `CancelarReservaUseCaseTest.cancelaLaReservaYLiquidaElFolioConLaPoliticaCongelada` | 2.12 | Implementada. `ReservaJpaRepository.buscarActivasPorApartamento` filtra por `EstadoReserva::esActiva`; el paso 2.12 consulta la disponibilidad tras cancelar (ver sección 4) |
| RN-13 | La retención usa la versión de política congelada en la reserva | `RetencionCancelacionService`, `PoliticaCancelacion.retencion` | CU-05, no-show | `PUT .../cancelar`, `PUT .../declarar-no-show` | `RetencionCancelacionServiceTest` (4 pruebas: 72 h, 71 h 59 min, mismo día, política de otra versión); `CancelarReservaUseCaseTest.usaLaPoliticaDeLaVersionCongeladaNoLaVigente`; `PoliticaCancelacionTest` (tramos y redondeo); `OperacionDiariaDominioTest.elNoShowRetieneElTramoDeAntelacionCeroDeLaPoliticaCongelada`, `.elNoShowRechazaUnaPoliticaDistintaALaCongeladaRN13`; `OperacionDiariaUseCasesTest.elNoShowAplicaLaPoliticaCongeladaYLiberaLaReserva` | 2.10 (cancelación); 5.3 (no-show, por su 409) | Implementada (cancelación y no-show) |
| RN-14 | Modificar revalida las condiciones de creación, recalcula y registra el ajuste | `Reserva.modificarEstancia`, `DisponibilidadApartamentoService` (con `reservaExcluida`) | CU-04 | `PUT .../modificar` | `ReservaModificacionTest` (15 pruebas, p. ej. `.noSePuedeModificarUnaReservaEnCursoRN14`); `ModificarReservaUseCaseTest` (11 pruebas) | 2.8, 3.10, 3.11, 3.22, 3.23, 3.24 | Implementada: cambia fechas, ocupantes y, opcionalmente, el apartamento (revalida y recotiza contra el destino) |
| RN-15 | Pago contra un folio con medio y fecha; saldo = cargos - pagos | `Folio.registrarPago`, `Folio.saldo`, `Pago` | CU-08 | `POST .../folio/pagos`, `GET .../folio` | `FolioTest.debeCalcularSaldoComoDiferenciaDeCargosYPagosRN15`; `ConfirmarYPagarUseCaseTest.registraElPagoYElSaldoBajaEnConsecuencia`, `.admiteVariosPagosConMediosDistintos` | 2.6, 2.9 | Implementada |
| RN-16 | Cargos y pagos no se editan ni borran; una corrección es un movimiento inverso | `Folio` (listas no modificables), `Ajuste` | CU-04, CU-05 | `PUT .../modificar`, `PUT .../cancelar` | `FolioTest.noDebePermitirModificarListaDeCargosDesdeAfueraRN16`; `FolioAjustesTest` (9 pruebas) | 2.9, 2.11 | Implementada |
| RN-17 | No se cierra un folio con saldo sin autorización registrada | `Folio.cerrar` | CU-11 | `PUT /api/reservas/{codigo}/folio/cerrar` | `FolioTest.noDebeCerrarConSaldoPendienteSinAutorizacionRN17`; `OperacionDiariaDominioTest.conSaldoPendienteElFolioSoloCierraConAutorizacionRN17`; `OperacionDiariaUseCasesTest.conSaldoPendienteExigeLaAutorizacionRN17YSiNoSeDaNoSeGuardaNada`, `.conSaldoPendienteYAutorizacionLaRegistraEnElFolioCU11` | 5.5, 5.6, 5.7 | Implementada |
| RN-18 | Reserva externa en conflicto: se rechaza y se registra; nunca sobrescribe la vigente | `RechazoReservaExterna` (constancia inmutable); la regla que viola la reserva sale de `DisponibilidadApartamentoService` y de `Reserva.crear` | CU-03 (`CrearReservaUseCase`, transacción sin rollback por la regla) | `POST /api/reservas` (409) y `GET /api/reservas-externas/rechazos` | `ReservaExternaTest` (constancia y sus datos obligatorios); `CrearReservaUseCaseTest` (`unaReservaExternaEnConflictoRN18...`, `cualquierReglaQueViola...`, el conflicto en PORTAL no se registra); `RechazoReservaExternaControllerTest`; `CanalExternoPersistenciaTest` (guardar, leer y listar de 10 en 10) | 3.28, 3.29 | Implementada |
| RN-19 | Canal + identificador externo es único; solo los canales externos lo llevan | `IdentificadorExterno`, `Reserva.crear`/`reconstruir` (invariante canal ↔ identificador), `RecepcionReservaExternaService`; respaldo en BD: restricción única `uk_reserva_canal_identificador_externo` | CU-03 (`CrearReservaUseCase`, se evalúa antes que la disponibilidad) | `POST /api/reservas` (409) | `RecepcionReservaExternaServiceTest` (5 pruebas); `ReservaExternaTest`; `CrearReservaUseCaseTest` (mensaje repetido); `CanalExternoPersistenciaTest` (búsqueda por canal e identificador y la restricción única) | 3.26, 3.27, 3.30, 3.31, 3.32 | Implementada |
| RN-20 | Tiempo de preparación entre la salida y la entrada del mismo día (3.3) | `TiempoPreparacion.permiteEntradaElMismoDia`, `DisponibilidadApartamentoService` (ambos sentidos) | CU-02, CU-03, CU-04 | igual que RN-01 | `TiempoPreparacionTest` (7 pruebas); `DisponibilidadApartamentoServiceTest.noDebePermitirEntradaMismoDiaSiExigePreparacionRN20`, `.debePermitirEntradaMismoDiaSiLaPreparacionCabeEnLaVentanaRN20`, `.noDebePermitirQueLaNuevaEstanciaTermineElDiaQueEmpiezaOtraSiNoHayPreparacionRN20` | no reproducible: con 3 h de preparación y ventana de 4 h (11:00 a 15:00) se permite | Implementada (el 409 solo se ve cambiando los parámetros de `application.properties`) |
| RN-21 | Reserva PENDIENTE que supera el plazo de confirmación se cancela automáticamente | `Reserva.haVencido`, `Reserva.vencer`, `PlazoConfirmacion`, `Folio.liquidarVencimiento` | `VencerReservasPendientesUseCase`, disparado por `VencimientoProgramado` | ninguno: es un proceso programado, sin intervención manual | `ReservaCicloDeVidaTest.unaReservaPendienteQueSuperaElPlazoDeConfirmacionSeCancelaRN21`, `.dentroDelPlazoDeConfirmacionNoVenceRN21`, `.soloVenceUnaReservaPendienteRN21`; `VencimientoReservaDominioTest.haVencidoSoloSiEstaPendienteYSuperoElPlazo`, `.unaReservaQueNoEstaPendienteNuncaHaVencido`, `.elVencimientoSinPagosDejaElFolioEnCeroSinPenalidad`, `.siHabiaUnPagoParcialElVencimientoLoDejaComoSaldoAFavor`; `VencerReservasPendientesUseCaseTest.cancelaCadaReservaVencidaRevierteSuFolioYCuentaCuantasFueron`, `.esIdempotenteUnaVezCanceladaYaNoVuelveAAparecer`, `.ignoraLasQueElPuertoDevuelvaPeroTodaviaEstanDentroDelPlazo`; `VencimientoProgramadoTest.ejecutaElCasoDeUsoConLaHoraDelServidor` | no aplica (no hay endpoint) | Implementada. Con la app en marcha, la reserva PENDIENTE sembrada (creada hoy) no se cancela al arrancar |
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
| L-06 | Dotación y características | **No modeladas** en `Apartamento` | - | - | Pendiente (tampoco hay filtro por características) |
| L-07 | Valores de las tarifas | `RegistroTarifaService`, `TarifarioRepository` | `PUT /api/apartamentos/{id}/tarifas/{temporada}` | `RegistroPoliticaYTarifaServiceTest` (3 pruebas de tarifa); HTTP 3.17 | Implementada (los valores son datos sembrados) |
| L-08 | Temporadas (mínimo 2 además de la base) | `Temporada`, `CalendarioTemporadas` | `POST/GET /api/temporadas` | `CalendarioTemporadasTest` (7 pruebas); `TarifarioUseCasesTest` (temporadas); HTTP 3.18, 3.19 | Implementada |
| L-09 | Umbral de edad facturable | `UmbralEdadFacturable`; propiedad `sga.alojamiento.umbral-edad-facturable-anios` | (afecta cotización) | `UmbralEdadFacturableTest`; `DomainServiceConfigTest.exponeLosParametrosDeLaFicha` | Implementada |
| L-10 | Tramos de la política de cancelación (mínimo 2) | `PoliticaCancelacion`, `TramoRetencion` | `POST /api/politicas-cancelacion` | `PoliticaCancelacionTest.exigeAlMenosDosTramos`, `.exigeUnTramoDeAntelacionCeroYSinRepetidos`; HTTP 3.20, 3.21 | Implementada |
| L-11 | Exigencia y monto del anticipo | `ConfirmacionReservaService`; propiedad `sga.alojamiento.anticipo-porcentaje` | `PUT .../confirmar` | `ConfirmacionReservaServiceTest` (9 pruebas) | Implementada |
| L-12 | Horas de entrada y salida | propiedades `sga.alojamiento.hora-entrada` / `hora-salida`; usadas por RN-13 y RN-20 | (afecta disponibilidad y cancelación) | `DomainServiceConfigTest.exponeLosParametrosDeLaFicha`; `RetencionCancelacionServiceTest` | Implementada |
| L-13 | Tiempo de preparación | `TiempoPreparacion`; propiedad `sga.alojamiento.tiempo-preparacion-horas` | (afecta disponibilidad) | `TiempoPreparacionTest` | Implementada |
| L-14 | Plazo de confirmación | `PlazoConfirmacion`; propiedad `sga.alojamiento.plazo-confirmacion-horas` | ninguno: lo usa el proceso programado (ver RN-21) | `ReservaCicloDeVidaTest` (vencimiento); `VencerReservasPendientesUseCaseTest.consultaLasPendientesCreadasAntesDelLimiteQueCalculaElPlazo` | Implementada |
| L-15 | Hora límite de no-show | `Reserva.declararNoShow`; propiedad `sga.alojamiento.hora-limite-no-show` | `PUT /api/reservas/{codigo}/declarar-no-show` | `ReservaCicloDeVidaTest.elNoShowSoloSeDeclaraVencidaLaHoraLimiteDelDiaDeEntrada`, `.elNoShowSoloAplicaAReservasConfirmadasRN08`; `OperacionDiariaUseCasesTest.antesDeLaHoraLimiteNoSePuedeDeclararNoShowYNoSeLiquidaNada` (HTTP 5.3) | Implementada |
| L-16 | Mascotas | - | - | - | Pendiente |
| L-17 | Servicios adicionales y su cargo | `Folio.registrarCargo` existe | **ninguno** | - | Pendiente (sin caso de uso ni endpoint) |
| L-18 | Medios de pago (mínimo 2) | `Pago.medio` es texto libre; no hay lista configurada | `POST .../folio/pagos` | `ConfirmarYPagarUseCaseTest.admiteVariosPagosConMediosDistintos` | Parcial: no se valida contra una lista de medios aceptados |
| L-19 | Reportes adicionales | - | - | - | Pendiente |
| L-20 | Tres reglas propias aprobadas por el docente | - | - | - | **Pendiente: el equipo debe definirlas y obtener la aprobación del docente** |
| L-21 | Caso de uso del modelo de IA (no bloqueante) | - | - | - | Pendiente |
| L-22 | Integración externa | `RecepcionReservaExternaService`, `RechazoReservaExterna` | `POST /api/reservas`, `GET /api/reservas-externas/rechazos` | 3.26 a 3.32 | Implementada (ver RN-18, RN-19) |
| L-23 | Funcionalidad opcional elegida | - | - | - | Pendiente |

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
| - | Consultas de reserva y folio | `ConsultarReservaUseCase`, `ListarReservasUseCase`, `ConsultarFolioUseCase` | `GET /api/reservas`, `GET /api/reservas/{codigo}`, `GET .../folio` | Implementado |
| - | Gestión de apartamentos | `CrearApartamentoUseCase`, `ConsultarApartamentoUseCase`, `ListarApartamentosUseCase`, `ActualizarApartamentoUseCase`, `ActivarApartamentoUseCase`, `DarDeBajaApartamentoUseCase` | `/api/apartamentos` (6 endpoints) | Implementado |
| - | Temporadas, tarifas y política | `RegistrarTemporadaUseCase`, `ListarTemporadasUseCase`, `RegistrarTarifaUseCase`, `ListarTarifasUseCase`, `RegistrarPoliticaCancelacionUseCase`, `ConsultarPoliticaVigenteUseCase`, `ConsultarPoliticaPorVersionUseCase` | `/api/temporadas`, `/api/apartamentos/{id}/tarifas`, `/api/politicas-cancelacion` | Implementado |

---

## 4. Verificaciones pendientes

Con la persistencia integrada ya se ejecutó lo siguiente:
- `SgaApplicationTests.contextLoads` y toda la suite (`./gradlew test`) pasan.
- La colección `docs/http/sga.http` se corrió completa contra la aplicación en marcha: **82 de 82 solicitudes** dieron el código
  esperado. Se comparó solo el código HTTP, no el contenido de cada respuesta.

Sigue pendiente:
- Comparar Swagger UI (`/swagger-ui.html`) con `docs/api/openapi.yaml` operación por operación; hasta ahora solo se
  comparó el conteo de rutas en ejecución.
- RN-12 de extremo a extremo: el paso 2.12 de la colección consulta la disponibilidad tras cancelar, pero la corrida solo
  comprobó el código 200, no que la estancia liberada aparezca en la respuesta.
- La llegada y el no-show no se pueden ejecutar con éxito por HTTP con las reservas del `.http` (son de 2028 y el servidor
  decide "ahora"); su camino de éxito está cubierto por pruebas de casos de uso, y por HTTP se prueba su 409.
- Falta "consultar las llegadas y salidas previstas para un día" (7.6): no hay endpoint propio (las llegadas se pueden
  aproximar con `GET /api/reservas?entradaDesde=...&entradaHasta=...`; las salidas no se pueden filtrar por fecha de salida).
