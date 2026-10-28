import api from "../src/api/axios";
import { preparar, registrar, leerDiario, pendientes, sincronizar } from "../src/offline/diario";

const output = document.getElementById("resultados")!;
const results: string[] = [];
function equal(actual: unknown, expected: unknown) {
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    throw new Error(`Esperado ${JSON.stringify(expected)}, obtenido ${JSON.stringify(actual)}`);
  }
}
async function rejects(action: Promise<unknown>) {
  let rejected = false;
  try { await action; } catch { rejected = true; }
  if (!rejected) throw new Error("Se esperaba un rechazo");
}
async function setup() {
  localStorage.setItem("user", JSON.stringify({ id: 1 }));
  const database = await new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open("pos-diario", 1);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  await new Promise<void>((resolve, reject) => {
    const tx = database.transaction("datos", "readwrite");
    tx.objectStore("datos").clear();
    tx.oncomplete = () => resolve();
    tx.onabort = () => reject(tx.error);
  });
  database.close();
  api.defaults.adapter = async config => ({
    config, status: 200, statusText: "OK", headers: {},
    data: { id: "session", dispositivo: JSON.parse(config.data).dispositivo, usuario: 1,
      turno: 1, folioTurno: "CAJ-1", vence: new Date(Date.now() + 86400000).toISOString(),
      cerrada: false, secuencia: 0, efectivo: 100,
      productos: [{ id: 1, codigo: "A", nombre: "Producto", precioVenta: 10, stock: 5 }] },
  });
  await preparar();
}
async function test(name: string, action: () => Promise<void>) {
  await setup();
  try { await action(); results.push(`PASS ${name}`); }
  catch (error) { results.push(`FAIL ${name}: ${String(error)}`); }
  output.textContent = results.join("\n");
}

async function run() {
  if (localStorage.getItem("token") || await leerDiario()) {
    throw new Error("Perfil ocupado: abre estas pruebas en un contexto de navegador nuevo y aislado.");
  }
  await test("venta persistida y reserva local", async () => {
    await registrar("VENTA", 20, "Venta", [{ idProducto: 1, cantidad: 2 }]);
    const recovered = (await leerDiario())!;
    equal(pendientes(recovered).length, 1);
    equal(recovered.sesion.productos[0].stock, 3);
    equal(recovered.sesion.efectivo, 120);
  });
  await test("concurrencia local no vende stock dos veces", async () => {
    const concurrent = await Promise.allSettled([
      registrar("VENTA", 40, "Venta", [{ idProducto: 1, cantidad: 4 }]),
      registrar("VENTA", 40, "Venta", [{ idProducto: 1, cantidad: 4 }]),
    ]);
    equal(concurrent.filter(r => r.status === "fulfilled").length, 1);
    equal((await leerDiario())!.sesion.productos[0].stock, 1);
  });
  await test("captura inválida revierte reserva, efectivo y secuencia", async () => {
    await rejects(registrar("VENTA", 19, "Venta", [{ idProducto: 1, cantidad: 2 }]));
    const recovered = (await leerDiario())!;
    equal(recovered.sesion.productos[0].stock, 5);
    equal(recovered.sesion.efectivo, 100);
    equal(recovered.sesion.secuencia, 0);
    equal(recovered.registros.length, 0);
  });
  await test("respuesta perdida conserva identificador para reintento", async () => {
    const sale = await registrar("VENTA", 20, "Venta", [{ idProducto: 1, cantidad: 2 }]);
    const ids: string[] = [];
    api.defaults.adapter = async config => {
      ids.push(JSON.parse(config.data).id);
      throw new Error("Conexión interrumpida");
    };
    await rejects(sincronizar());
    equal(pendientes((await leerDiario())!).length, 1);
    api.defaults.adapter = async config => {
      const payload = JSON.parse(config.data);
      ids.push(payload.id);
      return { config, status: 200, statusText: "OK", headers: {},
        data: { id: payload.id, secuencia: payload.secuencia, folio: "VTA-1" } };
    };
    await sincronizar();
    equal(ids, [sale.id, sale.id]);
    equal(pendientes((await leerDiario())!).length, 0);
  });
  await test("cierre provisional bloquea nuevos movimientos", async () => {
    await registrar("CIERRE", 99, "Cierre");
    await rejects(registrar("APORTACION", 1, "Entrada"));
    equal((await leerDiario())!.sesion.cerrada, true);
  });
  await test("confirmación inválida detiene la cola sin perder pendientes", async () => {
    await registrar("APORTACION", 1, "Entrada");
    await registrar("GASTO", 1, "Salida");
    let enviados = 0;
    api.defaults.adapter = async config => {
      enviados++;
      return { config, status: 200, statusText: "OK", headers: {},
        data: { id: "otro-id", secuencia: 1, folio: "MOV-1" } };
    };
    await rejects(sincronizar());
    equal(enviados, 1);
    equal(pendientes((await leerDiario())!).length, 2);
  });
  await test("otro usuario no escribe ni sincroniza el turno", async () => {
    await registrar("APORTACION", 1, "Entrada");
    localStorage.setItem("user", JSON.stringify({ id: 2 }));
    await rejects(registrar("APORTACION", 1, "Entrada"));
    await rejects(sincronizar());
    equal((await leerDiario())!.registros.length, 1);
  });
  document.body.dataset.result = results.some(r => r.startsWith("FAIL")) ? "failed" : "passed";
}
void run().catch(error => {
  output.textContent = String(error);
  document.body.dataset.result = "failed";
});
