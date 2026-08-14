import { useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface Categoria { id: number; nombre: string; }
interface Producto {
  id: number;
  codigo: string;
  nombre: string;
  precioVenta: number;
  precioCompra: number;
  stock: number;
  stockMinimo: number;
  activo: boolean;
  idCategoria?: number;
  nombreCategoria?: string;
}

const EMPTY: Producto = {
  id: 0, codigo: "", nombre: "", precioVenta: 0,
  precioCompra: 0, stock: 0, stockMinimo: 0, activo: true,
};

export default function Productos() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState<Producto>(EMPTY);
  const [editando, setEditando] = useState(false);
  const [confirmar, setConfirmar] = useState(false);
  const [confirmarEliminar, setConfirmarEliminar] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [errors, setErrors] = useState({ nombre: "", precioVenta: "" });

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const cargar = () => {
    api.get("/productos").then((r) => setProductos(r.data));
    api.get("/categorias").then((r) => setCategorias(r.data));
  };

  useEffect(() => { cargar(); }, []);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setModal(false);
        setConfirmar(false);
        setConfirmarEliminar(null);
      }
    };
    window.addEventListener("keydown", handleEsc);
    return () => window.removeEventListener("keydown", handleEsc);
  }, []);

  const filtrados = productos.filter((p) =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.codigo?.toLowerCase().includes(busqueda.toLowerCase())
  );

  const validate = () => {
    const newErrors = { nombre: "", precioVenta: "" };
    let valid = true;
    if (!form.nombre) { newErrors.nombre = "Nombre requerido"; valid = false; }
    if (!form.precioVenta || form.precioVenta <= 0) { newErrors.precioVenta = "Precio inválido"; valid = false; }
    setErrors(newErrors);
    return valid;
  };

  const abrirNuevo = () => { setForm(EMPTY); setEditando(false); setErrors({ nombre: "", precioVenta: "" }); setModal(true); };
  const abrirEditar = (p: Producto) => { setForm(p); setEditando(true); setErrors({ nombre: "", precioVenta: "" }); setModal(true); };

  const guardar = () => { if (!validate()) return; setConfirmar(true); };

  const guardarDatos = async () => {
    if (editando) {
      await api.put(`/productos/${form.id}`, form);
    } else {
      await api.post("/productos", form);
    }
  };

  const eliminarDatos = async () => {
    if (!confirmarEliminar) return;
    await api.delete(`/productos/${confirmarEliminar}`);
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Productos</h1>
        <p className="page-sub">Gestiona tu catálogo de productos</p>
      </div>

      <div className="card">
        <div className="card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h3 className="card-title">Catálogo</h3>
          <div style={{ display: "flex", gap: "10px" }}>
            <input className="search-input" placeholder="Buscar producto..." value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
            <button className="btn-primary" onClick={abrirNuevo}>+ Nuevo producto</button>
          </div>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>Código</th><th>Nombre</th><th>Categoría</th>
              <th>Precio venta</th><th>Stock</th><th>Estado</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filtrados.map((p) => (
              <tr key={p.id}>
                <td><code>{p.codigo || "—"}</code></td>
                <td>{p.nombre}</td>
                <td>{p.nombreCategoria || "—"}</td>
                <td>${p.precioVenta?.toFixed(2)}</td>
                <td>
                  <span className={p.stock <= p.stockMinimo ? "badge badge-danger" : "badge badge-success"}>
                    {p.stock}
                  </span>
                </td>
                <td>
                  <span className={p.activo ? "badge badge-success" : "badge badge-danger"}>
                    {p.activo ? "Activo" : "Inactivo"}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: "6px" }}>
                    <button className="btn-sm" onClick={() => abrirEditar(p)}>Editar</button>
                    <button className="btn-sm btn-danger" onClick={() => setConfirmarEliminar(p.id)}>Eliminar</button>
                  </div>
                </td>
              </tr>
            ))}
            {filtrados.length === 0 && (
              <tr><td colSpan={7} style={{ textAlign: "center", color: "#64748b", padding: "32px" }}>No hay productos</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* MODAL FORM */}
      {modal && (
        <div className="modal-overlay" onClick={() => setModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editando ? "Editar producto" : "Nuevo producto"}</h3>
              <button className="modal-close" onClick={() => setModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-field">
                  <label>Nombre *</label>
                  <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} placeholder="Nombre del producto" className={errors.nombre ? "input-error" : ""} />
                  {errors.nombre && <span className="error">{errors.nombre}</span>}
                </div>
                <div className="form-field">
                  <label>Código de barras</label>
                  <input value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })} placeholder="Ej. 7501000..." />
                </div>
                <div className="form-field">
                  <label>Precio venta *</label>
                  <input type="number" value={form.precioVenta} onChange={(e) => setForm({ ...form, precioVenta: +e.target.value })} className={errors.precioVenta ? "input-error" : ""} />
                  {errors.precioVenta && <span className="error">{errors.precioVenta}</span>}
                </div>
                <div className="form-field">
                  <label>Precio compra</label>
                  <input type="number" value={form.precioCompra} onChange={(e) => setForm({ ...form, precioCompra: +e.target.value })} />
                </div>
                <div className="form-field">
                  <label>Stock inicial</label>
                  <input type="number" value={form.stock} onChange={(e) => setForm({ ...form, stock: +e.target.value })} />
                </div>
                <div className="form-field">
                  <label>Stock mínimo</label>
                  <input type="number" value={form.stockMinimo} onChange={(e) => setForm({ ...form, stockMinimo: +e.target.value })} />
                </div>
                <div className="form-field">
                  <label>Categoría</label>
                  <select value={form.idCategoria || ""} onChange={(e) => setForm({ ...form, idCategoria: +e.target.value })}>
                    <option value="">Sin categoría</option>
                    {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                  </select>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
              <button className="btn-primary" onClick={guardar}>Guardar</button>
            </div>
          </div>
        </div>
      )}

      {/* CONFIRMAR GUARDAR */}
      {confirmar && (
        <div className="modal-overlay">
          <div className="modal small">
            {!loading ? (
              <>
                <div className="modal-header">
                  <h3>{editando ? "¿Guardar cambios?" : "¿Crear producto?"}</h3>
                </div>
                <div className="modal-body">
                  <p style={{ color: "#64748b", fontSize: "14px" }}>
                    {editando ? "Se actualizarán los datos del producto." : "Se agregará el producto al catálogo."}
                  </p>
                </div>
                <div className="modal-footer">
                  <button className="btn-secondary" onClick={() => setConfirmar(false)}>Cancelar</button>
                  <button className="btn-primary" onClick={async () => {
                    setLoading(true);
                    await new Promise((r) => setTimeout(r, 1000));
                    await guardarDatos();
                    showToast(editando ? "Producto actualizado correctamente" : "Producto creado correctamente");
                    setLoading(false);
                    setConfirmar(false);
                    setModal(false);
                    cargar();
                  }}>Confirmar</button>
                </div>
              </>
            ) : (
              <div className="loading-box">
                <div className="spinner" />
                <p>Guardando producto...</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* CONFIRMAR ELIMINAR */}
      {confirmarEliminar && (
        <div className="modal-overlay">
          <div className="modal small">
            {!loading ? (
              <>
                <div className="modal-header">
                  <h3>Eliminar producto</h3>
                  <button className="modal-close" onClick={() => setConfirmarEliminar(null)}>✕</button>
                </div>
                <div className="modal-body">
                  <div className="form-field" style={{ textAlign: "center" }}>
                    <label>¿Deseas eliminar este producto?</label>
                    <label className="danger-text">Esta acción no se puede deshacer</label>
                  </div>
                </div>
                <div className="modal-footer">
                  <button className="btn-secondary" onClick={() => setConfirmarEliminar(null)}>Cancelar</button>
                  <button className="btn-primary" style={{ background: "#ef4444" }} onClick={async () => {
                    setLoading(true);
                    await new Promise((r) => setTimeout(r, 1000));
                    await eliminarDatos();
                    showToast("Producto eliminado");
                    setLoading(false);
                    setConfirmarEliminar(null);
                    cargar();
                  }}>Eliminar</button>
                </div>
              </>
            ) : (
              <div className="loading-box">
                <div className="spinner" />
                <p>Eliminando producto...</p>
              </div>
            )}
          </div>
        </div>
      )}

      {toast && <div className="toast success">{toast}</div>}
    </Layout>
  );
}