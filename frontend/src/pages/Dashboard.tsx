import { useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

import { useNavigate } from "react-router-dom";
import { ROLES } from "../auth/roles";

interface Producto {
  id: number;
  nombre: string;
  stock: number;
  stockMinimo: number;
}

export default function Dashboard() {
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const [productosBajoStock, setProductosBajoStock] = useState<Producto[]>([]);
  const [totalProductos, setTotalProductos] = useState(0);

  useEffect(() => {
    api.get("/productos").then((r) => setTotalProductos(r.data.length)).catch(() => {});
    api.get("/productos/bajo-stock").then((r) => setProductosBajoStock(r.data)).catch(() => {});
  }, []);
const navigate = useNavigate();
const rol = user?.rol;

const accesos = [
  { icon: "💰", label: "Nueva venta", path: "/ventas", roles: ROLES.TODOS },
  { icon: "📦", label: "Ver productos", path: "/productos", roles: ROLES.ADMIN_ENCARGADO },
  { icon: "📊", label: "Ver reportes", path: "/reportes", roles: ROLES.ADMIN_ENCARGADO },
  { icon: "👥", label: "Ver clientes", path: "/clientes", roles: ROLES.ADMIN_ENCARGADO },
].filter(item => item.roles.includes(rol));

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-sub">Bienvenido, {user?.nombre}</p>
      </div>

      {/* TARJETAS */}
      <div className="stats-grid">
        <div className="stat-card">
          <span className="stat-icon">💰</span>
          <div>
            <p className="stat-label">Ventas hoy</p>
            <h2 className="stat-value">$0.00</h2>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon">📦</span>
          <div>
            <p className="stat-label">Productos</p>
            <h2 className="stat-value">{totalProductos}</h2>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon">🧾</span>
          <div>
            <p className="stat-label">Ventas del mes</p>
            <h2 className="stat-value">$0.00</h2>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon">⚠️</span>
          <div>
            <p className="stat-label">Bajo stock</p>
            <h2 className="stat-value">{productosBajoStock.length}</h2>
          </div>
        </div>
      </div>

      {/* TABLA BAJO STOCK */}
      {productosBajoStock.length > 0 && (
        <div className="card mt-24">
          <div className="card-header">
            <h3 className="card-title">⚠️ Productos bajo stock mínimo</h3>
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Stock actual</th>
                <th>Stock mínimo</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {productosBajoStock.map((p) => (
                <tr key={p.id}>
                  <td>{p.nombre}</td>
                  <td>{p.stock}</td>
                  <td>{p.stockMinimo}</td>
                  <td>
                    <span className="badge badge-danger">Bajo stock</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* ACCESOS RÁPIDOS */}
      <div className="card mt-24">
        <div className="card-header">
          <h3 className="card-title">Accesos rápidos</h3>
        </div>
        <div className="quick-grid">

        {accesos.map((item) => (
            <div
              key={item.path}
              className="quick-item"
              onClick={() => navigate(item.path)}
            >
              <span className="quick-icon">{item.icon}</span>
              <span>{item.label}</span>
            </div>
          ))}

          </div>
        </div>
    </Layout>
  );
}