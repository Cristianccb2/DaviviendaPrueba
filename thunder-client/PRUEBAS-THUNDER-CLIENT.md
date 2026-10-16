# Pruebas de la API en Thunder Client

Casos de prueba de los 7 endpoints. Para cada uno se indica **método**, **URL**, **JSON del cuerpo** y **resultado esperado**.

## Preparación

1. Arranca la aplicación con datos limpios con **doble clic en [`iniciar-app.cmd`](../iniciar-app.cmd)**.
   Se abre una ventana titulada *"Polizas API - puerto 8085"*, que debe quedar abierta mientras pruebas.
   Los datos semilla se recargan en cada arranque. Para detenerla, cierra esa ventana o ejecuta [`detener-app.cmd`](../detener-app.cmd).
2. En VS Code abre **Thunder Client → Collections → ☰ → Import** y selecciona
   [`thunder-collection_polizas-api.json`](thunder-collection_polizas-api.json).
3. Ejecuta las peticiones **en orden** (1.01 → 6.05), una a una o con **Run All** sobre la colección.
   Cada petición trae sus tests: código HTTP, campos del JSON e importes.
4. Para repetir la batería, **reinicia la aplicación** (cierra la ventana y vuelve a abrir `iniciar-app.cmd`). Algunas pruebas modifican datos y los resultados esperados dependen de los pasos anteriores.

> Si Thunder Client muestra **"Connection was refused by the server"**, la aplicación no está corriendo: abre `iniciar-app.cmd` y espera a ver `Started PolizasApplication` en su ventana.

Datos comunes:
- URL base: `http://localhost:8085`. Si arrancas la aplicación en otro puerto, reemplaza `8085` en las URLs.
- Header en todas las peticiones salvo las de seguridad: `x-api-key: 123456`.
- Peticiones con cuerpo: tipo **JSON** (`Content-Type: application/json`).
- Logs del CORE: los ves en la consola de la aplicación y en `logs/polizas-api.log`.

Datos semilla al arrancar:

| Póliza | Tipo | Estado | canonMensual | prima | Riesgos |
|---|---|---|---|---|---|
| 1 | INDIVIDUAL | ACTIVA | 1000.00 | 12000.00 | 1 ACTIVO |
| 2 | COLECTIVA | ACTIVA | 2500.00 | 30000.00 | 2 y 3 ACTIVO |
| 3 | COLECTIVA | CANCELADA | 1800.00 | 21600.00 | 4 CANCELADO |
| 4 | INDIVIDUAL | RENOVADA | 1200.00 | 14400.00 | 5 ACTIVO |

## 1. Consultas

| # | Método | URL | JSON (body) | Resultado esperado |
|---|---|---|---|---|
| 1.01 | GET | `http://localhost:8085/polizas` | — | 200 · Arreglo con las 4 pólizas semilla (ids 1, 2, 3, 4). |
| 1.02 | GET | `http://localhost:8085/polizas?tipo=COLECTIVA&estado=ACTIVA` | — | 200 · Solo la póliza 2: `{"id":2,"tipo":"COLECTIVA","estado":"ACTIVA","canonMensual":2500.00,"prima":30000.00}`. |
| 1.03 | GET | `http://localhost:8085/polizas?tipo=INDIVIDUAL` | — | 200 · Pólizas 1 y 4 (ambas INDIVIDUAL). |
| 1.04 | GET | `http://localhost:8085/polizas?estado=CANCELADA` | — | 200 · Solo la póliza 3. |
| 1.05 | GET | `http://localhost:8085/polizas?estado=VENCIDA` | — | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"Valor inválido para el parámetro 'estado': VENCIDA"}`. |
| 1.06 | GET | `http://localhost:8085/polizas/2/riesgos` | — | 200 · Riesgos 2 (Apartamento 101) y 3 (Apartamento 102), ambos ACTIVO y con polizaId 2. |
| 1.07 | GET | `http://localhost:8085/polizas/3/riesgos` | — | 200 · Riesgo 4 (Local comercial 5) con estado CANCELADO. |
| 1.08 | GET | `http://localhost:8085/polizas/999/riesgos` | — | 404 · `{"codigo":"POLIZA_NO_ENCONTRADA","mensaje":"No existe la póliza con id 999"}`. |
| 1.09 | GET | `http://localhost:8085/polizas/0/riesgos` | — | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"El id debe ser un entero positivo"}`. |

## 2. Renovación

| # | Método | URL | JSON (body) | Resultado esperado |
|---|---|---|---|---|
| 2.01 | POST | `http://localhost:8085/polizas/1/renovar` | `{ "ipcPorcentaje": 5 }` | 200 · `{"id":1,"tipo":"INDIVIDUAL","estado":"RENOVADA","canonMensual":1050.00,"prima":12600.00}`. Log: `CORE_CLIENT intento ... polizaId=1`, `CORE_MOCK intento ... polizaId=1`, `CORE_CLIENT resultado=OK`. |
| 2.02 | POST | `http://localhost:8085/polizas/2/renovar` | `{ "ipcPorcentaje": 5.20 }` | 200 · Póliza 2 RENOVADA: canonMensual 2500.00 × 1.052 = **2630.00**, prima 30000.00 × 1.052 = **31560.00**. Log CORE con polizaId=2. |
| 2.03 | POST | `http://localhost:8085/polizas/4/renovar` | `{ "ipcPorcentaje": 0 }` | 200 · Póliza 4 sin cambios (RENOVADA, 1200.00 / 14400.00). **No** aparece log CORE (no hubo cambio efectivo). |
| 2.04 | POST | `http://localhost:8085/polizas/3/renovar` | `{ "ipcPorcentaje": 5 }` | 409 · `{"codigo":"POLIZA_CANCELADA","mensaje":"No se puede renovar una póliza cancelada"}`. Sin log CORE. |
| 2.05 | POST | `http://localhost:8085/polizas/999/renovar` | `{ "ipcPorcentaje": 5 }` | 404 · `{"codigo":"POLIZA_NO_ENCONTRADA",...}`. |
| 2.06 | POST | `http://localhost:8085/polizas/1/renovar` | `{}` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"ipcPorcentaje es obligatorio"}`. |
| 2.07 | POST | `http://localhost:8085/polizas/1/renovar` | `{ "ipcPorcentaje": -1 }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"ipcPorcentaje debe estar entre 0 y 100"}`. |
| 2.08 | POST | `http://localhost:8085/polizas/1/renovar` | `{ "ipcPorcentaje": 150 }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"ipcPorcentaje debe estar entre 0 y 100"}`. |
| 2.09 | POST | `http://localhost:8085/polizas/1/renovar` | `{ "ipcPorcentaje": "abc" }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"El cuerpo de la solicitud está ausente o tiene un formato inválido"}`. |

## 3. Alta de riesgos

| # | Método | URL | JSON (body) | Resultado esperado |
|---|---|---|---|---|
| 3.01 | POST | `http://localhost:8085/polizas/2/riesgos` | `{ "descripcion": "Apartamento 301" }` | 201 · `{"id":6,"polizaId":2,"descripcion":"Apartamento 301","estado":"ACTIVO"}`. Log CORE con polizaId=2. |
| 3.02 | POST | `http://localhost:8085/polizas/1/riesgos` | `{ "descripcion": "Garaje" }` | 409 · `{"codigo":"TIPO_POLIZA_NO_PERMITE_RIESGOS","mensaje":"Solo las pólizas colectivas permiten agregar riesgos"}`. Sin log CORE. |
| 3.03 | POST | `http://localhost:8085/polizas/3/riesgos` | `{ "descripcion": "Bodega 7" }` | 409 · `{"codigo":"POLIZA_CANCELADA","mensaje":"No se pueden agregar riesgos a una póliza cancelada"}`. |
| 3.04 | POST | `http://localhost:8085/polizas/999/riesgos` | `{ "descripcion": "Apartamento 301" }` | 404 · `{"codigo":"POLIZA_NO_ENCONTRADA",...}`. |
| 3.05 | POST | `http://localhost:8085/polizas/2/riesgos` | `{}` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"descripcion es obligatoria y no puede estar vacía"}`. |
| 3.06 | POST | `http://localhost:8085/polizas/2/riesgos` | `{ "descripcion": "" }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"descripcion es obligatoria y no puede estar vacía"}`. |
| 3.07 | POST | `http://localhost:8085/polizas/2/riesgos` | `{ "descripcion": "AAAA…A" }  (256 letras A)` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"descripcion admite máximo 255 caracteres"}`. |
| 3.08 | GET | `http://localhost:8085/polizas/2/riesgos` | — | 200 · 3 riesgos: 2, 3 y el nuevo 6 (Apartamento 301), todos ACTIVO. |

## 4. Cancelaciones

| # | Método | URL | JSON (body) | Resultado esperado |
|---|---|---|---|---|
| 4.01 | POST | `http://localhost:8085/riesgos/2/cancelar` | — | 200 · `{"id":2,"polizaId":2,"descripcion":"Apartamento 101","estado":"CANCELADO"}`. Log CORE con polizaId=2. |
| 4.02 | GET | `http://localhost:8085/polizas/2/riesgos` | — | 200 · Riesgo 2 CANCELADO; riesgos 3 y 6 siguen ACTIVO. |
| 4.03 | GET | `http://localhost:8085/polizas?tipo=COLECTIVA&estado=RENOVADA` | — | 200 · La póliza 2 sigue RENOVADA (cancelar un riesgo no cancela la póliza). |
| 4.04 | POST | `http://localhost:8085/riesgos/2/cancelar` | — | 200 · Mismo riesgo CANCELADO, sin cambios. **No** aparece log CORE. |
| 4.05 | POST | `http://localhost:8085/riesgos/999/cancelar` | — | 404 · `{"codigo":"RIESGO_NO_ENCONTRADO","mensaje":"No existe el riesgo con id 999"}`. |
| 4.06 | POST | `http://localhost:8085/riesgos/5/cancelar` | — | 200 · `{"id":5,"polizaId":4,"descripcion":"Oficina 802","estado":"CANCELADO"}`. Log CORE con polizaId=4. |
| 4.07 | GET | `http://localhost:8085/polizas?tipo=INDIVIDUAL&estado=RENOVADA` | — | 200 · Pólizas 1 y 4, ambas RENOVADA (la 4 queda vigente sin riesgos activos). |
| 4.08 | POST | `http://localhost:8085/polizas/2/cancelar` | — | 200 · `{"id":2,"tipo":"COLECTIVA","estado":"CANCELADA","canonMensual":2630.00,"prima":31560.00}`. Un solo log CORE con polizaId=2. |
| 4.09 | GET | `http://localhost:8085/polizas/2/riesgos` | — | 200 · Riesgos 2, 3 y 6, todos CANCELADO. |
| 4.10 | POST | `http://localhost:8085/polizas/2/cancelar` | — | 200 · Póliza 2 CANCELADA, sin cambios. **No** aparece log CORE. |
| 4.11 | POST | `http://localhost:8085/polizas/999/cancelar` | — | 404 · `{"codigo":"POLIZA_NO_ENCONTRADA",...}`. |

## 5. CORE mock

| # | Método | URL | JSON (body) | Resultado esperado |
|---|---|---|---|---|
| 5.01 | POST | `http://localhost:8085/core-mock/evento` | `{ "evento": "ACTUALIZACION", "polizaId": 555 }` | 200 · `{"mensaje":"Evento registrado"}`. Log: `CORE_MOCK intento evento=ACTUALIZACION polizaId=555` (no valida que la póliza exista). |
| 5.02 | POST | `http://localhost:8085/core-mock/evento` | `{ "evento": "BORRADO", "polizaId": 555 }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"evento debe ser ACTUALIZACION"}`. |
| 5.03 | POST | `http://localhost:8085/core-mock/evento` | `{ "evento": "ACTUALIZACION", "polizaId": 0 }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"polizaId debe ser un entero positivo"}`. |
| 5.04 | POST | `http://localhost:8085/core-mock/evento` | `{ "evento": "ACTUALIZACION" }` | 400 · `{"codigo":"SOLICITUD_INVALIDA","mensaje":"polizaId es obligatorio"}`. |

## 6. Seguridad (x-api-key)

| # | Método | URL | JSON (body) | Resultado esperado |
|---|---|---|---|---|
| 6.01 | GET | `http://localhost:8085/polizas`<br>**sin header `x-api-key`** | — | 401 · `{"codigo":"API_KEY_INVALIDA","mensaje":"API key faltante o incorrecta"}`. |
| 6.02 | GET | `http://localhost:8085/polizas`<br>**`x-api-key: 000000`** | — | 401 · `{"codigo":"API_KEY_INVALIDA","mensaje":"API key faltante o incorrecta"}`. |
| 6.03 | POST | `http://localhost:8085/polizas/1/cancelar`<br>**sin header `x-api-key`** | — | 401 · `API_KEY_INVALIDA`. El controlador no se ejecuta. |
| 6.04 | GET | `http://localhost:8085/polizas?tipo=INDIVIDUAL&estado=RENOVADA` | — | 200 · La póliza 1 sigue RENOVADA (la petición sin key no hizo cambios). |
| 6.05 | POST | `http://localhost:8085/core-mock/evento`<br>**sin header `x-api-key`** | `{ "evento": "ACTUALIZACION", "polizaId": 555 }` | 401 · `API_KEY_INVALIDA` (el mock también está protegido). |
