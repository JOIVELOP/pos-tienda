import { FormEvent, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface VentaLinea {
  id: number;
  idProducto: number;
  nombreProducto: string;
  codigoProducto?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}
interface Venta {
  id: number;
  folio: string;
  estatus: string;
  metodoPago: string;
  detalles: VentaLinea[];
}
interface DevolucionLinea {
  idDetalleVenta: number;
  producto: string;
  cantidad: number;
  reingresaInventario: boolean;
  montoReembolsado: number;
}
interface Devolucion {
  id: number;
  folio: string;
  folioVenta: string;
  fecha: string;
  motivo: string;
  metodoReembolso: string;
  totalReembolsado: number;
  usuario: string;
  lineas: DevolucionLinea[];
}
interface CantidadDevolver { cantidad: string; reingresaInventario: boolean; }

const money = (value: number) =>
  new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(value);

function errorText(error: unknown) {
  const response = (error as { response?: { data?: { error?: string } } }).response;
  return response?.data?.error || "No fue posible completar la operación.";
}

export default function Devoluciones() {
  const [folio, setFolio] = useState("");
  const [venta, setVenta] = useState<Venta | null>(null);
  const [devoluciones, setDevoluciones] = useState<Devolucion[]>([]);
  const [cantidades, setCantidades] = useState<Record<number, CantidadDevolver>>({});
  const [motivo, setMotivo] = useState("");
  const [metodoReembolso, setMetodoReembolso] = useState("EFECTIVO");
  const [error, setError] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [cargando, setCargando] = useState(false);

  const buscar = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    setMensaje("");
    setVenta(null);
    setDevoluciones([]);
    setCargando(true);
    try {
      const response = await api.get(`/ventas/folio/${encodeURIComponent(folio.trim())}`);
      const sale: Venta = response.data;
      if (sale.estatus !== "COMPLETADA") {
        setError("Solo se pueden devolver artículos de ventas completadas.");
        return;
      }
      const previous = await api.get(`/devoluciones/venta/${sale.id}`);
      setVenta(sale);
      setDevoluciones(previous.data);
      setCantidades(Object.fromEntries(sale.detalles.map((linea) => [
        linea.id, {
          cantidad: "0",
          reingresaInventario: false,
        },
      ])));
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  const cantidadDevuelta = (detalleId: number) =>
    devoluciones.flatMap((d) => d.lineas)
      .filter((linea) => linea.idDetalleVenta === detalleId)
      .reduce((sum, linea) => sum + linea.cantidad, 0);

  const importeDevuelto = (detalleId: number) =>
    devoluciones.flatMap((d) => d.lineas)
      .filter((linea) => linea.idDetalleVenta === detalleId)
      .reduce((sum, linea) => sum + linea.montoReembolsado, 0);

  const crear = async (event: FormEvent) => {
    event.preventDefault();
    if (!venta) return;
    const lineas = venta.detalles
      .map((detalle) => ({
        idDetalleVenta: detalle.id,
        cantidad: Number(cantidades[detalle.id]?.cantidad || 0),
        reingresaInventario: cantidades[detalle.id]?.reingresaInventario || false,
      }))
      .filter((linea) => linea.cantidad > 0);
    if (!lineas.length || !motivo.trim()) {
      setError("Indica el motivo y al menos un artículo/cantidad a devolver.");
      return;
    }
    setCargando(true);
    setError("");
    setMensaje("");
    try {
      const response = await api.post("/devoluciones", {
        idVenta: venta.id,
        motivo: motivo.trim(),
        metodoReembolso,
        lineas,
      });
      setDevoluciones([...devoluciones, response.data]);
      setMotivo("");
      setCantidades(Object.fromEntries(venta.detalles.map((detalle) => [
        detalle.id, { cantidad: "0", reingresaInventario: false },
      ])));
      setMensaje(`Devolución ${response.data.folio} registrada por ${money(response.data.totalReembolsado)}.`);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Devoluciones</h1>
        <p className="page-sub">Registra devoluciones parciales o totales con autorización</p>
      </div>
      {error && <div className="card" role="alert" style={{ color: "#b91c1c", marginBottom: 16 }}>{error}</div>}
      {mensaje && <div className="card" role="status" style={{ color: "#166534", marginBottom: 16 }}>{mensaje}</div>}

      <div className="card" style={{ marginBottom: 16 }}>
        <form onSubmit={buscar} className="form-grid">
          <div className="form-field">
            <label>Folio de la venta</label>
            <input value={folio} onChange={(e) => setFolio(e.target.value)}
              placeholder="Ej. VTA-0000000001" required />
          </div>
          <div className="form-field" style={{ alignSelf: "end" }}>
            <button className="btn-primary" disabled={cargando}>Buscar venta</button>
          </div>
        </form>
      </div>

      {venta && (
        <>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <h3 className="card-title">Venta {venta.folio}</h3>
              <span>Pago original: {venta.metodoPago}</span>
            </div>
            <form onSubmit={crear}>
              <div style={{ overflowX: "auto" }}>
                <table className="table">
                  <thead><tr><th>Producto</th><th>Vendidos</th><th>Ya devueltos</th><th>Reembolsable</th><th>Devolver</th><th>Buen estado / reingresar</th></tr></thead>
                  <tbody>{venta.detalles.map((detalle) => {
                    const returned = cantidadDevuelta(detalle.id);
                    const remaining = detalle.cantidad - returned;
                    const state = cantidades[detalle.id] || { cantidad: "0", reingresaInventario: false };
                    const set = (next: Partial<CantidadDevolver>) =>
                      setCantidades({ ...cantidades, [detalle.id]: { ...state, ...next } });
                    return <tr key={detalle.id}>
                      <td>{detalle.nombreProducto}{detalle.codigoProducto ? ` (${detalle.codigoProducto})` : ""}</td>
                      <td>{detalle.cantidad}</td><td>{returned}</td>
                      <td>{money(detalle.subtotal - importeDevuelto(detalle.id))}</td>
                      <td><input aria-label={`Cantidad a devolver ${detalle.nombreProducto}`}
                        type="number" min="0" max={remaining} value={state.cantidad}
                        onChange={(e) => set({ cantidad: e.target.value })} /></td>
                      <td><label style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        <input type="checkbox" checked={state.reingresaInventario}
                          onChange={(e) => set({ reingresaInventario: e.target.checked })} />
                        Reingresar
                      </label></td>
                    </tr>;
                  })}</tbody>
                </table>
              </div>
              <div className="form-grid" style={{ marginTop: 16 }}>
                <div className="form-field">
                  <label>Motivo</label>
                  <input value={motivo} onChange={(e) => setMotivo(e.target.value)}
                    maxLength={255} required placeholder="Motivo de la devolución" />
                </div>
                <div className="form-field">
                  <label>Método de reembolso a registrar</label>
                  <select value={metodoReembolso} onChange={(e) => setMetodoReembolso(e.target.value)}>
                    <option value="EFECTIVO">Efectivo</option>
                    <option value="TARJETA">Tarjeta</option>
                    <option value="TRANSFERENCIA">Transferencia</option>
                  </select>
                </div>
              </div>
              <p>Los reembolsos por tarjeta o transferencia deben ejecutarse también en la terminal o banco. Solo el efectivo se descuenta automáticamente del turno de caja.</p>
              <button className="btn-primary" disabled={cargando}>Autorizar y registrar devolución</button>
            </form>
          </div>

          <div className="card">
            <div className="card-header"><h3 className="card-title">Devoluciones previas</h3></div>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead><tr><th>Folio</th><th>Fecha</th><th>Motivo</th><th>Método</th><th>Autorizó</th><th>Total</th></tr></thead>
                <tbody>{devoluciones.map((d) => (
                  <tr key={d.id}><td>{d.folio}</td><td>{new Date(d.fecha).toLocaleString("es-MX")}</td>
                    <td>{d.motivo}</td><td>{d.metodoReembolso}</td><td>{d.usuario}</td>
                    <td>{money(d.totalReembolsado)}</td></tr>
                ))}</tbody>
              </table>
              {devoluciones.length === 0 && <p style={{ padding: 16 }}>Esta venta no tiene devoluciones.</p>}
            </div>
          </div>
        </>
      )}
    </Layout>
  );
}
