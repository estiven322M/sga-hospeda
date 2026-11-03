# Diagrama de clases del dominio - SGA Hospeda (actualizado en la Entrega 2)

El dominio es Java puro (sin Spring, JPA ni Lombok). Los agregados se referencian entre sí **solo por identificador**
(`Reserva` guarda `IdentificacionApartamento`; `Folio` guarda `CodigoReserva`).

**Cambios del dominio respecto a la Entrega 1** (marcados con `«nuevo»` o `«cambio»` en el diagrama):
- Nuevos: `Temporada`, `PoliticaCancelacion`, `Periodo`, `TramoRetencion`, `Ajuste`, y los servicios `CalendarioTemporadas`,
  `ActivacionApartamentoService`, `RetencionCancelacionService`, `ConfirmacionReservaService`, `RegistroBloqueoService`,
  `RegistroApartamentoService`, `RegistroPoliticaCancelacionService`, `RegistroTarifaService`, `RegistroSalidaService`
  (el folio debe estar cerrado para registrar la salida, 7.6).
- `Apartamento`: nace **inactivo** (7.4), con `activar`, `darDeBaja`, `actualizarDatos` y `reincorporarAlServicio`.
- `Folio`: nueva lista de **ajustes** (movimientos con signo, RN-16), `liquidarCancelacion` y `liquidarNoShow`.
- `RetencionCancelacionService`: además de la cancelación calcula la consecuencia del no-show con la política congelada.
- Vencimiento automático (RN-21): `Reserva.haVencido`, `PlazoConfirmacion.limiteDeCreacion` y `Folio.liquidarVencimiento`
  (reverso del alojamiento sin penalidad).
- `Reserva.modificarEstancia`: ahora rechaza reservas en curso, exige al titular entre los ocupantes y revalida RN-04.
  Sobrecarga que además recibe el apartamento destino (RN-14): `apartamento` deja de ser `final` y el cambio queda en el historial.
- `TiempoPreparacion.permiteEntradaElMismoDia(salida, entrada)`: compara con la ventana salida-entrada (RN-20).
- Puertos nuevos o ampliados en `domain/repository`: paginación (`Pagina`, `SolicitudPagina`), `FiltroReservas`,
  `GeneradorCodigoReserva`, `TemporadaRepository`, `PoliticaCancelacionRepository`.

## Entidades y objetos de valor

```mermaid
classDiagram
    direction LR

    class Reserva {
        <<raíz de agregado>>
        -CodigoReserva codigo
        -IdentificacionApartamento apartamento
        -Estancia estancia
        -Ocupante titular
        -List~Ocupante~ ocupantes
        -CanalOrigen canalOrigen
        -ValorCongelado valor
        -VersionPolitica politica
        -EstadoReserva estado
        -LocalTime horaEstimadaLlegada
        -List~EventoReserva~ historial
        +crear(...)$ Reserva
        +indicarHoraEstimadaLlegada()
        +confirmar()
        +cancelar()
        +haVencido() boolean «nuevo»
        +vencer()
        +registrarLlegada()
        +registrarSalida()
        +declararNoShow()
        +modificarEstancia(apartamento?) Dinero «cambio»
    }

    class Apartamento {
        <<raíz de agregado>>
        -IdentificacionApartamento identificacion
        -String nombre
        -int dormitorios
        -int capacidad
        -EstadoOperativo estadoOperativo
        -boolean activo
        +admite(int) boolean
        +marcarOcupado()
        +liberar()
        +iniciarPreparacion()
        +marcarPreparado()
        +declararFueraDeServicio()
        +actualizarDatos() «nuevo»
        +activar() «nuevo»
        +darDeBaja() «nuevo»
        +reincorporarAlServicio() «nuevo»
    }

    class Folio {
        <<raíz de agregado>>
        -CodigoReserva reserva
        -List~Cargo~ cargos
        -List~Ajuste~ ajustes «nuevo»
        -List~Pago~ pagos
        -boolean cerrado
        +registrarCargo()
        +registrarAjuste() «nuevo»
        +registrarPago()
        +liquidarCancelacion() «nuevo»
        +liquidarNoShow() «nuevo»
        +liquidarVencimiento() «nuevo»
        +saldo() Dinero
        +cerrar()
    }

    class Bloqueo {
        -IdentificacionApartamento apartamento
        -Estancia rango
        -String motivo
        +afecta(Estancia) boolean
    }

    class Temporada {
        <<«nuevo»>>
        -String nombre
        -boolean base
        -List~Periodo~ periodos
        +reclama(LocalDate) boolean
        +seSolapaCon(Temporada) boolean
    }

    class PoliticaCancelacion {
        <<«nuevo»>>
        -VersionPolitica version
        -LocalDate vigenteDesde
        -List~TramoRetencion~ tramos
        +retencion(Dinero, Duration) Dinero
        +devolucion(Dinero, Duration) Dinero
    }

    class Ocupante {
        -DocumentoIdentidad documento
        -LocalDate fechaNacimiento
        -String nombre
        +edadA(LocalDate) int
        +esFacturableEn(Estancia, UmbralEdadFacturable) boolean
    }

    class Estancia {
        <<record>>
        +LocalDate fechaEntrada
        +LocalDate fechaSalida
        +noches() int
        +seSolapaCon(Estancia) boolean
    }
    class ValorCongelado {
        <<record>>
        +List~CargoNoche~ detalle
        +total() Dinero
    }
    class CargoNoche {
        <<record>>
        +LocalDate noche
        +String temporada
        +Dinero tarifaPorOcupante
        +int ocupantesFacturables
        +subtotal() Dinero
    }
    class Dinero {
        <<record>>
        +BigDecimal valor
    }
    class Cargo {
        <<record>>
    }
    class Pago {
        <<record>>
    }
    class Ajuste {
        <<record «nuevo»>>
        +String concepto
        +Dinero valor
    }
    class Periodo {
        <<record «nuevo»>>
    }
    class TramoRetencion {
        <<record «nuevo»>>
        +int horasAntelacionMinima
        +int porcentajeRetencion
    }
    class Tarifa {
        <<record>>
        +String temporada
        +Dinero valorPorOcupanteNoche
    }
    class EstadoReserva {
        <<enum>>
        PENDIENTE
        CONFIRMADA
        EN_CURSO
        FINALIZADA
        CANCELADA
        NO_SHOW
    }
    class EstadoOperativo {
        <<enum>>
        PREPARADO
        OCUPADO
        PENDIENTE_PREPARACION
        EN_PREPARACION
        FUERA_DE_SERVICIO
    }
    class CanalOrigen {
        <<enum>>
        PORTAL
        DIRECTO
        EXTERNO
    }

    Reserva "1" *-- "1" Estancia
    Reserva "1" *-- "1" ValorCongelado
    Reserva "1" *-- "1..*" Ocupante
    Reserva ..> EstadoReserva
    Reserva ..> CanalOrigen
    ValorCongelado "1" *-- "1..*" CargoNoche
    CargoNoche ..> Dinero
    Apartamento ..> EstadoOperativo
    Folio "1" *-- "1..*" Cargo
    Folio "1" *-- "0..*" Ajuste
    Folio "1" *-- "0..*" Pago
    Bloqueo "1" *-- "1" Estancia
    Temporada "1" *-- "0..*" Periodo
    PoliticaCancelacion "1" *-- "2..*" TramoRetencion

    Reserva ..> Apartamento : por IdentificacionApartamento
    Folio ..> Reserva : por CodigoReserva
    Bloqueo ..> Apartamento : por IdentificacionApartamento
    Reserva ..> PoliticaCancelacion : por VersionPolitica
```

## Servicios de dominio y puertos

Los servicios se registran como `@Bean` en `infrastructure/config/DomainServiceConfig` (el dominio no conoce Spring).
Cada uno existe porque su regla involucra **más de un agregado** y ninguno puede evaluarla solo.

```mermaid
classDiagram
    direction LR

    class DisponibilidadApartamentoService {
        RN-01 RN-02 RN-07 RN-20
    }
    class CotizadorEstancia {
        RN-05 RN-06
    }
    class RetencionCancelacionService {
        <<nuevo>>
        RN-13
    }
    class ConfirmacionReservaService {
        <<nuevo>>
        L-11 RN-08 RN-09
    }
    class RegistroBloqueoService {
        <<nuevo>>
        7.3
    }
    class CalendarioTemporadas {
        <<nuevo>>
        7.4
    }
    class ActivacionApartamentoService {
        <<nuevo>>
        7.4
    }
    class RegistroApartamentoService {
        <<nuevo>>
        L-04
    }
    class RegistroPoliticaCancelacionService {
        <<nuevo>>
        F-07
    }
    class RegistroTarifaService {
        <<nuevo>>
        7.4
    }
    class RegistroSalidaService {
        <<nuevo>>
        7.6 RN-17
    }

    class ReservaRepository { <<puerto>> }
    class ApartamentoRepository { <<puerto>> }
    class FolioRepository { <<puerto>> }
    class BloqueoRepository { <<puerto>> }
    class TarifarioRepository { <<puerto>> }
    class TemporadaRepository { <<puerto «nuevo»>> }
    class PoliticaCancelacionRepository { <<puerto «nuevo»>> }
    class GeneradorCodigoReserva { <<puerto «nuevo»>> }
    class ConfiguracionAlojamiento { <<puerto>> }

    DisponibilidadApartamentoService ..> ReservaRepository
    DisponibilidadApartamentoService ..> BloqueoRepository
    DisponibilidadApartamentoService ..> ConfiguracionAlojamiento
    CotizadorEstancia ..> TarifarioRepository
    CotizadorEstancia ..> ConfiguracionAlojamiento
    RetencionCancelacionService ..> ConfiguracionAlojamiento
    ConfirmacionReservaService ..> ConfiguracionAlojamiento
    RegistroBloqueoService ..> ReservaRepository
    ActivacionApartamentoService ..> TarifarioRepository
    ActivacionApartamentoService ..> TemporadaRepository
    RegistroApartamentoService ..> ApartamentoRepository
    RegistroPoliticaCancelacionService ..> PoliticaCancelacionRepository
    RegistroTarifaService ..> TemporadaRepository
```
