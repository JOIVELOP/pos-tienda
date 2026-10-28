# POS Tienda

## Caja, compras y devoluciones

Los módulos operativos agregados están disponibles para usuarios autenticados:

- **Caja** (`/caja`): cada empleado abre su propio turno con un fondo, registra aportaciones, retiros y gastos, y lo cierra con efectivo contado. Las salidas no pueden superar el efectivo disponible. Las ventas en efectivo, salidas manuales y reembolsos en efectivo se concilian en el esperado; administradores y encargados pueden consultar las diferencias de todos los turnos.
- **Compras** (`/compras`): administrador/encargado crea órdenes a proveedores y registra recepciones parciales. Se actualiza el stock solo por mercancía buena; cantidades dañadas, observaciones y costo real quedan guardados por recepción. El costo de compra actual del producto refleja el costo unitario de la última recepción.
- **Devoluciones** (`/devoluciones`): administrador/encargado puede devolver parcialmente artículos de una venta completada. El backend calcula el importe, controla cantidades previamente devueltas y repone stock solo si el artículo se marca para reingreso. Reembolsos en efectivo se registran como salida del turno actual; los de tarjeta/transferencia deben ejecutarse también en la terminal o banco.

Todos los importes y usuarios se validan en backend; caja y transacciones de inventario se ejecutan en transacciones con bloqueos de filas. Las rutas de compras/devoluciones están restringidas a administrador y encargado.

### Actualización de base de datos

Antes de desplegar esta versión:

1. Realiza y verifica un respaldo de MySQL.
2. Ejecuta una sola vez, contra la base de datos existente, el script [V20261027_01__caja_compras_devoluciones.sql](./pos.tienda/src/main/resources/db/manual/V20261027_01__caja_compras_devoluciones.sql).
3. Despliega backend y frontend juntos y prueba apertura/cierre, recepción parcial y devolución en una base de pruebas.

El proyecto todavía no incorpora un ejecutor automático de migraciones; no despliegues código que use estas entidades sin aplicar primero el SQL.

## Continuidad sin internet: caja preparada

La continuidad es **opt-in por turno y navegador**. La caja preparada guarda primero las
operaciones en IndexedDB, reserva stock/efectivo local y después intenta sincronizar.
La misma ruta se usa con y sin conexión: una respuesta perdida no genera una clave
nueva ni una segunda venta. El servidor serializa operaciones del empleado y guarda
la confirmación en la misma transacción que los cambios de caja/inventario.

### Preparación y uso

1. Respaldar la base y aplicar, después del script anterior,
   [V20261027_02__operacion_offline.sql](./pos.tienda/src/main/resources/db/manual/V20261027_02__operacion_offline.sql).
2. Desplegar el frontend compilado (`npm run build`) mediante HTTPS o localhost.
   `VITE_API_URL` permite configurar la URL del backend al compilar. El servidor del
   frontend debe resolver rutas como `/ventas` al documento principal. Configurar
   CORS en backend para el origen real; el valor actual está limitado al entorno local.
3. Iniciar sesión online, abrir caja y pulsar **Preparar turno online**. Esperar que
   aparezcan el folio del diario y su vigencia. En producción se espera también la
   instalación del service worker; la aplicación debe haberse cargado online al menos
   una vez. No usar navegación privada ni borrar datos del sitio.
4. Continuar en el mismo navegador y equipo. El diario permite ventas **en efectivo,
   a público en general y a precios base congelados**, aportaciones, gastos y retiros.
   No aplica promociones ni descuentos del flujo online normal. La autorización para
   nuevas capturas dura 24 horas desde la preparación.
5. Al recuperar conexión se reintenta al abrir la aplicación, al detectar reconexión,
   cada 30 segundos y mediante **Sincronizar pendientes**. No basta con que el navegador
   diga que tiene red: cada operación necesita confirmación de la API. Una sesión JWT
   vencida requiere iniciar sesión de nuevo con el mismo empleado.
6. El cierre se guarda provisionalmente y bloquea nuevos movimientos. Se sincroniza
   después de todas las operaciones anteriores. Los folios definitivos y el resultado
   del cierre se consultan después de recibir confirmación.

El panel conserva los identificadores, comprobantes y folios del turno después de
recargar. Permite exportar el diario a JSON para respaldo/conciliación; **ese archivo
no es una función de restauración automática**. Al preparar el siguiente turno se
archiva el diario anterior en IndexedDB. Cerrar sesión no borra el diario y no se
permite cambiar a otro empleado con un turno local activo o pendientes.

### Límites y conflictos

- Una preparación pertenece a un dispositivo/navegador por turno; no se puede abrir
  el mismo turno en otro equipo. Varias cajas desconectadas no comparten stock en
  tiempo real. Las pestañas del mismo origen comparten las reservas mediante una
  transacción IndexedDB, pero no equivalen a equipos independientes coordinados.
- No borrar un pendiente, editar sus importes/ID ni volver a cobrar cuando falta
  confirmación. Si el servidor detecta stock insuficiente, usuario desactivado,
  fecha no autorizada o contenido distinto bajo la misma clave, detiene la cola y
  muestra el motivo. El registro queda local, **no se descarta ni se contabiliza como
  sincronizado**. Exportar el diario y solicitar conciliación; esta versión no incluye
  un asistente para forzar ventas con stock negativo ni para importar respaldos.
- Los informes del servidor solo conocen lo ya sincronizado. Las ventas usan su fecha
  de captura; el registro de sincronización conserva también la fecha de recepción.
  Los pendientes locales aparecen en el panel, no en los informes centralizados.
- Un catálogo preparado no recibe nuevas existencias/precios durante ese turno.
  Compras/recepciones y administración siguen siendo operaciones online. El turno
  preparado no admite cobros bancarios, ajustes genéricos ni devoluciones desde su
  caja hasta cerrar y sincronizar. Las ventas de turnos preparados no se cancelan:
  usar devolución autorizada desde un turno online posterior.
- Login y apertura offline no están incluidos. Si nunca se preparó el equipo, no
  hay operación offline. Vite dev no proporciona la carga de la aplicación sin red;
  probar esa parte con el build de producción y el service worker.
- Se solicita almacenamiento persistente al navegador y commits IndexedDB estrictos,
  pero esto no sustituye respaldos. Borrado del perfil, avería de disco, falta de
  espacio o políticas de limpieza pueden impedir o destruir el almacenamiento local.
  Un regulador no mantiene encendida la computadora: usar un UPS/no-break.

### Validación

- Backend: `mvn -f pos.tienda\pom.xml -Dtest=OfflineIntegrationTest,SecurityConfigTest test`.
  La integración usa **H2 en memoria, no la base del negocio**. Cubre reintentos
  concurrentes, claves con contenido distinto, rollback, secuencia, precios congelados,
  dispositivo, stock insuficiente, efectivo y cierre.
- Diario local: con Vite ejecutándose, abrir `/tests/offline.html` en **un perfil de
  navegador nuevo y aislado**, sin datos ni sesión del negocio. La página utiliza
  IndexedDB real y una API simulada para probar persistencia, reservas concurrentes,
  confirmación perdida, cierre y aislamiento de usuario. Rechaza perfiles ya ocupados.
- Carga del build: `npm run build` y `node tests/serve-build.cjs` desde frontend.
  El servidor auxiliar solo escucha en `127.0.0.1:4175`, termina a los tres minutos y
  puede detenerse mediante `/__test_stop`. Preparar el service worker, detener ese
  servidor y recargar para comprobar la carga desde caché.
- **Antes del piloto:** aplicar ambas migraciones en MySQL de pruebas y repetir el
  circuito real con N ventas, movimientos, cortes de API, reinicio de navegador,
  reconexión y cierre. Verificar importes, existencias y confirmaciones exactamente
  una vez. H2 y API simulada no certifican la migración ni el comportamiento de MySQL.