import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Productos from "./pages/Productos";
import Ventas from "./pages/Ventas";
import Clientes from "./pages/Clientes";
import Proveedores from "./pages/Proveedores";
import Categorias from "./pages/Categorias";
import Promociones from "./pages/Promociones";
import Reportes from "./pages/Reportes";
import Usuarios from "./pages/Usuarios";
import Caja from "./pages/Caja";
import Compras from "./pages/Compras";
import Devoluciones from "./pages/Devoluciones";
import ProtectedRoute from "./auth/ProtectedRoute";
import RoleRoute from "./auth/RoleRoute";

const ADMIN_ENCARGADO = ["ADMINISTRADOR", "ENCARGADO"];
const SOLO_ADMIN = ["ADMINISTRADOR"];

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/login" element={<Login />} />

        <Route path="/" element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        } />

        <Route path="/ventas" element={
          <ProtectedRoute>
            <Ventas />
          </ProtectedRoute>
        } />

        <Route path="/caja" element={
          <ProtectedRoute>
            <Caja />
          </ProtectedRoute>
        } />

        <Route path="/compras" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Compras />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/devoluciones" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Devoluciones />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/productos" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Productos />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/categorias" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Categorias />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/proveedores" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Proveedores />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/promociones" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Promociones />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/clientes" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Clientes />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/reportes" element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ENCARGADO}>
              <Reportes />
            </RoleRoute>
          </ProtectedRoute>
        } />

        <Route path="/usuarios" element={
          <ProtectedRoute>
            <RoleRoute roles={SOLO_ADMIN}>
              <Usuarios />
            </RoleRoute>
          </ProtectedRoute>
        } />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
