import { useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface Categoria {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
}

const EMPTY: Categoria = {
  id: 0, nombre: "", descripcion: "", activo: true,
};

export default function Categorias() {
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState<Categoria>(EMPTY);
  const [editando, setEditando] = useState(false);
  const [confirmarEliminar, setConfirmarEliminar] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [errors, setErrors] = useState({ nombre: "" });

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2000);
  };

  const cargar = () => api.get("/categorias").then((r) => setCategorias(r.data));
  useEffect(() => { cargar(); }, []);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setModal(false);
        setConfirmarEliminar(null);
      }
    };
    window.addEventListener("keydown", handleEsc);
    return () => window.removeEventListener("keydown", handleEsc);
  }, []);

  const abrirNuevo = () => { setForm(EMPTY); setEditando(false); setErrors({ nombre: "" }); setModal(true); };
  const abrirEditar = (c: Categoria) => { setForm(c); setEditando(true); setErrors({ nombre: "" }); setModal(true); };

  const validate = () => {
    if (!form.nombre) {
      setErrors({ nombre: "Nombre requerido" });
      return false;
    }
    setErrors({ nombre: "" });
    return true;
  };

  const guardar = async () => {
    if (!validate()) return;
    setLoading(true);
    await new Promise((r) => setTimeout(r, 800));
    if (editando) {
      await api.put(`/categorias/${form.id}`, form);
      showToast("Categoría actualizada correctamente");
    } else {
      await api.post("/categorias", form);
      showToast("Categoría creada correctamente");
    }
    setLoading(false);
    setModal(false);
    cargar();
  };

  const eliminar = async () => {
    if (!confirmarEliminar) return;
    setLoading(true);
    await new Promise((r) => setTimeout(r, 800));
    await api.delete(`/categorias/${confirmarEliminar}`);
    showToast("Categoría eliminada");
    setLoading(false);
    setConfirmarEliminar(null);
    cargar();
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Categorías</h1>
        <p className="page-sub">Organiza tus productos por categoría</p>
      </div>

      <div className="card">
        <div className="card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h3 className="card-title">Lista de categorías</h3>
          <button className="btn-primary" onClick={abrirNuevo}>+ Nueva categoría</button>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Nombre</th>
              <th>Descripción</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {categorias.map((c) => (
              <tr key={c.id}>
                <td><span className="badge badge-success">{c.id}</span></td>
                <td>{c.nombre}</td>
                <td>{c.descripcion || "—"}</td>
                <td>
                  <span className={c.activo ? "badge badge-success" : "badge badge-danger"}>
                    {c.activo ? "Activa" : "Inactiva"}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: "6px" }}>
                    <button className="btn-sm" onClick={() => abrirEditar(c)}>Editar</button>
                    <button className="btn-sm btn-danger" onClick={() => setConfirmarEliminar(c.id)}>Eliminar</button>
                  </div>
                </td>
              </tr>
            ))}
            {categorias.length === 0 && (
              <tr>
                <td colSpan={5} style={{ textAlign: "center", color: "#64748b", padding: "32px" }}>
                  No hay categorías — crea la primera
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* MODAL FORM */}
      {modal && (
        <div className="modal-overlay" onClick={() => setModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editando ? "Editar categoría" : "Nueva categoría"}</h3>
              <button className="modal-close" onClick={() => setModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-grid" style={{ gridTemplateColumns: "1fr" }}>
                <div className="form-field">
                  <label>Nombre *</label>
                  <input
                    value={form.nombre}
                    onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                    placeholder="Ej. Bebidas, Snacks, Lácteos..."
                    className={errors.nombre ? "input-error" : ""}
                  />
                  {errors.nombre && <span className="error">{errors.nombre}</span>}
                </div>
                <div className="form-field">
                  <label>Descripción</label>
                  <input
                    value={form.descripcion}
                    onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
                    placeholder="Descripción opcional"
                  />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
              <button className="btn-primary" onClick={guardar} disabled={loading}>
                {loading ? "Guardando..." : editando ? "Guardar cambios" : "Crear categoría"}
              </button>
            </div>
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
                  <h3>Eliminar categoría</h3>
                  <button className="modal-close" onClick={() => setConfirmarEliminar(null)}>✕</button>
                </div>
                <div className="modal-body">
                  <div className="form-field" style={{ textAlign: "center" }}>
                    <label>¿Deseas eliminar esta categoría?</label>
                    <label className="danger-text">Esta acción no se puede deshacer</label>
                  </div>
                </div>
                <div className="modal-footer">
                  <button className="btn-secondary" onClick={() => setConfirmarEliminar(null)}>Cancelar</button>
                  <button className="btn-primary" style={{ background: "#ef4444" }} onClick={eliminar}>
                    Eliminar
                  </button>
                </div>
              </>
            ) : (
              <div className="loading-box">
                <div className="spinner" />
                <p>Eliminando categoría...</p>
              </div>
            )}
          </div>
        </div>
      )}

      {toast && <div className="toast success">{toast}</div>}
    </Layout>
  );
}