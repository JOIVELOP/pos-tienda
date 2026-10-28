export default function Header() {
  const user = JSON.parse(localStorage.getItem("user") || "{}");

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  const getInitials = (nombre: string) => {
    return nombre?.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2) || "U";
  };

  return (
    <div className="header">
      <div className="header-title">
        Panel de control
      </div>

      <div className="header-right">
        <div className="header-avatar">
          {getInitials(user?.nombre)}
        </div>
        <div className="header-user">
          <span className="header-nombre">{user?.nombre}</span>
          <span className="header-rol">{user?.rol}</span>
        </div>
        <button className="header-logout" onClick={logout}>
          Salir
        </button>
      </div>
    </div>
  );
}