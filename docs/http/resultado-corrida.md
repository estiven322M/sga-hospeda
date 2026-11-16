# Resultado de la corrida de `sga.http`

Evidencia de la ejecución completa de la colección [`sga.http`](sga.http) contra la aplicación en marcha, con aserciones
sobre el **cuerpo** de cada respuesta (`client.test`), no solo sobre el código HTTP.

| Dato | Valor |
|---|---|
| Fecha | 2026-11-16 a las 03:11:50 (hora de Bogotá) |
| Código base | `57916d7` (`main`) más los cambios del paso J6 (rama `feature/alojamiento-servicios`) |
| Aplicación | `./gradlew bootRun` recién arrancada, con los datos de prueba sembrados (H2 en memoria) |
| Cliente | [`ejecutar-coleccion.js`](ejecutar-coleccion.js): ejecuta cada solicitud en orden y corre sus bloques `client.test` con la misma API del cliente HTTP de IntelliJ (`client.assert`, `client.global`, `response.body`, `response.headers.valueOf`) |
| **Solicitudes ejecutadas** | **220** |
| **`client.test` ejecutados** | **220** |
| **`client.test` que pasaron** | **220** |
| **`client.test` que fallaron** | **0** |

Cada solicitud tiene al menos un `client.test` (220 de 220). Toda respuesta 409 verifica `status` = 409, que
`mensaje` incluya el texto de la regla violada, que `errores` sea nulo y que no haya trazas técnicas.

## Códigos HTTP observados

| Código | Solicitudes |
|---|---|
| 200 | 84 |
| 201 | 17 |
| 400 | 21 |
| 401 | 11 |
| 403 | 21 |
| 404 | 10 |
| 409 | 56 |

## Solicitudes por parte de la colección

| Parte | Contenido | Solicitudes |
|---|---|---|
| 0 | Inicio de sesión (público) | 7 |
| 1 | Consultas de referencia y filtros por características (L-06) | 15 |
| 2 | Flujo completo de una reserva | 13 |
| 3 | Escenarios 409 (una regla por escenario) | 37 |
| 4 | Errores 400 y 404 | 9 |
| 5 | Operación diaria, folio y estado operativo | 19 |
| 6 | Seguridad: 401, 403, propietario y rol/canal | 28 |
| 7 | Reglas propias (L-20) | 19 |
| 8 | Soporte del frontend: CORS, mis reservas, filtros | 18 |
| 9 | Alojamiento, servicios adicionales, medios de pago y calendarios (J6) | 52 |
| - | Pasos cuyo título está en un comentario (se numeran por su comentario) | 3 |

## Pasos manuales y límites

- **6.14 (token vencido -> 401)** es MANUAL. El token dura 30 minutos, así que en una corrida normal la solicitud responde 200 y su
  `client.test` lo acepta (200 o 401). Para verlo en 401: arrancar con `gradlew.bat bootRun --args="--sga.jwt.expiracion-minutos=1"`,
  ejecutar el login (0.2), esperar 2 minutos y ejecutar el paso. Lo verifica automáticamente `JwtConfigTest.unTokenVencidoSeRechaza`.
- **Lo que depende del tiempo no se puede recorrer con esta colección** porque las reservas son de 2028 y el servidor decide "ahora":
  la llegada y el no-show con éxito (RN-10, RN-11, L-15), el vencimiento automático (RN-21) y el tiempo de preparación entre reservas
  (RN-20, que depende de los parámetros). Su camino completo, con el reloj movido por la prueba, está en los recorridos
  `RecorridoOperacionDiariaIntegracionTest` y `RecorridoVencimientoIntegracionTest` (ver la matriz de trazabilidad).
- **La colección no es idempotente:** se ejecuta sobre una aplicación recién arrancada. En una segunda pasada sin reiniciar, el registro
  de Ana (0.6) y la reserva externa (3.26) responden 409 y los pasos que dependen de la primera pasada cambian de resultado.
- Las aserciones de los pasos 1.x a 6.x que no son de L-06 ni de L-20 se generaron a partir de las respuestas observadas en una corrida
  verificada: comprueban la forma del cuerpo, los valores estables (estado, canal, estado operativo, moneda, noches...) y, en los
  errores, el mensaje de la regla. Los importes y las cifras de negocio se verifican con exactitud en las PARTES 1 (L-06), 7 (L-20) y 8
  y en los recorridos de Java.

## Cómo reproducirla

```
./gradlew bootRun                     # en una terminal (puerto 8080)
node docs/http/ejecutar-coleccion.js docs/http/sga.http http://localhost:8080
```

El cliente imprime cada `client.test` (`ok` o `FALLA` con el cuerpo recibido) y, al final, los totales. En IntelliJ se ejecuta con el
entorno `local` de `http-client.env.json`.
