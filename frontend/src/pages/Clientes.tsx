import { useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface Cliente {
  id: number;
  nombre: string;
  telefono: string;
  email: string;
  rfc: string;
  razonSocial: string;
  activo: boolean;
}

const EMPTY: Cliente = {
  id: 0, nombre: "", telefono: "", email: "",
  rfc: "", razonSocial: "", activo: true,
};

export default function Clientes() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState<Cliente>(EMPTY);
  const [editando, setEditando] = useState(false);

  const cargar = () => api.get("/clientes").then((r) => setClientes(r.data));
  useEffect(() => { cargar(); }, []);

  const filtrados = clientes.filter((c) =>
    c.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    c.rfc?.toLowerCase().includes(busqueda.toLowerCase())
  );

  const abrirNuevo = () => { setForm(EMPTY); setEditando(false); setModal(true); };
  const abrirEditar = (c: Cliente) => { setForm(c); setEditando(true); setModal(true); };

  const guardar = async () => {
    if (!form.nombre) return;
    if (editando) {
      await api.put(`/clientes/${form.id}`, form);
    } else {
      await api.post("/clientes", form);
    }
    setModal(false);
    cargar();
  };

  const eliminar = async (id: number) => {
    if (!confirm("¿Eliminar cliente?")) return;
    await api.delete(`/clientes/${id}`);
    cargar();
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Clientes</h1>
        <p className="page-sub">Gestiona tu cartera de clientes</p>
      </div>

      <div className="card">
        <div className="card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h3 className="card-title">Lista de clientes</h3>
          <div style={{ display: "flex", gap: "10px" }}>
            <input
              className="search-input"
              placeholder="Buscar cliente..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
            <button className="btn-primary" onClick={abrirNuevo}>+ Nuevo cliente</button>
          </div>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Teléfono</th>
              <th>Email</th>
              <th>RFC</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filtrados.map((c) => (
              <tr key={c.id}>
                <td>{c.nombre}</td>
                <td>{c.telefono || "—"}</td>
                <td>{c.email || "—"}</td>
                <td><code>{c.rfc || "—"}</code></td>
                <td>
                  <span className={c.activo ? "badge badge-success" : "badge badge-danger"}>
                    {c.activo ? "Activo" : "Inactivo"}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: "6px" }}>
                    <button className="btn-sm" onClick={() => abrirEditar(c)}>Editar</button>
                    <button className="btn-sm btn-danger" onClick={() => eliminar(c.id)}>Eliminar</button>
                  </div>
                </td>
              </tr>
            ))}
            {filtrados.length === 0 && (
              <tr><td colSpan={6} style={{ textAlign: "center", color: "#64748b", padding: "32px" }}>
                No hay clientes
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <div className="modal-overlay" onClick={() => setModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editando ? "Editar cliente" : "Nuevo cliente"}</h3>
              <button className="modal-close" onClick={() => setModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-field">
                  <label>Nombre *</label>
                  <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} placeholder="Nombre completo" />
                </div>
                <div className="form-field">
                  <label>Teléfono</label>
                  <input value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} placeholder="55 1234 5678" />
                </div>
                <div className="form-field">
                  <label>Email</label>
                  <input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="correo@ejemplo.com" />
                </div>
                <div className="form-field">
                  <label>RFC</label>
                  <input value={form.rfc} onChange={(e) => setForm({ ...form, rfc: e.target.value })} placeholder="XAXX010101000" />
                </div>
                <div className="form-field">
                  <label>Razón social</label>
                  <input value={form.razonSocial} onChange={(e) => setForm({ ...form, razonSocial: e.target.value })} placeholder="Empresa S.A. de C.V." />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
              <button className="btn-primary" onClick={guardar}>
                {editando ? "Guardar cambios" : "Crear cliente"}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}