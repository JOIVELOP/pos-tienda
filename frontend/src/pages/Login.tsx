import { useState } from "react";
import api from "../api/axios";
import { LoginRequest, LoginResponse } from "../types/auth";
import { useNavigate } from "react-router-dom";
import tienda from "../assets/tienda.avif";
import { leerDiario, pendientes } from "../offline/diario";
import { isAxiosError } from "axios";

export default function Login() {
  const navigate = useNavigate();
  const [form, setForm] = useState<LoginRequest>({ username: "", password: "" });
  const [errors, setErrors] = useState({ username: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const validate = () => {
    const newErrors = { username: "", password: "" };
    let valid = true;
    if (!form.username) { newErrors.username = "Usuario requerido"; valid = false; }
    if (!form.password) { newErrors.password = "Contraseña requerida"; valid = false; }
    setErrors(newErrors);
    return valid;
  };

  const handleLogin = async () => {
    if (!validate()) return;
    setLoading(true);
    try {
      const res = await api.post<LoginResponse>("/auth/login", form);
      const diario = await leerDiario();
      if (diario && (!diario.sesion.cerrada || pendientes(diario).length)
          && diario.sesion.usuario !== res.data.id) {
        setErrors({ username: "Este navegador tiene un turno de otro empleado. Sincroniza su cierre antes de cambiar de usuario.", password: "" });
        setLoading(false);
        return;
      }
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("user", JSON.stringify({
        id: res.data.id,
        nombre: res.data.nombre,
        rol: res.data.rol 
      }));

      setSuccess(true);
      setTimeout(() => navigate("/"), 1800);
    } catch (error) {
      const mensaje = isAxiosError(error)
        ? (error.response?.status === 401 || error.response?.status === 403
          ? "Usuario o contraseña incorrectos" : "No se pudo contactar al servidor de autenticación.")
        : error instanceof Error ? error.message : "No se pudo verificar el diario local.";
      setErrors({ username: mensaje, password: "" });
      setLoading(false);
    }
  };


  const handleEnter = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleLogin();
  };

  if (success) {
    return (
      <div className="login-splash">
        <div className="login-splash-inner">
          <div className="splash-logo">🏪</div>
          <h2 className="splash-title">POS Tienda</h2>
          <div className="splash-bar">
            <div className="splash-bar-fill" />
          </div>
          <p className="splash-text">Cargando tu sesión...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <div className="login-left">
        <img src={tienda} alt="Tienda" className="login-bg-img" />
        <div className="login-overlay">
          <div className="login-brand">
            <div className="login-logo">POS</div>
            <p className="login-tagline">Sistema de punto de venta</p>
          </div>
          <div className="login-badge">
            <span className="login-dot-green" />
            Sistema activo
          </div>
          <div className="login-dots">
            <span className="ldot active" />
            <span className="ldot" />
            <span className="ldot" />
          </div>
        </div>
      </div>

      <div className="login-right">
        <div className="login-box">
          <h2>Bienvenido</h2>
          <p className="login-sub">Inicia sesión para continuar</p>

          <div className="login-field">
            <label>Usuario</label>
            <input
              placeholder="Ingresa tu usuario"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              onKeyDown={handleEnter}
              className={errors.username ? "input-error" : ""}
            />
            {errors.username && <span className="error">{errors.username}</span>}
          </div>

          <div className="login-field">
            <label>Contraseña</label>
            <input
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              onKeyDown={handleEnter}
              className={errors.password ? "input-error" : ""}
            />
            {errors.password && <span className="error">{errors.password}</span>}
          </div>

          <button
            onClick={handleLogin}
            disabled={loading || !form.username || !form.password}
          >
            {loading ? "Verificando..." : "Iniciar sesión"}
          </button>
        </div>
      </div>
    </div>
  );
}