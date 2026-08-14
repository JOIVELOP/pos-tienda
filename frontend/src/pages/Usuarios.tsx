    import { useEffect, useState } from "react";
    import Layout from "../layout/Layout";
    import api from "../api/axios";

    interface Usuario {
    id: number;
    nombre: string;
    apellido: string;
    username: string;
    password: string;
    rol: string;
    activo: boolean;
    }

    const EMPTY: Usuario = {
    id: 0,
    nombre: "",
    apellido: "",
    username: "",
    password: "",
    rol: "EMPLEADO",
    activo: true,
    };

    export default function Usuarios() {

    const [usuarios, setUsuarios] = useState<Usuario[]>([]);
    const [modal, setModal] = useState(false);
    const [form, setForm] = useState<Usuario>(EMPTY);
    const [editando, setEditando] = useState(false);

    const [confirmar, setConfirmar] = useState(false);
    const [confirmarEliminar, setConfirmarEliminar] = useState<number | null>(null);
    const [loading, setLoading] = useState(false);

    // ✅ TOAST
    const [toast, setToast] = useState<string | null>(null);

    const [errors, setErrors] = useState({
        nombre: "",
        apellido: "",
        username: "",
        password: "",
    });

    const showToast = (message: string) => {
        setToast(message);
        setTimeout(() => setToast(null), 2000);
    };

    const cargar = () =>
        api.get("/usuarios").then((r) => setUsuarios(r.data));

    useEffect(() => {
        cargar();
    }, []);

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

    const abrirNuevo = () => {
        setForm(EMPTY);
        setEditando(false);
        setModal(true);
    };

    const abrirEditar = (u: Usuario) => {
        setForm({ ...u, password: "" });
        setEditando(true);
        setModal(true);
    };

    const validate = () => {
        const newErrors = {
        nombre: "",
        apellido: "",
        username: "",
        password: "",
        };

        let valid = true;

        if (!form.nombre) {
        newErrors.nombre = "Nombre requerido";
        valid = false;
        }

        if (!form.apellido) {
        newErrors.apellido = "Apellido requerido";
        valid = false;
        }

        if (!form.username) {
        newErrors.username = "Username requerido";
        valid = false;
        }

        if (!form.password) {
        newErrors.password = "Contraseña requerida";
        valid = false;
        }

        setErrors(newErrors);
        return valid;
    };

    const guardar = () => {
        if (!validate()) return;
        setConfirmar(true);
    };

    const guardarDatos = async () => {
        if (editando) {
        await api.put(`/usuarios/${form.id}`, form);
        } else {
        await api.post("/usuarios", form);
        }
    };

    const eliminar = (id: number) => {
        setConfirmarEliminar(id);
    };

    const eliminarDatos = async () => {
        if (!confirmarEliminar) return;
        await api.delete(`/usuarios/${confirmarEliminar}`);
    };

    const rolColor: Record<string, string> = {
        ADMINISTRADOR: "badge-danger",
        EMPLEADO: "badge-success",
        ENCARGADO: "badge-warning",
    };

    return (
        <Layout>

        <div className="page-header">
            <h1 className="page-title">Usuarios</h1>
            <p className="page-sub">Gestiona los accesos al sistema</p>
        </div>

        <div className="card">
            <div className="card-header" style={{ display: "flex", justifyContent: "space-between" }}>
            <h3 className="card-title">Lista de usuarios</h3>
            <button className="btn-primary" onClick={abrirNuevo}>
                + Nuevo usuario
            </button>
            </div>

            <table className="table">
            <thead>
                <tr>
                <th>Nombre</th>
                <th>Apellido</th>
                <th>Username</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Acciones</th>
                </tr>
            </thead>

            <tbody>
                {usuarios.map((u) => (
                <tr key={u.id}>
                    <td>{u.nombre}</td>
                    <td>{u.apellido}</td>
                    <td><code>{u.username}</code></td>

                    <td>
                    <span className={`badge ${rolColor[u.rol]}`}>
                        {u.rol === "ADMINISTRADOR" && "🛡️ "}
                        {u.rol === "EMPLEADO" && "👤 "}
                        {u.rol === "ENCARGADO" && "📦 "}
                        {u.rol}
                    </span>
                    </td>

                    <td>
                    <span className={u.activo ? "badge badge-success" : "badge badge-danger"}>
                        {u.activo ? "Activo" : "Inactivo"}
                    </span>
                    </td>

                    <td>
                    <div style={{ display: "flex", gap: 6 }}>
                        <button className="btn-sm" onClick={() => abrirEditar(u)}>
                        Editar
                        </button>

                        <button className="btn-sm btn-danger" onClick={() => eliminar(u.id)}>
                        Eliminar
                        </button>
                    </div>
                    </td>
                </tr>
                ))}
            </tbody>
            </table>
        </div>

        {/* MODAL FORM */}
        {modal && (
            <div className="modal-overlay" onClick={() => setModal(false)}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>

                <div className="modal-header">
                <h3>{editando ? "Editar usuario" : "Nuevo usuario"}</h3>
                <button className="modal-close" onClick={() => setModal(false)}>✕</button>
                </div>

                <div className="modal-body">
                <div className="form-grid">

                    <div className="form-field">
                    <label>Nombre *</label>
                    <input value={form.nombre}
                        onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                        className={errors.nombre ? "input-error" : ""} />
                    {errors.nombre && <span className="error">{errors.nombre}</span>}
                    </div>

                    <div className="form-field">
                    <label>Apellido *</label>
                    <input value={form.apellido}
                        onChange={(e) => setForm({ ...form, apellido: e.target.value })}
                        className={errors.apellido ? "input-error" : ""} />
                    {errors.apellido && <span className="error">{errors.apellido}</span>}
                    </div>

                    <div className="form-field">
                    <label>Username *</label>
                    <input value={form.username}
                        onChange={(e) => setForm({ ...form, username: e.target.value })}
                        className={errors.username ? "input-error" : ""} />
                    {errors.username && <span className="error">{errors.username}</span>}
                    </div>

                    <div className="form-field">
                    <label>Contraseña *</label>
                    <input type="password"
                        value={form.password}
                        onChange={(e) => setForm({ ...form, password: e.target.value })}
                        className={errors.password ? "input-error" : ""} />
                    {errors.password && <span className="error">{errors.password}</span>}
                    </div>

                    <div className="form-field">
                    <label>Rol</label>
                    <select value={form.rol}
                        onChange={(e) => setForm({ ...form, rol: e.target.value })}>
                        <option>ADMINISTRADOR</option>
                        <option>EMPLEADO</option>
                        <option>ENCARGADO</option>
                    </select>
                    </div>

                </div>
                </div>

                <div className="modal-footer">
                <button className="btn-secondary" onClick={() => setModal(false)}>
                    Cancelar
                </button>

                <button className="btn-primary" onClick={guardar}>
                    Guardar
                </button>
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
    
                <h3>
                {editando
                    ? "¿Deseas modificar los datos?"
                    : "¿Deseas crear este usuario?"}
                </h3>


                <div className="modal-footer">
                <button
                    className="btn-secondary"
                    onClick={() => setConfirmar(false)}
                >
                    Cancelar
                </button>

                <button
                    className="btn-primary"
                    onClick={async () => {
                    setLoading(true);

                    await new Promise(r => setTimeout(r, 1200));
                    await guardarDatos();

                    showToast(
                        editando
                        ? "Usuario actualizado correctamente"
                        : "Usuario agregado correctamente"
                    );

                    setLoading(false);
                    setConfirmar(false);
                    setModal(false);
                    cargar();
                    }}
                >
                    Guardar
                </button>
                </div>
                </>
                ) : (
                <div className="loading-box">
                    <div className="spinner"></div>
                    <p>Guardando cambios...</p>
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
                <h3>Eliminar usuario</h3>
                <button
                className="modal-close"
                onClick={() => setConfirmarEliminar(null)}
                >
                ✕
                </button>
            </div>


        <div>
            <div className="form-field text-center">
                
                <label>¿Deseas eliminar este usuario?</label>
           
                <label className="danger-text">
                Esta acción no se puede deshacer
                </label>
                
            </div>
            </div>

            <div className="modal-footer">
                <button
                className="btn-secondary"
                onClick={() => setConfirmarEliminar(null)}
                >
                Cancelar
                </button>

                <button
                className="btn-primary"
                style={{ background: "#ef4444" }}
                onClick={async () => {
                    setLoading(true);
                    await new Promise(r => setTimeout(r, 1000));

                    await eliminarDatos();

                    showToast("🗑️ Usuario eliminado");

                    setLoading(false);
                    setConfirmarEliminar(null);
                    cargar();
                }}
                >
                🗑️ Eliminar
                </button>
            </div>
            </>
        ) : (
            <div className="loading-box">
            <div className="spinner"></div>
            <p>Eliminando usuario...</p>
            </div>
        )}

        </div>
    </div>
    )}

        {/* TOAST */}
        {toast && (
            <div className="toast success">
            {toast}
            </div>
        )}

        </Layout>
    );
    }