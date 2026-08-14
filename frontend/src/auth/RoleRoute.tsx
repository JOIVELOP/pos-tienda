import { ReactNode } from "react";
import { Navigate } from "react-router-dom";

interface Props {
  children: ReactNode;
  roles: string[];
}

export default function RoleRoute({ children, roles }: Props) {
  const user = JSON.parse(localStorage.getItem("user") || "{}");

  if (!user || !roles.includes(user.rol)) {
    return <Navigate to="/ventas" replace />;
  }

  return <>{children}</>;
}