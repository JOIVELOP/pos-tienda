package com.pos.pos.tienda;

import com.pos.pos.tienda.dto.CompraDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.impl.CompraServiceImpl;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {
    @Mock private CompraRepository compraRepository;
    @Mock private RecepcionCompraRepository recepcionRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private CompraServiceImpl service;
    private Compra compra;
    private LineaCompra linea;
    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new CompraServiceImpl(compraRepository, recepcionRepository,
                productoRepository, proveedorRepository, usuarioRepository);
        usuario = new Usuario();
        usuario.setId(2L);
        usuario.setUsername("encargado");
        usuario.setActivo(true);
        Proveedor proveedor = new Proveedor();
        proveedor.setId(3L);
        proveedor.setNombre("Proveedor");
        proveedor.setActivo(true);
        producto = new Producto();
        producto.setId(4L);
        producto.setNombre("Cajas");
        producto.setCodigo("CJ-1");
        producto.setActivo(true);
        producto.setStock(5);
        producto.setPrecioCompra(new BigDecimal("4.00"));
        linea = new LineaCompra();
        linea.setId(12L);
        linea.setCantidadSolicitada(10);
        linea.setCantidadRecibida(0);
        linea.setCostoUnitario(new BigDecimal("4.50"));
        linea.setProducto(producto);
        compra = new Compra();
        compra.setId(9L);
        compra.setFolio("COM-0000000009");
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setLineas(new ArrayList<>(List.of(linea)));
        linea.setCompra(compra);
    }

    @Test
    void partialReceiptAddsOnlyGoodStockAndKeepsReceiptCostHistory() {
        when(compraRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(compra));
        when(usuarioRepository.findByUsername("encargado")).thenReturn(Optional.of(usuario));
        when(productoRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recepcionRepository.saveAndFlush(any(RecepcionCompra.class))).thenAnswer(invocation -> {
            RecepcionCompra recepcion = invocation.getArgument(0);
            recepcion.setId(15L);
            return recepcion;
        });
        when(recepcionRepository.save(any(RecepcionCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recepcionRepository.findByCompraIdOrderByFechaAsc(9L)).thenAnswer(invocation -> {
            List<RecepcionCompra> receipts = new ArrayList<>();
            return receipts;
        });
        CompraDTO.RecepcionRequest request = new CompraDTO.RecepcionRequest();
        CompraDTO.LineaRecepcionRequest received = new CompraDTO.LineaRecepcionRequest();
        received.setIdLineaCompra(12L);
        received.setCantidadBuena(3);
        received.setCantidadDanada(1);
        received.setCostoUnitario(new BigDecimal("5.25"));
        request.setLineas(List.of(received));

        CompraDTO result = service.recibir(9L, request, "encargado");

        assertEquals("PARCIAL", result.getEstado());
        assertEquals(4, result.getLineas().get(0).getCantidadRecibida());
        assertEquals(8, producto.getStock());
        assertEquals(new BigDecimal("5.25"), producto.getPrecioCompra());
        ArgumentCaptor<RecepcionCompra> receiptCaptor = ArgumentCaptor.forClass(RecepcionCompra.class);
        verify(recepcionRepository).saveAndFlush(receiptCaptor.capture());
        assertEquals(3, receiptCaptor.getValue().getLineas().get(0).getCantidadBuena());
        assertEquals(1, receiptCaptor.getValue().getLineas().get(0).getCantidadDanada());
        verify(productoRepository).save(producto);
    }

    @Test
    void receiptCannotExceedTheRemainingOrderQuantity() {
        when(compraRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(compra));
        when(usuarioRepository.findByUsername("encargado")).thenReturn(Optional.of(usuario));
        CompraDTO.RecepcionRequest request = new CompraDTO.RecepcionRequest();
        CompraDTO.LineaRecepcionRequest received = new CompraDTO.LineaRecepcionRequest();
        received.setIdLineaCompra(12L);
        received.setCantidadBuena(11);
        received.setCantidadDanada(0);
        received.setCostoUnitario(new BigDecimal("5.25"));
        request.setLineas(List.of(received));

        assertThrows(RuntimeException.class, () -> service.recibir(9L, request, "encargado"));
        assertEquals(5, producto.getStock());
        verify(productoRepository, never()).save(any(Producto.class));
    }
}
