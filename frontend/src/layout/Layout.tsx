import { ReactNode } from "react";
import Sidebar from "./Sidebar";
import Header from "./Header";
import PanelOffline from "../offline/PanelOffline";

interface Props {
  children: ReactNode;
}

export default function Layout({ children }: Props) {
  return (
    <div className="layout">
      <Sidebar />
      <div className="layout-main">
        <Header />
        <div className="layout-content">
          <PanelOffline />
          {children}
        </div>
      </div>
    </div>
  );
}