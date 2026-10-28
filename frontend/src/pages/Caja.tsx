import { FormEvent, useEffect, useState } from "react";
import Layout from "../layout/Layout";
import api from "../api/axios";
import { leerDiario, propio, pendientes, registrar, sincronizar, Tipo } from "../offline/diario";

interface Movimiento {
  id: number;
  tipo: "ENTRADA" | "SALIDA";
  concepto: string;
  descripcion: string;
  monto: number;
  fecha: string;
  usuario: string;
}

interface Turno {
  id: number;
  folio: string;
  estado: string;
  apertura: string;
  cierre?: string;
  usuarioApertura: string;
  fondoInicial: number;
  ventasEfectivo: number;
  entradas: number;
  salidas: number;
  efectivoEsperado: number;
  efectivoContado?: number;
  diferencia?: number;
  movimientos: Movimiento[];
}

const money = (value: number) =>
  new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(value);

function errorText(error: unknown) {
  const response = (error as { response?: { data?: { error?: string } } }).response;
  return response?.data?.error || (error instanceof Error ? error.message : "No fue posible completar la operación.");
}

export default function Caja() {
  const [turno, setTurno] = useState<Turno | null>(null);
  const [historial, setHistorial] = useState<Turno[]>([]);
  const [fondoInicial, setFondoInicial] = useState("0");
  const [efectivoContado, setEfectivoContado] = useState("");
  const [tipo, setTipo] = useState<"ENTRADA" | "SALIDA">("SALIDA");
  const [concepto, setConcepto] = useState("GASTO");
  const [descripcion, setDescripcion] = useState("");
  const [monto, setMonto] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const [modoDiario, setModoDiario] = useState(false);

  const cargar = async () => {
    try {
      const diario = await leerDiario();
      if (diario && (!diario.sesion.cerrada || pendientes(diario).length)) {
        propio(diario);
        setModoDiario(true);
        setTurno({
          id: diario.sesion.turno, folio: diario.sesion.folioTurno,
          estado: diario.sesion.cerrada ? "CIERRE_PENDIENTE" : "ABIERTO",
          apertura: diario.registros[0]?.operacion.captura || new Date().toISOString(),
          usuarioApertura: "Empleado actual", fondoInicial: 0, ventasEfectivo: 0,
          entradas: 0, salidas: 0, efectivoEsperado: diario.sesion.efectivo,
          movimientos: diario.registros.filter(r => ["APORTACION", "GASTO", "RETIRO"].includes(r.operacion.tipo))
            .map(r => ({
              id: r.operacion.secuencia, tipo: r.operacion.tipo === "APORTACION" ? "ENTRADA" : "SALIDA",
              concepto: r.operacion.tipo, descripcion: r.operacion.descripcion,
              monto: r.operacion.monto, fecha: r.operacion.captura, usuario: "Empleado actual",
            })),
        });
        setHistorial([]);
        return;
      }
      setModoDiario(false);
      const actual = await api.get("/caja/actual");
      setTurno(actual.status === 204 ? null : actual.data);
      const previous = await api.get("/caja/historial");
      setHistorial(previous.data);
    } catch (e) {
      setError(errorText(e));
    }
  };

  useEffect(() => {
    void cargar();
    const actualizar = () => { void cargar(); };
    window.addEventListener("diario", actualizar);
    return () => window.removeEventListener("diario", actualizar);
  }, []);

  const abrir = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    setCargando(true);
    try {
      await api.post("/caja/abrir", { fondoInicial: Number(fondoInicial) });
      await cargar();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  const registrarMovimiento = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    setCargando(true);
    try {
      if (modoDiario) {
        if (!["APORTACION", "GASTO", "RETIRO"].includes(concepto)) {
          throw new Error("En caja preparada usa aportación, gasto o retiro.");
        }
        await registrar(concepto as Tipo, Number(monto), descripcion);
        setDescripcion(""); setMonto("");
        void sincronizar().catch(e => setError(e instanceof Error ? e.message : String(e)));
        await cargar();
        return;
      }
      await api.post("/caja/movimientos", {
        tipo, concepto, descripcion, monto: Number(monto),
      });
      setDescripcion("");
      setMonto("");
      await cargar();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  const cerrar = async (event: FormEvent) => {
    event.preventDefault();
    if (!turno) return;
    setError("");
    setCargando(true);
    try {
      if (modoDiario) {
        await registrar("CIERRE", Number(efectivoContado), "Cierre provisional");
        setEfectivoContado("");
        void sincronizar().catch(e => setError(e instanceof Error ? e.message : String(e)));
        await cargar();
        return;
      }
      await api.post(`/caja/${turno.id}/cerrar`, { efectivoContado: Number(efectivoContado) });
      setEfectivoContado("");
      await cargar();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setCargando(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1 className="page-title">Caja por turno</h1>
        <p className="page-sub">Apertura, movimientos y conciliación de efectivo</p>
      </div>

      {error && <div className="card" role="alert" style={{ color: "#b91c1c", marginBottom: 16 }}>{error}</div>}

      {!turno ? (
        <div className="card">
          <div className="card-header"><h3 className="card-title">Abrir turno</h3></div>
          <form onSubmit={abrir} className="form-grid">
            <div className="form-field">
              <label>Fondo inicial en efectivo</label>
              <input type="number" min="0" step="0.01" value={fondoInicial}
                onChange={(e) => setFondoInicial(e.target.value)} required />
            </div>
            <div className="form-field" style={{ alignSelf: "end" }}>
              <button className="btn-primary" disabled={cargando}>Abrir caja</button>
            </div>
          </form>
        </div>
      ) : (
        <>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <h3 className="card-title">Turno {turno.folio} · {turno.estado}</h3>
              <span>Desde {new Date(turno.apertura).toLocaleString("es-MX")}</span>
            </div>
            {modoDiario ? <p>Efectivo local esperado: {money(turno.efectivoEsperado)}.
              El desglose definitivo estará disponible tras sincronizar el cierre.</p> : <div className="form-grid">
              <div><strong>Fondo inicial</strong><p>{money(turno.fondoInicial)}</p></div>
              <div><strong>Ventas en efectivo</strong><p>{money(turno.ventasEfectivo)}</p></div>
              <div><strong>Entradas manuales</strong><p>{money(turno.entradas)}</p></div>
              <div><strong>Salidas y reembolsos</strong><p>{money(turno.salidas)}</p></div>
              <div><strong>Efectivo esperado</strong><p>{money(turno.efectivoEsperado)}</p></div>
            </div>}
          </div>

          <div className="form-grid" style={{ marginBottom: 16 }}>
            <div className="card">
              <div className="card-header"><h3 className="card-title">Registrar movimiento</h3></div>
              <form onSubmit={registrarMovimiento}>
                <div className="form-field">
                  <label>Tipo</label>
                  <select value={tipo} onChange={(e) => {
                    const next = e.target.value as "ENTRADA" | "SALIDA";
                    setTipo(next);
                    setConcepto(next === "ENTRADA" ? "APORTACION" : "GASTO");
                  }}>
                    <option value="SALIDA">Salida</option>
                    <option value="ENTRADA">Entrada</option>
                  </select>
                </div>
                <div className="form-field">
                  <label>Concepto</label>
                  <select value={concepto} onChange={(e) => setConcepto(e.target.value)}>
                    {tipo === "ENTRADA" ? <option value="APORTACION">Aportación</option> :
                      <><option value="GASTO">Gasto</option><option value="RETIRO">Retiro</option></>}
                    {!modoDiario && <option value="AJUSTE">Ajuste</option>}
                  </select>
                </div>
                <div className="form-field">
                  <label>Descripción</label>
                  <input value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                    maxLength={255} required />
                </div>
                <div className="form-field">
                  <label>Monto</label>
                  <input type="number" min="0.01" step="0.01" value={monto}
                    onChange={(e) => setMonto(e.target.value)} required />
                </div>
                <button className="btn-primary" disabled={cargando || turno.estado !== "ABIERTO"}>Guardar movimiento</button>
              </form>
            </div>

            <div className="card">
              <div className="card-header"><h3 className="card-title">Cerrar turno</h3></div>
              <p>Cuenta físicamente el efectivo del cajón antes de cerrar.</p>
              <form onSubmit={cerrar}>
                <div className="form-field">
                  <label>Efectivo contado</label>
                  <input type="number" min="0" step="0.01" value={efectivoContado}
                    onChange={(e) => setEfectivoContado(e.target.value)} required />
                </div>
                <button className="btn-primary" disabled={cargando || turno.estado !== "ABIERTO"}>
                  {modoDiario ? "Guardar cierre provisional" : "Cerrar y conciliar"}</button>
              </form>
            </div>
          </div>

          <div className="card">
            <div className="card-header"><h3 className="card-title">Movimientos del turno</h3></div>
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead><tr><th>Hora</th><th>Tipo</th><th>Concepto</th><th>Descripción</th><th>Usuario</th><th>Monto</th></tr></thead>
                <tbody>
                  {turno.movimientos.map((m) => (
                    <tr key={m.id}><td>{new Date(m.fecha).toLocaleTimeString("es-MX")}</td>
                      <td>{m.tipo}</td><td>{m.concepto}</td><td>{m.descripcion}</td>
                      <td>{m.usuario}</td><td>{money(m.monto)}</td></tr>
                  ))}
                  {turno.movimientos.length === 0 && <tr><td colSpan={6}>Sin movimientos manuales.</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      <div className="card" style={{ marginTop: 16 }}>
        <div className="card-header"><h3 className="card-title">Turnos anteriores</h3></div>
        <div style={{ overflowX: "auto" }}>
          <table className="table">
            <thead><tr><th>Folio</th><th>Empleado</th><th>Apertura</th><th>Cierre</th><th>Esperado</th><th>Contado</th><th>Diferencia</th></tr></thead>
            <tbody>{historial.filter((t) => t.estado === "CERRADO").map((t) => (
              <tr key={t.id}><td>{t.folio}</td><td>{t.usuarioApertura}</td><td>{new Date(t.apertura).toLocaleString("es-MX")}</td>
                <td>{t.cierre ? new Date(t.cierre).toLocaleString("es-MX") : "—"}</td>
                <td>{money(t.efectivoEsperado)}</td><td>{money(t.efectivoContado || 0)}</td>
                <td>{money(t.diferencia || 0)}</td></tr>
            ))}{historial.every((t) => t.estado !== "CERRADO") && (
              <tr><td colSpan={7}>Sin turnos cerrados.</td></tr>
            )}</tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
