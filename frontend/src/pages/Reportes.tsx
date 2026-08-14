import { useEffect, useRef, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";

interface ProductoVendido {
  nombreProducto: string;
  cantidadVendida: number;
  totalGenerado: number;
}

interface VentaDiaria {
  fecha: string;
  cantidadVentas: number;
  totalVentas: number;
}

interface Reporte {
  totalVentas: number;
  cantidadVentas: number;
  promedioVenta: number;
  productosMasVendidos: ProductoVendido[];
  ventasPorDia: VentaDiaria[];
}

export default function Reportes() {
  const [reporte, setReporte] = useState<Reporte | null>(null);
  const [loading, setLoading] = useState(false);
  const reporteRef = useRef<HTMLDivElement>(null);

  const [inicio, setInicio] = useState(() => {
    const d = new Date();
    d.setDate(1);
    return d.toISOString().split("T")[0];
  });
  const [fin, setFin] = useState(() => new Date().toISOString().split("T")[0]);

  const cargar = async () => {
    setLoading(true);
    try {
      const res = await api.get("/ventas/reporte", {
        params: { inicio: `${inicio}T00:00:00`, fin: `${fin}T23:59:59` },
      });
      setReporte(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { cargar(); }, []);

  const imprimirReporte = () => window.print();

  const exportarCSV = () => {
    if (!reporte) return;
    const filas = [
      ["Fecha", "Ventas", "Total"],
      ...reporte.ventasPorDia.map((v) => [v.fecha, v.cantidadVentas, v.totalVentas.toFixed(2)]),
      [],
      ["Resumen"],
      ["Total ventas", reporte.totalVentas.toFixed(2)],
      ["Cantidad ventas", reporte.cantidadVentas],
      ["Promedio", reporte.promedioVenta.toFixed(2)],
    ];
    const csv = filas.map((r) => r.join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `reporte_${inicio}_${fin}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const periodoLabel = () => {
    const i = new Date(inicio + "T00:00:00").toLocaleDateString("es-MX", { day: "2-digit", month: "long", year: "numeric" });
    const f = new Date(fin + "T00:00:00").toLocaleDateString("es-MX", { day: "2-digit", month: "long", year: "numeric" });
    return `${i} — ${f}`;
  };

  return (
    <Layout>
      <div className="page-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <h1 className="page-title">Reportes</h1>
          <p className="page-sub">Análisis de ventas e inventario</p>
        </div>
        {reporte && reporte.cantidadVentas > 0 && (
          <div style={{ display: "flex", gap: "10px" }} className="no-print">
            <button className="btn-secondary" onClick={exportarCSV}>📥 Exportar CSV</button>
            <button className="btn-primary" onClick={imprimirReporte}>🖨️ Imprimir</button>
          </div>
        )}
      </div>

      {/* FILTROS */}
      <div className="card no-print" style={{ marginBottom: "20px" }}>
        <div className="card-header">
          <h3 className="card-title">Período</h3>
        </div>
        <div style={{ padding: "16px 20px", display: "flex", gap: "16px", alignItems: "flex-end", flexWrap: "wrap" }}>
          <div className="form-field">
            <label>Fecha inicio</label>
            <input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
          </div>
          <div className="form-field">
            <label>Fecha fin</label>
            <input type="date" value={fin} onChange={(e) => setFin(e.target.value)} />
          </div>
          <button className="btn-primary" onClick={cargar} disabled={loading}>
            {loading ? "Cargando..." : "Generar reporte"}
          </button>
        </div>
      </div>

      {/* CONTENIDO IMPRIMIBLE */}
      <div ref={reporteRef}>

        {/* ENCABEZADO SOLO EN IMPRESIÓN */}
        <div className="print-only reporte-print-header">
          <h2>Reporte de ventas</h2>
          <p>{periodoLabel()}</p>
        </div>

        {loading && (
          <div style={{ textAlign: "center", padding: "40px", color: "#64748b" }}>
            <div className="spinner" style={{ margin: "0 auto 12px" }} />
            Generando reporte...
          </div>
        )}

        {reporte && !loading && (
          <>
            <div className="stats-grid" style={{ marginBottom: "20px" }}>
              <div className="stat-card">
                <span className="stat-icon">💰</span>
                <div>
                  <p className="stat-label">Total ventas</p>
                  <h2 className="stat-value">${(reporte.totalVentas ?? 0).toFixed(2)}</h2>
                </div>
              </div>
              <div className="stat-card">
                <span className="stat-icon">🧾</span>
                <div>
                  <p className="stat-label">Número de ventas</p>
                  <h2 className="stat-value">{reporte.cantidadVentas ?? 0}</h2>
                </div>
              </div>
              <div className="stat-card">
                <span className="stat-icon">📊</span>
                <div>
                  <p className="stat-label">Promedio por venta</p>
                  <h2 className="stat-value">${(reporte.promedioVenta ?? 0).toFixed(2)}</h2>
                </div>
              </div>
            </div>

            {reporte.ventasPorDia?.length > 0 && (
              <div className="card" style={{ marginBottom: "20px" }}>
                <div className="card-header">
                  <h3 className="card-title">Ventas por día</h3>
                </div>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Número de ventas</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.ventasPorDia.map((v) => (
                      <tr key={v.fecha}>
                        <td>{new Date(v.fecha + "T00:00:00").toLocaleDateString("es-MX", { weekday: "long", day: "2-digit", month: "long" })}</td>
                        <td>{v.cantidadVentas}</td>
                        <td><strong>${v.totalVentas?.toFixed(2)}</strong></td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr style={{ background: "#f8fafc" }}>
                      <td><strong>Total</strong></td>
                      <td><strong>{reporte.cantidadVentas}</strong></td>
                      <td><strong>${reporte.totalVentas?.toFixed(2)}</strong></td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}

            {reporte.productosMasVendidos?.length > 0 && (
              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Productos más vendidos</h3>
                </div>
                <table className="table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Producto</th>
                      <th>Cantidad vendida</th>
                      <th>Total generado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.productosMasVendidos.map((p, i) => (
                      <tr key={p.nombreProducto}>
                        <td>
                          <span className={`badge ${i === 0 ? "badge-danger" : i === 1 ? "badge-warning" : "badge-success"}`}>
                            {i + 1}
                          </span>
                        </td>
                        <td>{p.nombreProducto}</td>
                        <td>{p.cantidadVendida}</td>
                        <td><strong>${p.totalGenerado?.toFixed(2)}</strong></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {reporte.cantidadVentas === 0 && (
              <div className="card">
                <div style={{ padding: "40px", textAlign: "center", color: "#64748b" }}>
                  No hay ventas en el período seleccionado
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  );
}