import { useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface Producto { id: number; nombre: string; }
interface Promocion {
  id: number;
  nombre: string;
  tipo: string;
  valor: number;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
  idProducto: number;
  nombreProducto: string;
}

const EMPTY: Promocion = {
  id: 0, nombre: "", tipo: "PORCENTAJE", valor: 0,
  fechaInicio: "", fechaFin: "", activo: true,
  idProducto: 0, nombreProducto: "",
};

export default function Promociones() {
  const [promociones, setPromociones] = useState<Promocion[]>([]);
  const [productos, setProductos] = useState<Producto[]>([]);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState<Promocion>(EMPTY);
  const [editando, setEditando] = useState(false);

  const cargar = () => {
    api.get("/promociones").then((r) => setPromociones(r.data));
    api.get("/productos").then((r) => setProductos(r.data));
  };
  useEffect(() => { cargar(); }, []);

  const abrirNuevo = () => { setForm(EMPTY); setEditando(false); setModal(true); };
  const abrirEditar = (p: Promocion) => { setForm(p); setEditando(true); setModal(true); };

  const guardar = async () => {
    if (!form.nombre || !form.idProducto) return;
    if (editando) {
      await api.put(`/promociones/${form.id}`, form);
    } else {
      await api.post("/promociones", form);
    }
    setModal(false);
    cargar();
  };

  const eliminar = async (id: number) => {
    if (!confirm("¿Eliminar promoción?")) return;
    await api.delete(`/promociones/${id}`);
    cargar();
  };

  const tipoLabel: Record<string, string> = {
    PORCENTAJE: "% Descuento",
    MONTO_FIJO: "$ Descuento",
    DOS_X_UNO:  "2x1",
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Promociones</h1>
        <p className="page-sub">Gestiona descuentos y ofertas</p>
      </div>

      <div className="card">
        <div className="card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h3 className="card-title">Lista de promociones</h3>
          <button className="btn-primary" onClick={abrirNuevo}>+ Nueva promoción</button>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Producto</th>
              <th>Tipo</th>
              <th>Valor</th>
              <th>Vigencia</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {promociones.map((p) => (
              <tr key={p.id}>
                <td>{p.nombre}</td>
                <td>{p.nombreProducto || "—"}</td>
                <td><span className="badge badge-warning">{tipoLabel[p.tipo] || p.tipo}</span></td>
                <td>{p.tipo === "DOS_X_UNO" ? "—" : p.tipo === "PORCENTAJE" ? `${p.valor}%` : `$${p.valor}`}</td>
                <td style={{ fontSize: "12px" }}>
                  {p.fechaInicio ? `${p.fechaInicio} → ${p.fechaFin}` : "Sin límite"}
                </td>
                <td>
                  <span className={p.activo ? "badge badge-success" : "badge badge-danger"}>
                    {p.activo ? "Activa" : "Inactiva"}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: "6px" }}>
                    <button className="btn-sm" onClick={() => abrirEditar(p)}>Editar</button>
                    <button className="btn-sm btn-danger" onClick={() => eliminar(p.id)}>Eliminar</button>
                  </div>
                </td>
              </tr>
            ))}
            {promociones.length === 0 && (
              <tr><td colSpan={7} style={{ textAlign: "center", color: "#64748b", padding: "32px" }}>
                No hay promociones
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <div className="modal-overlay" onClick={() => setModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editando ? "Editar promoción" : "Nueva promoción"}</h3>
              <button className="modal-close" onClick={() => setModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-field" style={{ gridColumn: "span 2" }}>
                  <label>Nombre *</label>
                  <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} placeholder="Ej. Descuento de verano" />
                </div>
                <div className="form-field">
                  <label>Producto *</label>
                  <select value={form.idProducto} onChange={(e) => setForm({ ...form, idProducto: +e.target.value })}>
                    <option value={0}>Selecciona un producto</option>
                    {productos.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                  </select>
                </div>
                <div className="form-field">
                  <label>Tipo</label>
                  <select value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
                    <option value="PORCENTAJE">% Descuento</option>
                    <option value="MONTO_FIJO">$ Descuento fijo</option>
                    <option value="DOS_X_UNO">2x1</option>
                  </select>
                </div>
                {form.tipo !== "DOS_X_UNO" && (
                  <div className="form-field">
                    <label>Valor</label>
                    <input type="number" value={form.valor} onChange={(e) => setForm({ ...form, valor: +e.target.value })} />
                  </div>
                )}
                <div className="form-field">
                  <label>Fecha inicio</label>
                  <input type="date" value={form.fechaInicio} onChange={(e) => setForm({ ...form, fechaInicio: e.target.value })} />
                </div>
                <div className="form-field">
                  <label>Fecha fin</label>
                  <input type="date" value={form.fechaFin} onChange={(e) => setForm({ ...form, fechaFin: e.target.value })} />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
              <button className="btn-primary" onClick={guardar}>
                {editando ? "Guardar cambios" : "Crear promoción"}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}