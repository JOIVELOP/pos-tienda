import { FormEvent, useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface Proveedor { id: number; nombre: string; activo: boolean; }
interface Producto { id: number; nombre: string; codigo?: string; precioCompra: number; }
interface CompraLinea {
  id: number; idProducto: number; nombreProducto: string; codigoProducto?: string;
  cantidadSolicitada: number; cantidadRecibida: number; costoUnitario: number;
}
interface RecepcionLinea {
  idLineaCompra: number; producto: string; cantidadBuena: number;
  cantidadDanada: number; costoUnitario: number; observacion?: string;
}
interface Recepcion {
  id: number; folio: string; fecha: string; usuario: string; lineas: RecepcionLinea[];
}
interface Compra {
  id: number; folio: string; estado: string; fecha: string;
  nombreProveedor: string; lineas: CompraLinea[]; recepciones: Recepcion[];
}
interface NuevaLinea { idProducto: number; cantidadSolicitada: number; costoUnitario: number; }
interface CapturaRecepcion { buena: string; danada: string; costo: string; observacion: string; }

const money = (value: number) =>
  new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(value);

function errorText(error: unknown) {
  const response = (error as { response?: { data?: { error?: string } } }).response;
  return response?.data?.error || "No fue posible completar la operación.";
}

export default function Compras() {
  const [compras, setCompras] = useState<Compra[]>([]);
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [productos, setProductos] = useState<Producto[]>([]);
  const [idProveedor, setIdProveedor] = useState("");
  const [lineas, setLineas] = useState<NuevaLinea[]>([]);
  const [idProducto, setIdProducto] = useState("");
  const [cantidad, setCantidad] = useState("1");
  const [costo, setCosto] = useState("");
  const [compraRecepcion, setCompraRecepcion] = useState<Compra | null>(null);
  const [capturas, setCapturas] = useState<Record<number, CapturaRecepcion>>({});
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);

  const cargar = async () => {
    try {
      const [orders, suppliers, items] = await Promise.all([
        api.get("/compras"), api.get("/proveedores"), api.get("/productos"),
      ]);
      setCompras(orders.data);
      setProveedores(suppliers.data.filter((p: Proveedor) => p.activo));
      setProductos(items.data.filter((p: Producto & { activo: boolean }) => p.activo));
    } catch (e) {
      setError(errorText(e));
    }
  };
  useEffect(() => { void cargar(); }, []);

  const agregarLinea = () => {
    const productId = Number(idProducto);
    if (!productId || Number(cantidad) < 1 || Number(costo) < 0) {
      setError("Selecciona un producto e indica una cantidad y costo válidos.");
      return;
    }
    if (lineas.some((linea) => linea.idProducto === productId)) {
      setError("Agrega cada producto una sola vez a la orden.");
      return;
    }
    setError("");
    setLineas([...lineas, {
      idProducto: productId,
      cantidadSolicitada: Number(cantidad),
      costoUnitario: Number(costo),
    }]);
    setIdProducto("");
    setCantidad("1");
    setCosto("");
  };

  const crear = async (event: FormEvent) => {
    event.preventDefault();
    if (!idProveedor || lineas.length === 0) {
      setError("Selecciona proveedor y agrega al menos un producto.");
      return;
    }
    setCargando(true);
    setError("");
    try {
      await api.post("/compras", { idProveedor: Number(idProveedor), lineas });
      setLineas([]);
      setIdProveedor("");
      await cargar();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  const iniciarRecepcion = (compra: Compra) => {
    setCompraRecepcion(compra);
    setCapturas(Object.fromEntries(compra.lineas
      .filter((linea) => linea.cantidadRecibida < linea.cantidadSolicitada)
      .map((linea) => [linea.id, {
        buena: "0", danada: "0", costo: String(linea.costoUnitario),
        observacion: "",
      }])));
    setError("");
  };

  const recibir = async (event: FormEvent) => {
    event.preventDefault();
    if (!compraRecepcion) return;
    const receiptLines = compraRecepcion.lineas
      .filter((linea) => linea.cantidadRecibida < linea.cantidadSolicitada)
      .map((linea) => ({
        idLineaCompra: linea.id,
        cantidadBuena: Number(capturas[linea.id]?.buena || 0),
        cantidadDanada: Number(capturas[linea.id]?.danada || 0),
        costoUnitario: Number(capturas[linea.id]?.costo || 0),
        observacion: capturas[linea.id]?.observacion || "",
      }))
      .filter((linea) => linea.cantidadBuena + linea.cantidadDanada > 0);
    if (!receiptLines.length) {
      setError("Indica la cantidad buena o dañada de al menos un producto.");
      return;
    }
    setCargando(true);
    setError("");
    try {
      await api.post(`/compras/${compraRecepcion.id}/recepciones`, { lineas: receiptLines });
      setCompraRecepcion(null);
      await cargar();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Compras y recepción</h1>
        <p className="page-sub">Controla pedidos a proveedores y entradas reales de mercancía</p>
      </div>
      {error && <div className="card" role="alert" style={{ color: "#b91c1c", marginBottom: 16 }}>{error}</div>}

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="card-header"><h3 className="card-title">Nueva orden de compra</h3></div>
        <form onSubmit={crear}>
          <div className="form-grid">
            <div className="form-field">
              <label>Proveedor</label>
              <select value={idProveedor} onChange={(e) => setIdProveedor(e.target.value)} required>
                <option value="">Selecciona proveedor</option>
                {proveedores.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label>Producto</label>
              <select value={idProducto} onChange={(e) => {
                setIdProducto(e.target.value);
                const product = productos.find((p) => p.id === Number(e.target.value));
                if (product) setCosto(String(product.precioCompra || 0));
              }}>
                <option value="">Selecciona producto</option>
                {productos.map((p) => <option key={p.id} value={p.id}>{p.codigo ? `${p.codigo} · ` : ""}{p.nombre}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label>Cantidad solicitada</label>
              <input type="number" min="1" step="1" value={cantidad}
                onChange={(e) => setCantidad(e.target.value)} />
            </div>
            <div className="form-field">
              <label>Costo unitario estimado</label>
              <input type="number" min="0" step="0.01" value={costo}
                onChange={(e) => setCosto(e.target.value)} />
            </div>
          </div>
          <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
            <button type="button" className="btn-secondary" onClick={agregarLinea}>Agregar producto</button>
            <button className="btn-primary" disabled={cargando || !lineas.length}>Crear orden</button>
          </div>
        </form>
        {lineas.length > 0 && (
          <div style={{ overflowX: "auto", marginTop: 16 }}>
            <table className="table">
              <thead><tr><th>Producto</th><th>Cantidad</th><th>Costo unitario</th><th>Importe</th><th /></tr></thead>
              <tbody>{lineas.map((linea) => {
                const product = productos.find((p) => p.id === linea.idProducto);
                return <tr key={linea.idProducto}>
                  <td>{product?.nombre}</td><td>{linea.cantidadSolicitada}</td>
                  <td>{money(linea.costoUnitario)}</td>
                  <td>{money(linea.costoUnitario * linea.cantidadSolicitada)}</td>
                  <td><button type="button" className="btn-sm btn-danger"
                    onClick={() => setLineas(lineas.filter((l) => l.idProducto !== linea.idProducto))}>Quitar</button></td>
                </tr>;
              })}</tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header"><h3 className="card-title">Órdenes</h3></div>
        <div style={{ overflowX: "auto" }}>
          <table className="table">
            <thead><tr><th>Folio</th><th>Fecha</th><th>Proveedor</th><th>Estado</th><th>Artículos</th><th>Historial de recepción</th><th>Acción</th></tr></thead>
            <tbody>{compras.map((compra) => (
              <tr key={compra.id}>
                <td>{compra.folio}</td><td>{new Date(compra.fecha).toLocaleString("es-MX")}</td>
                <td>{compra.nombreProveedor}</td><td>{compra.estado}</td>
                <td>{compra.lineas.map((l) => `${l.nombreProducto}: ${l.cantidadRecibida}/${l.cantidadSolicitada}`).join(", ")}</td>
                <td>{compra.recepciones.length > 0 ? (
                  <details>
                    <summary>{compra.recepciones.length} recepción(es)</summary>
                    {compra.recepciones.map((recepcion) => (
                      <div key={recepcion.id} style={{ marginTop: 8 }}>
                        <strong>{recepcion.folio}</strong> · {new Date(recepcion.fecha).toLocaleString("es-MX")} · {recepcion.usuario}
                        {recepcion.lineas.map((linea, index) => (
                          <div key={`${recepcion.id}-${linea.idLineaCompra}-${index}`}>
                            {linea.producto}: {linea.cantidadBuena} buena, {linea.cantidadDanada} dañada;
                            costo {money(linea.costoUnitario)}{linea.observacion ? `; ${linea.observacion}` : ""}
                          </div>
                        ))}
                      </div>
                    ))}
                  </details>
                ) : "Sin recepciones"}</td>
                <td>{compra.estado !== "RECIBIDA" &&
                  <button className="btn-sm" onClick={() => iniciarRecepcion(compra)}>Recibir mercancía</button>}</td>
              </tr>
            ))}</tbody>
          </table>
          {compras.length === 0 && <p style={{ padding: 16 }}>Todavía no hay órdenes de compra.</p>}
        </div>
      </div>

      {compraRecepcion && (
        <div className="modal-overlay" onClick={() => setCompraRecepcion(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Recepción de {compraRecepcion.folio}</h3>
              <button className="modal-close" onClick={() => setCompraRecepcion(null)}>✕</button>
            </div>
            <form onSubmit={recibir}>
              <div className="modal-body" style={{ overflowX: "auto" }}>
                <p>La cantidad dañada queda registrada, pero no aumenta el inventario.</p>
                <table className="table">
                  <thead><tr><th>Producto</th><th>Pendiente</th><th>Buena</th><th>Dañada</th><th>Costo unitario real</th><th>Observación / daño</th></tr></thead>
                  <tbody>{compraRecepcion.lineas.filter((l) => l.cantidadRecibida < l.cantidadSolicitada).map((linea) => {
                    const captura = capturas[linea.id] || { buena: "0", danada: "0", costo: String(linea.costoUnitario), observacion: "" };
                    const set = (key: keyof CapturaRecepcion, value: string) =>
                      setCapturas({ ...capturas, [linea.id]: { ...captura, [key]: value } });
                    return <tr key={linea.id}>
                      <td>{linea.nombreProducto}</td>
                      <td>{linea.cantidadSolicitada - linea.cantidadRecibida}</td>
                      <td><input aria-label={`Cantidad buena ${linea.nombreProducto}`} type="number" min="0"
                        max={linea.cantidadSolicitada - linea.cantidadRecibida} value={captura.buena}
                        onChange={(e) => set("buena", e.target.value)} /></td>
                      <td><input aria-label={`Cantidad dañada ${linea.nombreProducto}`} type="number" min="0"
                        max={linea.cantidadSolicitada - linea.cantidadRecibida} value={captura.danada}
                        onChange={(e) => set("danada", e.target.value)} /></td>
                      <td><input aria-label={`Costo unitario ${linea.nombreProducto}`} type="number" min="0" step="0.01"
                        value={captura.costo} onChange={(e) => set("costo", e.target.value)} /></td>
                      <td><input aria-label={`Observación de recepción ${linea.nombreProducto}`} maxLength={255}
                        value={captura.observacion} onChange={(e) => set("observacion", e.target.value)} /></td>
                    </tr>;
                  })}</tbody>
                </table>
                {compraRecepcion.lineas.some((linea) => linea.cantidadRecibida < linea.cantidadSolicitada) &&
                  <div className="modal-footer">
                    <button type="button" className="btn-secondary" onClick={() => setCompraRecepcion(null)}>Cancelar</button>
                    <button className="btn-primary" disabled={cargando}>Registrar recepción</button>
                  </div>}
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
