import { useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface Proveedor {
  id: number;
  nombre: string;
  rfc: string;
  telefono: string;
  email: string;
  direccion: string;
  activo: boolean;
}

const EMPTY: Proveedor = {
  id: 0, nombre: "", rfc: "", telefono: "",
  email: "", direccion: "", activo: true,
};

export default function Proveedores() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState<Proveedor>(EMPTY);
  const [editando, setEditando] = useState(false);

  const cargar = () => api.get("/proveedores").then((r) => setProveedores(r.data));
  useEffect(() => { cargar(); }, []);

  const filtrados = proveedores.filter((p) =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.rfc?.toLowerCase().includes(busqueda.toLowerCase())
  );

  const abrirNuevo = () => { setForm(EMPTY); setEditando(false); setModal(true); };
  const abrirEditar = (p: Proveedor) => { setForm(p); setEditando(true); setModal(true); };

  const guardar = async () => {
    if (!form.nombre) return;
    if (editando) {
      await api.put(`/proveedores/${form.id}`, form);
    } else {
      await api.post("/proveedores", form);
    }
    setModal(false);
    cargar();
  };

  const eliminar = async (id: number) => {
    if (!confirm("¿Eliminar proveedor?")) return;
    await api.delete(`/proveedores/${id}`);
    cargar();
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Proveedores</h1>
        <p className="page-sub">Gestiona tus proveedores</p>
      </div>

      <div className="card">
        <div className="card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h3 className="card-title">Lista de proveedores</h3>
          <div style={{ display: "flex", gap: "10px" }}>
            <input
              className="search-input"
              placeholder="Buscar proveedor..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
            <button className="btn-primary" onClick={abrirNuevo}>+ Nuevo proveedor</button>
          </div>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>RFC</th>
              <th>Teléfono</th>
              <th>Email</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filtrados.map((p) => (
              <tr key={p.id}>
                <td>{p.nombre}</td>
                <td><code>{p.rfc || "—"}</code></td>
                <td>{p.telefono || "—"}</td>
                <td>{p.email || "—"}</td>
                <td>
                  <span className={p.activo ? "badge badge-success" : "badge badge-danger"}>
                    {p.activo ? "Activo" : "Inactivo"}
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
            {filtrados.length === 0 && (
              <tr><td colSpan={6} style={{ textAlign: "center", color: "#64748b", padding: "32px" }}>
                No hay proveedores
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <div className="modal-overlay" onClick={() => setModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editando ? "Editar proveedor" : "Nuevo proveedor"}</h3>
              <button className="modal-close" onClick={() => setModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-field">
                  <label>Nombre *</label>
                  <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} placeholder="Nombre del proveedor" />
                </div>
                <div className="form-field">
                  <label>RFC</label>
                  <input value={form.rfc} onChange={(e) => setForm({ ...form, rfc: e.target.value })} placeholder="RFC del proveedor" />
                </div>
                <div className="form-field">
                  <label>Teléfono</label>
                  <input value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} placeholder="55 1234 5678" />
                </div>
                <div className="form-field">
                  <label>Email</label>
                  <input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="contacto@proveedor.com" />
                </div>
                <div className="form-field" style={{ gridColumn: "span 2" }}>
                  <label>Dirección</label>
                  <input value={form.direccion} onChange={(e) => setForm({ ...form, direccion: e.target.value })} placeholder="Calle, colonia, ciudad" />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
              <button className="btn-primary" onClick={guardar}>
                {editando ? "Guardar cambios" : "Crear proveedor"}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}