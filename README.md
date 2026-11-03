# SGA — Sistema de gestión de alojamiento

> **Proyecto:** Sistema de Gestión de Alojamiento (SGA)
> **Alojamiento:** Hospeda
> **Curso:** Programación Avanzada
> **Institución:** Universidad del Quindío

---

## 1. Descripción del alojamiento
Sistema de gestión integral para el alojamiento turístico **Hospeda**, ubicado en Salento, Quindío. La plataforma permite administrar habitaciones, huéspedes, disponibilidad, reservas, pagos y servicios asociados al hospedaje, bajo una arquitectura hexagonal que favorece la separación de responsabilidades y la mantenibilidad del sistema.


---

## 2. Integrantes del equipo
* **Karen Dahiana Martínez** — karen.martinezh@uqvirtual.edu.co
* **Miguel Villegas** — miguela.villegasv@uqvirtual.edu.co
* **Jhon Stivenson Méndez** — jhons.mendeza@uqvirtual.edu.co

---

## 3. Requisitos para ejecutar
Para compilar y ejecutar este backend se requiere:
* **JDK 25** (OpenJDK / Temurin / Corretto 25).
* **Git** (para el control de versiones).
* **Gradle Wrapper** (incluido en el proyecto; no requiere instalación manual de Gradle).

---

## 4. Instrucciones de ejecución

### Desde la línea de comandos (Terminal)
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/estiven322M/sga-hospeda.git
   cd sga-hospeda

---

## 5. Cómo probar la API

- **Arrancar:** `.\gradlew.bat bootRun` (Windows) o `./gradlew bootRun`. Puerto 8080, JDK 25.
- **Pruebas automáticas:** `.\gradlew.bat test`.
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Contrato de la API:** `docs/api/openapi.yaml` (coincide con los endpoints implementados).
- **Consola H2:** http://localhost:8080/h2-console, URL `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña.
- **Datos de prueba:** se cargan solos al iniciar (apartamentos de ejemplo, temporadas BASE, MEDIA y ALTA, una política de cancelación, reservas en todos los estados y una por canal). Se desactivan con `sga.datos-prueba.habilitado=false` en `application.properties`. Los valores de la Ficha del Alojamiento están en `CatalogoDatosPrueba` y se ajustan en esa única clase.
- **Colección de pruebas:** `docs/http/sga.http` (cliente HTTP de IntelliJ, entorno `local`). Se ejecuta **una sola vez, de arriba hacia abajo, con la aplicación recién arrancada**; si se repite en la misma sesión, las reservas de 2028 ya existen y varias solicitudes darán 409.

---

## 6. Documentación

- `docs/matriz-trazabilidad.md`: regla de negocio, su implementación, su prueba y el endpoint que la expone.
- `docs/diagrama-clases-dominio.md`: diagrama de clases del dominio.
- `docs/contrato-persistencia.md`: contrato entre la capa de aplicación y la persistencia.

---

## 7. Alcance de esta entrega

El backend se entrega **sin seguridad JWT**, por indicación del docente: los endpoints están abiertos y no hay respuestas 401 ni 403.