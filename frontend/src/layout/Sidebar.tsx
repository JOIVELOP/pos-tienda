import { useNavigate, useLocation } from "react-router-dom";

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const rol = user?.rol;

  const menuItems = [
    { icon: "🏠", label: "Dashboard",   path: "/",           roles: ["ADMINISTRADOR", "ENCARGADO", "EMPLEADO"] },
    { icon: "💰", label: "Ventas",      path: "/ventas",     roles: ["ADMINISTRADOR", "ENCARGADO", "EMPLEADO"] },
    { icon: "🧾", label: "Caja",        path: "/caja",       roles: ["ADMINISTRADOR", "ENCARGADO", "EMPLEADO"] },
    { icon: "🛒", label: "Compras",     path: "/compras",    roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "↩️", label: "Devoluciones",path: "/devoluciones",roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "📦", label: "Productos",   path: "/productos",  roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "🏷️", label: "Categorías",  path: "/categorias", roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "🏷️", label: "Promociones", path: "/promociones",roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "🚚", label: "Proveedores", path: "/proveedores",roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "👥", label: "Clientes",    path: "/clientes",   roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "📊", label: "Reportes",    path: "/reportes",   roles: ["ADMINISTRADOR", "ENCARGADO"] },
    { icon: "👤", label: "Usuarios",    path: "/usuarios",   roles: ["ADMINISTRADOR"] },
  ].filter(item => item.roles.includes(rol));

  return (
    <div className="sidebar">
      <div className="sidebar-logo">
        <span>🏪</span>
        <span>Mi tiendita</span>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item) => {
          const active = location.pathname === item.path;

          return (
            <div
              key={item.path}
              className={`sidebar-item ${active ? "active" : ""}`}
              onClick={() => navigate(item.path)}
            >
              <span className="sidebar-item-icon">{item.icon}</span>
              <span className="sidebar-item-label">{item.label}</span>
            </div>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <span className="sidebar-version">v1.0.0</span>
      </div>
    </div>
  );
}