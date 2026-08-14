  import { useEffect, useState } from "react";
  import Layout from "../layout/Layout";
  import api from "../api/axios";
  import { AxiosError } from "axios";
  import logo from "../assets/bolsa.png";

  interface Producto {
    id: number;
    codigo: string;
    nombre: string;
    precioVenta: number;
    stock: number;
    stockMinimo?: number;
  }

  interface ItemCarrito {
    producto: Producto;
    cantidad: number;
    subtotal: number;
  }

  interface DetalleVenta {
    id: number;
    idProducto: number;
    nombreProducto: string;
    cantidad: number;
    subtotal: number;
    precioUnitario: number;
  }

  interface VentaResponse {
    id: number;
    folio: string;
    fecha: string;
    total: number;
    detalles: DetalleVenta[];
  }

  export default function Ventas() {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    const [clientes, setClientes] = useState<{id: number; nombre: string}[]>([]);
    const [idCliente, setIdCliente] = useState<number | null>(null);
    const [productos, setProductos] = useState<Producto[]>([]);
    const [busqueda, setBusqueda] = useState("");
    const [carrito, setCarrito] = useState<ItemCarrito[]>([]);
    const [metodoPago, setMetodoPago] = useState("EFECTIVO");
    const [efectivo, setEfectivo] = useState(0);
    const [ticketVisible, setTicketVisible] = useState(false);
    const [ultimaVenta, setUltimaVenta] =useState<VentaResponse | null>(null);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [efectivoVenta, setEfectivoVenta] = useState(0);

    // ✅ cargar productos
    useEffect(() => {
      api.get("/productos").then((r) => setProductos(r.data));
    }, []);

        // Carga clientes
    useEffect(() => {
      api.get("/productos").then((r) => setProductos(r.data));
      api.get("/clientes").then((r) => setClientes(r.data));
    }, []);

    
    // ✅ ESC para cerrar modales
    useEffect(() => {
      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === "Escape") {
          setTicketVisible(false);
          setErrorMsg(null);
        }
      };

      window.addEventListener("keydown", handleKeyDown);
      return () =>
        window.removeEventListener("keydown", handleKeyDown);
    }, []);

    const filtrados = productos.filter(
      (p) =>
        p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        p.codigo?.toLowerCase().includes(busqueda.toLowerCase())
    );

    // ✅ agregar producto
    const agregarAlCarrito = (producto: Producto) => {
      setErrorMsg(null);

      setCarrito((prev) => {
        const existe = prev.find(
          (i) => i.producto.id === producto.id
        );

        const cantidadActual = existe ? existe.cantidad : 0;

        if (cantidadActual + 1 > producto.stock) {
          setErrorMsg(
            `Stock insuficiente para: ${producto.nombre}`
          );
          return prev;
        }

        if (existe) {
          return prev.map((i) =>
            i.producto.id === producto.id
              ? {
                  ...i,
                  cantidad: i.cantidad + 1,
                  subtotal:
                    (i.cantidad + 1) *
                    i.producto.precioVenta,
                }
              : i
          );
        }

        return [
          ...prev,
          {
            producto,
            cantidad: 1,
            subtotal: producto.precioVenta,
          },
        ];
      });
    };

    // ✅ cambiar cantidad
    const cambiarCantidad = (
      id: number,
      cantidad: number
    ) => {
      const item = carrito.find(
        (i) => i.producto.id === id
      );
      if (!item) return;

      if (isNaN(cantidad)) return;

      if (cantidad > item.producto.stock) {
        setErrorMsg(
          `Stock insuficiente para: ${item.producto.nombre}`
        );
        cantidad = item.producto.stock;
      }

      if (cantidad < 1) {
        quitarDelCarrito(id);
        return;
      }

      setCarrito((prev) =>
        prev.map((i) =>
          i.producto.id === id
            ? {
                ...i,
                cantidad,
                subtotal:
                  cantidad * i.producto.precioVenta,
              }
            : i
        )
      );
    };

    const quitarDelCarrito = (id: number) => {
      setCarrito((prev) =>
        prev.filter((i) => i.producto.id !== id)
      );
    };

    const total = carrito.reduce(
      (s, i) => s + i.subtotal,
      0
    );
    const cambio = efectivo - total;

    // ✅ PROCESAR VENTA
    const procesarVenta = async () => {
      if (carrito.length === 0) return;

      if (
        metodoPago === "EFECTIVO" &&
        efectivo < total
      ) {
        setErrorMsg(
          "El efectivo recibido es menor al total"
        );
        return;
      }

      const dto = {
        metodoPago,
        idUsuario: user.id,
        subtotal: total,
        total,
        descuento: 0,
        detalles: carrito.map((i) => ({
          idProducto: i.producto.id,
          cantidad: i.cantidad,
          precioUnitario:
            i.producto.precioVenta,
          subtotal: i.subtotal,
          descuento: 0,
        })),
      };

      try {
        const res = await api.post("/ventas", dto);

        setUltimaVenta(res.data);

        // ✅ alerta stock agotado
        res.data.detalles.forEach(
          (d: DetalleVenta) => {
            const prod = productos.find(
              (p) => p.id === d.idProducto
            );

            if (
              prod &&
              prod.stock - d.cantidad <= 0
            ) {
              setErrorMsg(
                `⚠️ ${d.nombreProducto} se quedó sin stock`
              );
            }
          }
        );

        await api
          .get("/productos")
          .then((r) => setProductos(r.data));

        setEfectivoVenta(efectivo);
        setCarrito([]);
        setBusqueda("");
        setEfectivo(0);
        setTicketVisible(true);

        setTimeout(() => {
          window.print();
        }, 300);


      } catch (error) {
        const err = error as AxiosError<{
          message: string;
        }>;

        const msg =
          err.response?.data?.message ||
          "Error al procesar la venta";

        setErrorMsg(msg);
      }
    
    };

    return (
      <Layout>
        <div className="pos-grid">

          {/* PRODUCTOS */}
  <div className="pos-productos">
    <div className="card">

      <div className="card-header">
        <input
          className="search-input"
          style={{ width: "100%" }}
          placeholder="🔍 Buscar producto..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
      </div>

      <div className="productos-grid">
        {filtrados.map((p) => (
          <div
            key={p.id}
            className="producto-card"
            style={{
              opacity: p.stock === 0 ? 0.5 : 1,
              pointerEvents: p.stock === 0 ? "none" : "auto",
              border:
                p.stock > 0 &&
                p.stock <= (p.stockMinimo || 0)
                  ? "2px solid #f59e0b"
                  : ""
            }}
            onClick={() => agregarAlCarrito(p)}
          >
            <div className="producto-nombre">{p.nombre}</div>
            <div className="producto-precio">
              ${p.precioVenta.toFixed(2)}
            </div>

            <div className="producto-stock">
              Stock: {p.stock}
              {p.stock > 0 &&
                p.stock <= (p.stockMinimo || 0) && (
                  <span style={{ color: "#f59e0b", marginLeft: 5 }}>
                    ⚠️ Bajo
                  </span>
                )}
            </div>
          </div>
        ))}
      </div>

    </div>
  </div>

          {/* CARRITO */}
<div className="pos-carrito">
  <div className="card" style={{ height: "100%" }}>

    <div className="card-header">
      <h3 className="card-title">🛒 Carrito</h3>
    </div>

    <div className="carrito-items">
      {carrito.length === 0 && (
        <div className="carrito-empty">
          Agrega productos para iniciar la venta
        </div>
      )}

      {carrito.map((item) => (
        <div key={item.producto.id} className="carrito-item">

          <div className="carrito-item-nombre">
            {item.producto.nombre}
          </div>

          <div className="carrito-item-controles">

            <button
              className="qty-btn"
              onClick={() =>
                cambiarCantidad(item.producto.id, item.cantidad - 1)
              }
            >
              −
            </button>

            {/* ✅ INPUT EDITABLE */}
            <input
              type="number"
              className="qty-input"
              value={item.cantidad}
              min={1}
              max={item.producto.stock}
              onChange={(e) =>
                cambiarCantidad(
                  item.producto.id,
                  Number(e.target.value)
                )
              }
            />

            <button
              className="qty-btn"
              onClick={() =>
                cambiarCantidad(item.producto.id, item.cantidad + 1)
              }
            >
              +
            </button>
          </div>

          <div className="carrito-item-precio">
            ${item.subtotal.toFixed(2)}
          </div>

          <button
            className="carrito-remove"
            onClick={() =>
              quitarDelCarrito(item.producto.id)
            }
          >
            ✕
          </button>
        </div>
      ))}
    </div>

    <div className="carrito-footer">

      <div className="carrito-total">
        <span>Total</span>
        <span className="total-amount">
          ${total.toFixed(2)}
        </span>
      </div>

      <div className="form-field">
        <label>Método de pago</label>
        <select
          value={metodoPago}
          onChange={(e) => setMetodoPago(e.target.value)}
        >
          <option value="EFECTIVO">Efectivo</option>
          <option value="TARJETA">Tarjeta</option>
          <option value="TRANSFERENCIA">Transferencia</option>
        </select>
      </div>
<div className="form-field" style={{ marginBottom: "10px" }}>
  <label>Cliente</label>
  <select
    value={idCliente || ""}
    onChange={(e) => setIdCliente(e.target.value ? +e.target.value : null)}
  >
    <option value="">Público en general</option>
    {clientes.map((c) => (
      <option key={c.id} value={c.id}>{c.nombre}</option>
    ))}
  </select>
</div>
      {metodoPago === "EFECTIVO" && (
        <div className="form-field">
          <label>Efectivo recibido</label>


        <input
          type="number"
          value={efectivo}
          onChange={(e) => setEfectivo(Number(e.target.value))}
          style={{
            border: efectivo < total ? "2px solid #ef4444" : ""
          }}
        />


          {efectivo > 0 && (
            <span style={{ color: "#16a34a" }}>
              Cambio: ${cambio.toFixed(2)}
            </span>
          )}
        </div>
      )}

        <button
          className="btn-primary"
          style={{ width: "100%" }}
          disabled={
            carrito.length === 0 ||
            total <= 0 ||
            (metodoPago === "EFECTIVO" && efectivo < total)
          }
          onClick={procesarVenta}
        >
        Cobrar ${total.toFixed(2)}
      </button>

    </div>
  </div>
</div>

        </div>

        {/* ERROR */}
        {errorMsg && (
  <div className="modal-overlay">
    <div className="ticket">

      <div className="ticket-header">
        <h2 style={{ color: "#dc2626" }}>⚠️ Atención</h2>
      </div>

      <div className="ticket-divider" />

      <p style={{ textAlign: "center", margin: "16px 0" }}>
        {errorMsg}
      </p>

      <button
        className="btn-primary"
        style={{ width: "100%" }}
        onClick={() => setErrorMsg(null)}
      >
        Aceptar
      </button>

    </div>
  </div>
)}

        {/* TICKET */}
{ticketVisible && ultimaVenta && (
  <div className="modal-overlay">
    <div className="ticket">

        <div className="ticket-header">
          <img src={logo} alt="Tienda" 
            style={{
              width: "50px",
              display: "block",
              margin: "0 auto 6px auto"
            }}
          />

      <h2>🏪 TIENDITA DE PRUEBA 🏪</h2>
      <p style={{ fontSize: "12px" }}>
        CP 52420 - León Guzmán No. 5
      </p>
      <p style={{ fontSize: "12px" }}>
        San Ramón, Tenancingo de Degollado
      </p>
      <p style={{ fontSize: "12px", marginBottom: "8px" }}>
        Estado de México
      </p>
      <p>Folio: {ultimaVenta.folio}</p>
      <p>{new Date(ultimaVenta.fecha).toLocaleString("es-MX")}</p>
    </div>
<div className="ticket-row">
  <span>Cliente:</span>
  <span>{clientes.find(c => c.id === idCliente)?.nombre || "Público en general"}</span>
</div>
      <div className="ticket-divider" />

      <table className="ticket-table">
        <thead>
          <tr>
            <th>Producto</th>
            <th>Cant.</th>
            <th>Total</th>
          </tr>
        </thead>

        <tbody>
          {ultimaVenta.detalles.map((d) => (
            <tr key={d.id}>
              <td>{d.nombreProducto}</td>
              <td>{d.cantidad}</td>
              <td>${d.subtotal.toFixed(2)}</td>
            </tr>
          ))}

        </tbody>
      </table>

    <div className="ticket-divider" />

    <div className="ticket-row ">
      <span>TIPO DE PAGO:</span>
      <span>{metodoPago}</span>
    </div>

    <div className="ticket-row ticket-total">
      <span>TOTAL</span>
      <span>${ultimaVenta.total.toFixed(2)}</span>
    </div>

      {metodoPago === "EFECTIVO" && (
        <>
          <div className="ticket-total">
            <span>EFECTIVO</span>
            <span>${efectivoVenta.toFixed(2)}</span>
          </div>

          <div className="ticket-total">
            <span>CAMBIO</span>
            <span style={{ color: "#16a34a" }}>
              ${(efectivoVenta - ultimaVenta.total).toFixed(2)}
            </span>
          </div>
        </>
      )}

      <p style={{ fontSize: "13px", textAlign: "center" }}>
        Le atendió:
      </p>

      <p style={{ fontWeight: "bold", textAlign: "center" }}>
        {user?.nombre || " Usuario " }
      </p>
      
      <p style={{ fontSize: "11px", textAlign: "center", marginTop: "6px" }}>
        Conserve su ticket para cualquier aclaración.
      </p>

      <p className="ticket-gracias">¡Gracias por su compra!</p>
       <p className="ticket-gracias">¡Síguenos en Facebook e Instagram como: @TuTienda!</p>

      <div style={{ display: "flex", gap: "10px", marginTop: "16px" }}>

      <button
        className="btn-secondary no-print"
        onClick={() => window.print()}
      >
        🖨️ Imprimir
      </button>


        <button
          className="btn-primary no-print "
          style={{ flex: 2 }}
          onClick={() => setTicketVisible(false)}
        >
          Cerrar
        </button>
      </div>

    </div>
  </div>
)}
      </Layout>
    );
  }