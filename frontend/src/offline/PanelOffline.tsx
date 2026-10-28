import { useEffect, useState } from "react";
import { Diario, leerDiario, pendientes, preparar, sincronizar } from "./diario";

export default function PanelOffline() {
  const [diario, setDiario] = useState<Diario>();
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [red, setRed] = useState(navigator.onLine);
  useEffect(() => {
    const cargar = () => { void leerDiario().then(setDiario).catch(e => setError(String(e))); };
    const enviar = () => {
      void sincronizar().then(() => setError("")).catch(e => setError(e instanceof Error ? e.message : String(e)));
    };
    const conexion = () => setRed(navigator.onLine);
    cargar();
    enviar();
    window.addEventListener("diario", cargar);
    window.addEventListener("online", enviar);
    window.addEventListener("online", conexion);
    window.addEventListener("offline", conexion);
    const shellError = (event: Event) => setError((event as CustomEvent<string>).detail);
    window.addEventListener("offline-shell-error", shellError);
    const timer = window.setInterval(enviar, 30000);
    return () => {
      window.removeEventListener("diario", cargar);
      window.removeEventListener("online", enviar);
      window.removeEventListener("online", conexion);
      window.removeEventListener("offline", conexion);
      window.removeEventListener("offline-shell-error", shellError);
      window.clearInterval(timer);
    };
  }, []);

  const accion = async (preparacion: boolean) => {
    setOcupado(true); setError("");
    try { await (preparacion ? preparar() : sincronizar()); }
    catch (e) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setOcupado(false); }
  };
  const exportar = () => {
    const blob = new Blob([JSON.stringify(diario, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url; link.download = `diario-${diario?.sesion.folioTurno}.json`; link.click();
    URL.revokeObjectURL(url);
  };
  return <section className="card" style={{ marginBottom: 16 }} aria-label="Continuidad de caja">
    <strong>{diario ? `Diario ${diario.sesion.folioTurno}` : "Continuidad sin internet"}</strong>
    <p>{red ? "Red detectada; la confirmación del servidor determina la sincronización." : "Sin conexión de red: conserva los pendientes en este equipo."}</p>
    <p>{diario ? `${pendientes(diario).length} pendientes. ${diario.sesion.cerrada ? "Cierre local registrado." :
      `Autorizado hasta ${new Date(diario.sesion.vence).toLocaleString()}. Efectivo local: $${diario.sesion.efectivo.toFixed(2)}.`}`
      : "Abre un turno online y prepara este navegador antes de un corte. Una caja por turno."}</p>
    <p>En caja preparada: solo efectivo, precios base congelados y público en general.
      No borres los datos del navegador. La sincronización requiere el mismo empleado.
      La carga tras reiniciar sin red requiere el build de producción preparado online (no Vite dev).</p>
    <button disabled={ocupado} onClick={() => void accion(false)}>Sincronizar pendientes</button>{" "}
    {(!diario || (diario.sesion.cerrada && !pendientes(diario).length)) &&
      <button disabled={ocupado} onClick={() => void accion(true)}>Preparar turno online</button>}
    {diario && <button onClick={exportar}>Exportar respaldo del diario</button>}
    {(error || diario?.error) && <p role="alert">{error || diario?.error}</p>}
    {diario && <details><summary>Operaciones y folios del turno</summary>
      <ul>{diario.registros.map(r => <li key={r.operacion.id}>
        {r.operacion.secuencia}: {r.operacion.tipo} ${r.operacion.monto.toFixed(2)}
        {" — "}{r.folio || `PROVISIONAL ${r.operacion.id}`}
        <details><summary>Comprobante recuperable</summary>
          <p>Identificador: {r.operacion.id}. Captura: {new Date(r.operacion.captura).toLocaleString()}.
            {" "}{r.operacion.descripcion}</p>
          <ul>{r.operacion.lineas.map(linea => <li key={linea.idProducto}>
            {diario.sesion.productos.find(p => p.id === linea.idProducto)?.nombre || `Producto ${linea.idProducto}`}
            {" × "}{linea.cantidad}
          </li>)}</ul>
        </details>
      </li>)}</ul>
    </details>}
  </section>;
}
