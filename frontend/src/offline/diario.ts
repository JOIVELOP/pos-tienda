import axios from "axios";
import api from "../api/axios";

export interface Articulo {
  id: number; codigo: string; nombre: string; precioVenta: number; stock: number;
}
export interface Sesion {
  id: string; dispositivo: string; usuario: number; turno: number; folioTurno: string;
  vence: string; cerrada: boolean; secuencia: number; efectivo: number; productos: Articulo[];
}
export type Tipo = "VENTA" | "APORTACION" | "GASTO" | "RETIRO" | "CIERRE";
export interface Operacion {
  id: string; sesion: string; dispositivo: string; secuencia: number; captura: string;
  tipo: Tipo; monto: number; descripcion: string;
  lineas: { idProducto: number; cantidad: number }[];
}
export interface Registro {
  operacion: Operacion; folio?: string;
}
export interface Diario {
  sesion: Sesion; registros: Registro[]; error: string;
}

function usuarioActual(): number {
  const usuario: { id?: number } = JSON.parse(localStorage.getItem("user") || "{}");
  if (!usuario.id) throw new Error("Inicia sesión con el propietario del turno.");
  return usuario.id;
}

let database: Promise<IDBDatabase> | undefined;
function db() {
  if (!database) {
    database = new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("pos-diario", 1);
      request.onupgradeneeded = () => request.result.createObjectStore("datos");
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(new Error("No se pudo abrir el diario local. No registres cobros."));
      request.onblocked = () => reject(new Error("Cierra otras pestañas para actualizar el diario."));
    });
  }
  return database;
}

// All local reservations and journal writes share one read/write transaction across tabs.
async function modificar<T>(actualizar: (diario: Diario | undefined, store: IDBObjectStore) => T): Promise<T> {
  const database = await db();
  return new Promise<T>((resolve, reject) => {
    const tx = database.transaction("datos", "readwrite", { durability: "strict" });
    const store = tx.objectStore("datos");
    const request = store.get("actual");
    let result: T;
    let failure: unknown;
    request.onsuccess = () => {
      try { result = actualizar(request.result as Diario | undefined, store); }
      catch (error) { failure = error; tx.abort(); }
    };
    tx.oncomplete = () => { avisar(); resolve(result); };
    tx.onabort = () => reject(failure || new Error("No se pudo guardar en el equipo. No confirmes el cobro."));
    tx.onerror = () => { failure = new Error("Error de almacenamiento local; conserva los datos y solicita ayuda."); };
  });
}

export async function leerDiario(): Promise<Diario | undefined> {
  const database = await db();
  return new Promise((resolve, reject) => {
    const request = database.transaction("datos").objectStore("datos").get("actual");
    request.onsuccess = () => resolve(request.result as Diario | undefined);
    request.onerror = () => reject(new Error("No se pudo leer el diario local."));
  });
}

const canal = typeof BroadcastChannel !== "undefined" ? new BroadcastChannel("pos-diario") : undefined;
function avisar() {
  window.dispatchEvent(new Event("diario"));
  canal?.postMessage("cambio");
}
if (canal) canal.onmessage = () => window.dispatchEvent(new Event("diario"));

export const pendientes = (diario: Diario) => diario.registros.filter(r => !r.folio);
export function propio(diario: Diario) {
  if (diario.sesion.usuario !== usuarioActual()) {
    throw new Error("Hay un turno local de otro empleado. Inicia sesión con su usuario; no borres los datos.");
  }
  return diario;
}

export async function preparar() {
  if (import.meta.env.PROD) {
    if (!("serviceWorker" in navigator)) throw new Error("La carga offline requiere HTTPS o localhost.");
    await Promise.race([
      navigator.serviceWorker.ready,
      new Promise<never>((_resolve, reject) => window.setTimeout(
        () => reject(new Error("La aplicación aún no está lista para cargar sin conexión. Recarga online.")), 15000)),
    ]);
  }
  if (navigator.storage?.persist) await navigator.storage.persist();
  const anterior = await leerDiario();
  if (anterior) {
    propio(anterior);
    if (!anterior.sesion.cerrada || pendientes(anterior).length) {
      throw new Error("Ya hay un turno preparado. Continúa con él o sincroniza su cierre.");
    }
  }
  // Persist the device ID before requesting authorization so a lost response is recoverable.
  const dispositivo = await modificar((_diario, store) => {
    const id = anterior?.sesion.dispositivo || localStorage.getItem("pos-dispositivo") || crypto.randomUUID();
    localStorage.setItem("pos-dispositivo", id);
    store.put(id, "dispositivo");
    return id;
  });
  const response = await api.post<Sesion>("/offline/preparar", { dispositivo }, { timeout: 15000 });
  if (response.data.usuario !== usuarioActual()) throw new Error("La sesión cambió durante la preparación.");
  await modificar((actual, store) => {
    if (actual && (!actual.sesion.cerrada || pendientes(actual).length)) {
      throw new Error("Otra pestaña ya preparó el turno. Actualiza la pantalla.");
    }
    if (actual) store.put(actual, `archivo-${actual.sesion.id}`);
    store.put({ sesion: response.data, registros: [], error: "" } satisfies Diario, "actual");
  });
}

export async function registrar(tipo: Tipo, monto: number, descripcion: string,
                                 lineas: Operacion["lineas"] = []): Promise<Operacion> {
  return modificar((diario, store) => {
    if (!diario) throw new Error("Prepara la caja online antes de operar sin conexión.");
    propio(diario);
    const sesion = diario.sesion;
    if (sesion.cerrada) throw new Error("El cierre provisional bloquea nuevos movimientos.");
    if (tipo !== "CIERRE" && Date.now() > Date.parse(sesion.vence)) {
      throw new Error("Terminó la autorización de 24 horas. Sincroniza y cierra el turno.");
    }
    if (!Number.isFinite(monto) || monto < 0 || monto > 9999999999.99 || (tipo !== "CIERRE" && monto === 0)
        || Math.abs(monto * 100 - Math.round(monto * 100)) > 0.00001) {
      throw new Error("Importe inválido; utiliza un máximo de dos decimales.");
    }
    if (!descripcion.trim() || descripcion.length > 255) throw new Error("Descripción inválida.");
    if (tipo === "VENTA") {
      if (!lineas.length || new Set(lineas.map(l => l.idProducto)).size !== lineas.length) {
        throw new Error("Artículos inválidos o repetidos.");
      }
      let centavos = 0;
      for (const linea of lineas) {
        const producto = sesion.productos.find(p => p.id === linea.idProducto);
        if (!producto || !Number.isSafeInteger(linea.cantidad) || linea.cantidad <= 0
            || linea.cantidad > producto.stock) throw new Error("Existencia local insuficiente.");
        centavos += Math.round(producto.precioVenta * 100) * linea.cantidad;
        producto.stock -= linea.cantidad;
      }
      if (centavos !== Math.round(monto * 100)) throw new Error("El catálogo cambió; vuelve a cargar el carrito.");
    } else if (lineas.length) throw new Error("Solo una venta admite artículos.");
    const salida = tipo === "GASTO" || tipo === "RETIRO";
    if (salida && monto > sesion.efectivo) throw new Error("Efectivo local insuficiente.");
    if (salida) sesion.efectivo = Math.round((sesion.efectivo - monto) * 100) / 100;
    if (tipo === "VENTA" || tipo === "APORTACION") {
      sesion.efectivo = Math.round((sesion.efectivo + monto) * 100) / 100;
    }
    if (sesion.efectivo > 9999999999.99) throw new Error("Se excedió el límite de efectivo del turno.");
    const operacion: Operacion = {
      id: crypto.randomUUID(), sesion: sesion.id, dispositivo: sesion.dispositivo,
      secuencia: ++sesion.secuencia, captura: new Date().toISOString(), tipo,
      monto, descripcion: descripcion.trim(), lineas,
    };
    if (tipo === "CIERRE") sesion.cerrada = true;
    diario.registros.push({ operacion });
    store.put(diario, "actual");
    return operacion;
  });
}

let sincronizando: Promise<void> | undefined;
export function sincronizar(): Promise<void> {
  if (sincronizando) return sincronizando;
  sincronizando = enviar().finally(() => { sincronizando = undefined; });
  return sincronizando;
}

async function enviar() {
  let diario = await leerDiario();
  if (!diario || !pendientes(diario).length) return;
  propio(diario);
  while (diario && pendientes(diario).length) {
    const siguiente = pendientes(diario)[0].operacion;
    try {
      const response = await api.post<{ id: string; folio: string; secuencia: number }>(
        "/offline/sincronizar", siguiente, { timeout: 15000 });
      if (response.data.id !== siguiente.id || response.data.secuencia !== siguiente.secuencia
          || !response.data.folio) throw new Error("Confirmación del servidor inválida.");
      await modificar((actual, store) => {
        if (!actual || actual.sesion.id !== siguiente.sesion) throw new Error("El turno local cambió.");
        const registro = actual.registros.find(r => r.operacion.id === siguiente.id);
        if (!registro) throw new Error("Operación local no encontrada; solicita conciliación.");
        registro.folio = response.data.folio;
        actual.error = "";
        store.put(actual, "actual");
      });
    } catch (error) {
      let mensaje = "Sin confirmación del servidor. Los pendientes siguen guardados; no vuelvas a cobrar.";
      if (axios.isAxiosError<{ error?: string }>(error) && error.response) {
        mensaje = error.response.status === 401 || error.response.status === 403
          ? "Vuelve a iniciar sesión con el mismo empleado para sincronizar."
          : error.response.data?.error || `Servidor respondió ${error.response.status}. Conserva los pendientes.`;
      } else if (error instanceof Error && !axios.isAxiosError(error)) mensaje = error.message;
      await modificar((actual, store) => {
        if (actual) { actual.error = mensaje; store.put(actual, "actual"); }
      });
      throw new Error(mensaje);
    }
    diario = await leerDiario();
  }
}
