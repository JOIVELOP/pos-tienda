package com.pos.pos.tienda;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.DetalleVentaDTO;
import com.pos.pos.tienda.dto.VentaDTO;
import com.pos.pos.tienda.model.DetalleVenta;
import com.pos.pos.tienda.model.Producto;
import com.pos.pos.tienda.model.Promocion;
import com.pos.pos.tienda.model.TurnoCaja;
import com.pos.pos.tienda.model.Usuario;
import com.pos.pos.tienda.model.Venta;
import com.pos.pos.tienda.repository.ClienteRepository;
import com.pos.pos.tienda.repository.DetalleVentaRepository;
import com.pos.pos.tienda.repository.ProductoRepository;
import com.pos.pos.tienda.repository.PromocionRepository;
import com.pos.pos.tienda.repository.UsuarioRepository;
import com.pos.pos.tienda.repository.VentaRepository;
import com.pos.pos.tienda.repository.TurnoCajaRepository;
import com.pos.pos.tienda.service.impl.VentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private DetalleVentaRepository detalleVentaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PromocionRepository promocionRepository;
    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    private VentaServiceImpl ventaService;
    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ventaService = new VentaServiceImpl(ventaRepository, detalleVentaRepository,
                productoRepository, clienteRepository, usuarioRepository, promocionRepository,
                turnoCajaRepository);

        producto = new Producto();
        producto.setId(7L);
        producto.setNombre("Floriset");
        producto.setCodigo("FL-01");
        producto.setPrecioVenta(new BigDecimal("12.50"));
        producto.setStock(10);
        producto.setActivo(true);

        usuario = new Usuario();
        usuario.setId(3L);
        usuario.setUsername("cajera");
        usuario.setNombre("Ana");
        usuario.setActivo(true);
        usuario.setRol(Usuario.Rol.EMPLEADO);
    }

    private void turnoAbierto() {
        TurnoCaja turno = new TurnoCaja();
        turno.setId(5L);
        turno.setFolio("CAJ-0000000005");
        turno.setEstado(TurnoCaja.Estado.ABIERTO);
        turno.setFondoInicial(BigDecimal.ZERO);
        turno.setUsuarioApertura(usuario);
        when(turnoCajaRepository.findOpenByUsernameForUpdate(
                "cajera", TurnoCaja.Estado.ABIERTO)).thenReturn(Optional.of(turno));
    }

    @Test
    void saveUsesAuthenticatedUserAndRecalculatesClientTotals() {
        turnoAbierto();
        when(usuarioRepository.findByUsername("cajera")).thenReturn(Optional.of(usuario));
        when(productoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(producto));
        when(promocionRepository.findPromocionVigenteByProducto(any(), any())).thenReturn(List.of());
        when(ventaRepository.saveAndFlush(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setId(42L);
            return venta;
        });
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaDTO request = ventaRequest(2, "999.00", "0.01", "0.00");
        request.setIdUsuario(999L);
        VentaDTO saved = ventaService.save(request, "cajera");

        assertEquals(3L, saved.getIdUsuario());
        assertEquals("25.00", saved.getSubtotal().toPlainString());
        assertEquals("25.00", saved.getTotal().toPlainString());
        assertEquals("VTA-0000000042", saved.getFolio());
        assertEquals(8, producto.getStock());

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertEquals("VTA-0000000042", ventaCaptor.getValue().getFolio());
    }

    @Test
    void saveRejectsDiscountGreaterThanCalculatedSubtotal() {
        turnoAbierto();
        usuario.setRol(Usuario.Rol.ENCARGADO);
        when(usuarioRepository.findByUsername("cajera")).thenReturn(Optional.of(usuario));
        when(productoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(producto));
        when(promocionRepository.findPromocionVigenteByProducto(any(), any())).thenReturn(List.of());

        VentaDTO request = ventaRequest(2, "25.00", "1.00", "25.00");

        assertThrows(BusinessException.class, () -> ventaService.save(request, "cajera"));
        assertEquals(10, producto.getStock());
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void saveAllowsAuthorizedDiscountAndRecalculatesFinalTotal() {
        turnoAbierto();
        usuario.setRol(Usuario.Rol.ENCARGADO);
        when(usuarioRepository.findByUsername("cajera")).thenReturn(Optional.of(usuario));
        when(productoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(producto));
        when(promocionRepository.findPromocionVigenteByProducto(any(), any())).thenReturn(List.of());
        when(ventaRepository.saveAndFlush(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setId(43L);
            return venta;
        });
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaDTO saved = ventaService.save(
                ventaRequest(2, "25.00", "20.00", "5.00"), "cajera");

        assertEquals("25.00", saved.getSubtotal().toPlainString());
        assertEquals("20.00", saved.getTotal().toPlainString());
    }

    @Test
    void saveRejectsManualDiscountFromCashier() {
        turnoAbierto();
        when(usuarioRepository.findByUsername("cajera")).thenReturn(Optional.of(usuario));

        VentaDTO request = ventaRequest(2, "25.00", "20.00", "5.00");

        assertThrows(BusinessException.class, () -> ventaService.save(request, "cajera"));
        verify(productoRepository, never()).findByIdForUpdate(7L);
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void saveChecksCombinedQuantityWhenProductAppearsMoreThanOnce() {
        turnoAbierto();
        when(usuarioRepository.findByUsername("cajera")).thenReturn(Optional.of(usuario));
        VentaDTO request = ventaRequest(6, "150.00", "150.00", "0.00");
        DetalleVentaDTO secondLine = new DetalleVentaDTO();
        secondLine.setIdProducto(7L);
        secondLine.setCantidad(6);
        secondLine.setPrecioUnitario(new BigDecimal("12.50"));
        secondLine.setSubtotal(new BigDecimal("75.00"));
        List<DetalleVentaDTO> details = new ArrayList<>(request.getDetalles());
        details.add(secondLine);
        request.setDetalles(details);

        assertThrows(BusinessException.class, () -> ventaService.save(request, "cajera"));
        verify(productoRepository).findByIdForUpdate(7L);
        verify(productoRepository, never()).save(any(Producto.class));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void saveCombinesRepeatedProductLinesBeforeApplyingBuyOneGetOnePromotion() {
        turnoAbierto();
        Promocion promocion = new Promocion();
        promocion.setTipo(Promocion.TipoPromocion.DOS_X_UNO);
        promocion.setValor(BigDecimal.ZERO);
        when(usuarioRepository.findByUsername("cajera")).thenReturn(Optional.of(usuario));
        when(productoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(producto));
        when(promocionRepository.findPromocionVigenteByProducto(any(), any()))
                .thenReturn(List.of(promocion));
        when(ventaRepository.saveAndFlush(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setId(44L);
            return venta;
        });
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaDTO request = ventaRequest(1, "25.00", "25.00", "0.00");
        DetalleVentaDTO secondLine = new DetalleVentaDTO();
        secondLine.setIdProducto(7L);
        secondLine.setCantidad(1);
        secondLine.setPrecioUnitario(new BigDecimal("12.50"));
        secondLine.setSubtotal(new BigDecimal("12.50"));
        request.setDetalles(new ArrayList<>(List.of(request.getDetalles().get(0), secondLine)));

        VentaDTO saved = ventaService.save(request, "cajera");

        assertEquals(1, saved.getDetalles().size());
        assertEquals(2, saved.getDetalles().get(0).getCantidad());
        assertEquals("12.50", saved.getTotal().toPlainString());
    }

    @Test
    void cancelRestoresStockOnlyOnceWhenRequestIsRepeated() {
        Venta venta = new Venta();
        venta.setId(10L);
        venta.setFolio("VTA-0000000010");
        venta.setEstatus(Venta.Estatus.COMPLETADA);
        venta.setDetalles(new ArrayList<>(List.of(saleDetail(venta, producto, 2))));
        producto.setStock(8);

        when(ventaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ventaService.cancelar(10L);
        ventaService.cancelar(10L);

        assertEquals(10, producto.getStock());
        verify(productoRepository, times(1)).save(producto);
        verify(ventaRepository, times(1)).save(venta);
    }

    @Test
    void reportCountsOnlyCompletedSales() {
        LocalDateTime inicio = LocalDateTime.parse("2026-10-01T00:00:00");
        LocalDateTime fin = LocalDateTime.parse("2026-10-31T23:59:59");
        Venta primera = new Venta();
        Venta segunda = new Venta();
        when(ventaRepository.totalVentasPorPeriodo(inicio, fin)).thenReturn(new BigDecimal("30.00"));
        when(ventaRepository.findByFechaBetweenAndEstatus(
                inicio, fin, Venta.Estatus.COMPLETADA)).thenReturn(List.of(primera, segunda));
        when(detalleVentaRepository.productosMasVendidos()).thenReturn(List.of());
        when(ventaRepository.reporteVentasPorDia(inicio, fin)).thenReturn(List.of());
        when(ventaRepository.reporteVentasPorHora(inicio, fin)).thenReturn(List.of());

        var reporte = ventaService.generarReporte(inicio, fin);

        assertEquals(2L, reporte.getCantidadVentas());
        assertEquals("15.00", reporte.getPromedioVenta().toPlainString());
        verify(ventaRepository).findByFechaBetweenAndEstatus(
                inicio, fin, Venta.Estatus.COMPLETADA);
    }

    private VentaDTO ventaRequest(int cantidad, String subtotal, String total, String descuento) {
        VentaDTO venta = new VentaDTO();
        venta.setMetodoPago(Venta.MetodoPago.EFECTIVO);
        venta.setSubtotal(new BigDecimal(subtotal));
        venta.setTotal(new BigDecimal(total));
        venta.setDescuento(new BigDecimal(descuento));
        DetalleVentaDTO detalle = new DetalleVentaDTO();
        detalle.setIdProducto(7L);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal("0.01"));
        detalle.setSubtotal(new BigDecimal("0.01"));
        venta.setDetalles(List.of(detalle));
        return venta;
    }

    private DetalleVenta saleDetail(Venta venta, Producto producto, int cantidad) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(producto.getPrecioVenta());
        detalle.setDescuento(BigDecimal.ZERO);
        detalle.setSubtotal(producto.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad)));
        return detalle;
    }
}
