# API de Gestión de Pólizas

API REST para gestionar pólizas de seguro y sus riesgos:

- consultar pólizas con filtros y ver sus riesgos;
- renovar pólizas aplicando el IPC;
- cancelar pólizas (con todos sus riesgos) o riesgos individuales;
- agregar riesgos a pólizas colectivas.

Todas las rutas están protegidas con una API key. Cada cambio en los datos se notifica a un CORE simulado.

## Stack

| Componente | Versión |
|---|---|
| Java | 17 |
| Spring Boot (Web, Data JPA, Validation) | 3.5.16 |
| Hibernate ORM | 6.6 |
| Base de datos H2 (en memoria) | 2.3.232 |
| Maven (incluye Maven Wrapper) | 3.9.16 |
| Pruebas | JUnit 5, MockMvc, AssertJ, Mockito |

## Requisitos

- **JDK 17** o superior, con `JAVA_HOME` configurado (o `java` en el `PATH`).
- No hace falta instalar Maven: el proyecto incluye el Maven Wrapper (`mvnw` / `mvnw.cmd`), que lo descarga la primera vez.
- Conexión a Internet en la primera compilación, para descargar las dependencias.

## Instalación y ejecución

```bash
git clone https://github.com/Cristianccb2/DaviviendaPrueba.git
cd DaviviendaPrueba
```

Arrancar la aplicación:

| Sistema | Comando |
|---|---|
| Linux / macOS / Git Bash | `./mvnw spring-boot:run` |
| Windows (cmd o PowerShell) | `mvnw.cmd spring-boot:run` |
| Windows (doble clic) | `iniciar-app.cmd`: compila si hace falta y abre la app en su propia ventana |

O generar y ejecutar el JAR:

```bash
./mvnw clean package
java -jar target/polizas-api-1.0.0.jar
```

- **URL base:** `http://localhost:8085`
- **Otro puerto:** `java -jar target/polizas-api-1.0.0.jar --server.port=9090`, o `iniciar-app.cmd 9090`.
- **Detener:** `Ctrl + C`, cerrar la ventana de `iniciar-app.cmd`, o ejecutar `detener-app.cmd`.

La aplicación está lista cuando el log muestra `Started PolizasApplication`.

## Seguridad: API key

Todas las peticiones deben incluir el header:

```
x-api-key: 123456
```

Si falta o es incorrecto, la respuesta es:

```json
HTTP 401
{ "codigo": "API_KEY_INVALIDA", "mensaje": "API key faltante o incorrecta" }
```

La clave se configura con la propiedad `seguridad.api-key`. El valor `123456` es solo para uso local.

## Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/polizas?tipo=&estado=` | Lista pólizas. Filtros opcionales `tipo` (`INDIVIDUAL`, `COLECTIVA`) y `estado` (`ACTIVA`, `RENOVADA`, `CANCELADA`), combinables | 200 |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza, incluidos los cancelados | 200 |
| POST | `/polizas/{id}/renovar` | Renueva la póliza aplicando el IPC indicado | 200 |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos | 200 |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo a una póliza colectiva | 201 |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo | 200 |
| POST | `/core-mock/evento` | CORE simulado: registra el evento en el log | 200 |

### Cuerpos de las peticiones

```jsonc
// POST /polizas/{id}/renovar  -> ipcPorcentaje entre 0 y 100 (5.20 = 5,20 %)
{ "ipcPorcentaje": 5.20 }

// POST /polizas/{id}/riesgos  -> descripcion obligatoria, máximo 255 caracteres
{ "descripcion": "Apartamento 301" }

// POST /core-mock/evento
{ "evento": "ACTUALIZACION", "polizaId": 555 }
```

### Respuestas

```jsonc
// Póliza
{ "id": 1, "tipo": "INDIVIDUAL", "estado": "RENOVADA", "canonMensual": 1050.00, "prima": 12600.00 }

// Riesgo
{ "id": 6, "polizaId": 2, "descripcion": "Apartamento 301", "estado": "ACTIVO" }

// Error
{ "codigo": "POLIZA_CANCELADA", "mensaje": "No se puede renovar una póliza cancelada" }
```

## Reglas de negocio

- **Renovación:**
  - `canonMensual` y `prima` se multiplican por `1 + ipcPorcentaje / 100`, se redondean a 2 decimales (`HALF_UP`) y la póliza pasa a `RENOVADA`.
  - Ejemplo: canon `1000.00` y prima `12000.00` con IPC `5` quedan en `1050.00` y `12600.00`.
  - Una póliza `RENOVADA` puede renovarse de nuevo; el IPC se aplica sobre los importes actuales.
  - Una póliza `CANCELADA` no se puede renovar.
- **Cancelar póliza:** la póliza pasa a `CANCELADA` y **todos** sus riesgos a `CANCELADO`, en una sola transacción.
- **Cancelar riesgo:** solo cambia ese riesgo. La póliza y sus demás riesgos no se modifican.
- **Agregar riesgo:** solo en pólizas `COLECTIVA` que no estén canceladas. El riesgo se crea `ACTIVO`.
- **Idempotencia:** repetir una cancelación devuelve 200 sin cambios.
- **Sin borrado:** los riesgos cancelados se conservan como historial.

### Integración con el CORE

Cada operación que modifica datos envía un `POST` HTTP a `/core-mock/evento` con `{"evento":"ACTUALIZACION","polizaId":<id>}`.

- **Cuándo se envía:** después de guardar los cambios en la base de datos, y solo si hubo un cambio real. Una cancelación repetida o un IPC 0 sobre una póliza ya renovada no generan evento.
- **Si el CORE no responde:** la operación queda guardada y se registra un `WARN` en el log.

Ejemplo de log:

```
INFO  CoreClient          : CORE_CLIENT intento evento=ACTUALIZACION polizaId=1
INFO  CoreMockController  : CORE_MOCK intento evento=ACTUALIZACION polizaId=1
INFO  CoreClient          : CORE_CLIENT resultado=OK evento=ACTUALIZACION polizaId=1
```

### Códigos de error

| HTTP | `codigo` | Cuándo |
|---|---|---|
| 400 | `SOLICITUD_INVALIDA` | Filtro con valor inexistente, id no numérico o ≤ 0, JSON inválido, IPC ausente o fuera de 0–100, descripción vacía o de más de 255 caracteres, evento inválido |
| 401 | `API_KEY_INVALIDA` | Header `x-api-key` ausente o incorrecto |
| 404 | `POLIZA_NO_ENCONTRADA` / `RIESGO_NO_ENCONTRADO` | El recurso no existe |
| 409 | `POLIZA_CANCELADA` | Renovar una póliza cancelada o agregar un riesgo a una colectiva cancelada |
| 409 | `TIPO_POLIZA_NO_PERMITE_RIESGOS` | Agregar un riesgo a una póliza individual |
| 500 | `ERROR_INTERNO` | Error inesperado |

## Configuración

Archivo `src/main/resources/application.properties`. Cualquier valor se puede sobrescribir con `--propiedad=valor` al arrancar.

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `server.port` | `8085` | Puerto HTTP |
| `spring.datasource.url` | `jdbc:h2:mem:polizasdb` | Base de datos H2 en memoria (usuario `sa`, sin contraseña) |
| `seguridad.api-key` | `123456` | Valor esperado en el header `x-api-key` |
| `core.mock.base-url` | `http://localhost:${server.port}` | Destino de los eventos del CORE |

La base de datos vive en memoria: se crea y se carga con los datos de ejemplo en cada arranque, y los cambios se pierden al detener la aplicación.

## Datos de ejemplo

| Póliza | Tipo | Estado | canonMensual | prima | Riesgos |
|---|---|---|---|---|---|
| 1 | INDIVIDUAL | ACTIVA | 1000.00 | 12000.00 | 1 · Casa Calle 10 # 20-30 (ACTIVO) |
| 2 | COLECTIVA | ACTIVA | 2500.00 | 30000.00 | 2 · Apartamento 101, 3 · Apartamento 102 (ACTIVO) |
| 3 | COLECTIVA | CANCELADA | 1800.00 | 21600.00 | 4 · Local comercial 5 (CANCELADO) |
| 4 | INDIVIDUAL | RENOVADA | 1200.00 | 14400.00 | 5 · Oficina 802 (ACTIVO) |

## Cómo probarla

### 1. Pruebas automatizadas

```bash
./mvnw test          # Windows: mvnw.cmd test
```

Ejecuta las pruebas de integración con H2 y MockMvc:

| Clase | Qué verifica |
|---|---|
| `ConsultasApiTest` | Listado con filtros, riesgos por póliza, 404 y validación de parámetros |
| `RenovacionApiTest` | Cálculo del IPC y redondeo, límites 0 y 100, póliza cancelada (409), entradas inválidas (400) |
| `CancelacionesApiTest` | Cancelación en cascada, idempotencia y cancelación de un riesgo sin afectar la póliza |
| `RiesgosApiTest` | Alta en colectivas (201), rechazo en individuales o canceladas (409), validaciones (400) |
| `SeguridadApiTest` | 401 sin API key o con API key incorrecta en todos los endpoints |
| `CoreMockApiTest` | Registro del evento en el log y validación del cuerpo |
| `CoreClientTest` | Envío al CORE después del commit y tolerancia a fallos |
| `IntegracionCoreTest` | Prueba completa con servidor real: una operación notifica al CORE por HTTP |
| `PolizaRepositoryTest` | Datos de ejemplo y consulta con filtros |

### 2. Thunder Client (VS Code)

La carpeta [`thunder-client/`](thunder-client) contiene:

- `thunder-collection_polizas-api.json`: colección importable con 46 peticiones que cubren todos los endpoints. Cada una trae el header y el cuerpo ya configurados, y sus tests (código HTTP, campos e importes).
- [`PRUEBAS-THUNDER-CLIENT.md`](thunder-client/PRUEBAS-THUNDER-CLIENT.md): cada caso con método, URL, cuerpo JSON y resultado esperado.

Pasos:

1. Arranca la aplicación en `http://localhost:8085`.
2. En Thunder Client: **Collections → ☰ → Import** → `thunder-collection_polizas-api.json`.
3. Ejecuta las peticiones **en orden** (1.01 → 6.05), una a una o con **Run All**.
4. Para repetirlas, **reinicia la aplicación**. Algunas peticiones modifican datos y los resultados esperados dependen de las anteriores.

### 3. curl

```bash
curl -H 'x-api-key: 123456' 'http://localhost:8085/polizas?tipo=COLECTIVA&estado=ACTIVA'
curl -H 'x-api-key: 123456' http://localhost:8085/polizas/1/riesgos
curl -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' -d '{"ipcPorcentaje":5}' http://localhost:8085/polizas/1/renovar
curl -X POST -H 'x-api-key: 123456' http://localhost:8085/polizas/1/cancelar
curl -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' -d '{"descripcion":"Apartamento 301"}' http://localhost:8085/polizas/2/riesgos
curl -X POST -H 'x-api-key: 123456' http://localhost:8085/riesgos/2/cancelar
curl -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' -d '{"evento":"ACTUALIZACION","polizaId":555}' http://localhost:8085/core-mock/evento
```

> En PowerShell usa `curl.exe` y escapa las comillas del JSON (`-d '{\"ipcPorcentaje\":5}'`), o ejecuta los ejemplos desde Git Bash.

## Estructura del proyecto

```
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/          Maven Wrapper
├── iniciar-app.cmd                Arranque en Windows (puerto 8085 por defecto)
├── detener-app.cmd                Detiene la aplicación
├── thunder-client/                Colección y casos de prueba para Thunder Client
└── src/
    ├── main/java/com/prueba/polizas/
    │   ├── config/       ApiKeyFilter: validación del header x-api-key
    │   ├── controller/   Endpoints REST (pólizas, riesgos, CORE mock)
    │   ├── service/      Reglas de negocio, transacciones y cliente del CORE
    │   ├── repository/   Acceso a datos (Spring Data JPA)
    │   ├── model/        Entidades Poliza y Riesgo, y sus enums
    │   ├── dto/          Objetos de entrada y salida de la API
    │   └── exception/    Excepciones y manejo centralizado de errores
    ├── main/resources/   application.properties y data.sql (datos de ejemplo)
    └── test/java/        Pruebas automatizadas
```

## Consideraciones

- La API key fija solo sirve para uso local. En producción haría falta un mecanismo de autenticación real.
- La base de datos es en memoria: los datos se reinician en cada arranque.
- El CORE es simulado. La notificación es síncrona y sin reintentos: si el CORE falla, el evento solo queda registrado en el log.
