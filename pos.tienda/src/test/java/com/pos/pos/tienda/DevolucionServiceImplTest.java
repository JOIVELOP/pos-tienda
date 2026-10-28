package com.pos.pos.tienda;

import com.pos.pos.tienda.dto.DevolucionDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.impl.DevolucionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevolucionServiceImplTest {
    @Mock private VentaRepository ventaRepository;
    @Mock private DevolucionVentaRepository devolucionRepository;
    @Mock private LineaDevolucionRepository lineaDevolucionRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TurnoCajaRepository turnoCajaRepository;
    @Mock private MovimientoCajaRepository movimientoCajaRepository;

    private DevolucionServiceImpl service;
    private Venta venta;
    private DetalleVenta detalle;
    private Producto producto;
    private Usuario encargado;
    private TurnoCaja turno;

    @BeforeEach
    void setUp() {
        service = new DevolucionServiceImpl(ventaRepository, devolucionRepository,
                lineaDevolucionRepository, productoRepository, usuarioRepository,
                turnoCajaRepository, movimientoCajaRepository);

        encargado = new Usuario();
        encargado.setId(5L);
        encargado.setUsername("encargado");
        encargado.setActivo(true);
        encargado.setRol(Usuario.Rol.ENCARGADO);
        turno = new TurnoCaja();
        turno.setId(8L);
        turno.setFolio("CAJ-0000000008");
        turno.setEstado(TurnoCaja.Estado.ABIERTO);
        turno.setFondoInicial(new BigDecimal("20.00"));
        turno.setUsuarioApertura(encargado);
        producto = new Producto();
        producto.setId(3L);
        producto.setNombre("Globos");
        producto.setStock(4);
        detalle = new DetalleVenta();
        detalle.setId(11L);
        detalle.setCantidad(2);
        detalle.setSubtotal(new BigDecimal("20.00"));
        detalle.setPrecioUnitario(new BigDecimal("10.00"));
        detalle.setProducto(producto);
        venta = new Venta();
        venta.setId(9L);
        venta.setFolio("VTA-0000000009");
        venta.setEstatus(Venta.Estatus.COMPLETADA);
        venta.setDetalles(new ArrayList<>(List.of(detalle)));
        detalle.setVenta(venta);

        when(usuarioRepository.findByUsername("encargado")).thenReturn(Optional.of(encargado));
        when(turnoCajaRepository.findOpenByUsernameForUpdate(
                "encargado", TurnoCaja.Estado.ABIERTO)).thenReturn(Optional.of(turno));
        when(ventaRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(venta));
        when(lineaDevolucionRepository.findByDetalleVentaId(11L)).thenReturn(List.of());
        when(devolucionRepository.saveAndFlush(any(DevolucionVenta.class))).thenAnswer(invocation -> {
            DevolucionVenta devolucion = invocation.getArgument(0);
            devolucion.setId(14L);
            return devolucion;
        });
        when(devolucionRepository.save(any(DevolucionVenta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void partialCashRefundRestocksOnlyAcceptedItemsAndCreatesCashOutflow() {
        when(ventaRepository.sumCashSalesByTurnoId(8L)).thenReturn(new BigDecimal("100.00"));
        when(movimientoCajaRepository.sumMontoByTurnoAndTipo(turno, MovimientoCaja.Tipo.ENTRADA))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoCajaRepository.sumMontoByTurnoAndTipo(turno, MovimientoCaja.Tipo.SALIDA))
                .thenReturn(BigDecimal.ZERO);
        when(productoRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoCajaRepository.save(any(MovimientoCaja.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DevolucionDTO.CrearRequest request = request(1, true);
        DevolucionDTO result = service.crear(request, "encargado");

        assertEquals("DEV-0000000014", result.getFolio());
        assertEquals(new BigDecimal("10.00"), result.getTotalReembolsado());
        assertEquals(5, producto.getStock());
        verify(movimientoCajaRepository).save(any(MovimientoCaja.class));
    }

    @Test
    void partialReturnWithoutRestockingLeavesInventoryUnchanged() {
        DevolucionDTO.CrearRequest request = request(1, false);

        service.crear(request, "encargado");

        assertEquals(4, producto.getStock());
        verify(productoRepository, never()).save(any(Producto.class));
        verify(movimientoCajaRepository).save(any(MovimientoCaja.class));
    }

    private DevolucionDTO.CrearRequest request(int quantity, boolean restock) {
        DevolucionDTO.CrearRequest request = new DevolucionDTO.CrearRequest();
        request.setIdVenta(9L);
        request.setMotivo("Producto defectuoso");
        request.setMetodoReembolso(Venta.MetodoPago.EFECTIVO);
        DevolucionDTO.LineaRequest line = new DevolucionDTO.LineaRequest();
        line.setIdDetalleVenta(11L);
        line.setCantidad(quantity);
        line.setReingresaInventario(restock);
        request.setLineas(List.of(line));
        return request;
    }
}
