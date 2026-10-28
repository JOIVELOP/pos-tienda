package com.pos.pos.tienda;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.OfflineDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.impl.OfflineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:offline;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=offline-integration-test-key-not-for-production-123456789",
        "jwt.expiration=3600000"
})
class OfflineIntegrationTest {
    @Autowired OfflineService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired TurnoCajaRepository turnos;
    @Autowired VentaRepository ventas;
    @Autowired OperacionOfflineRepository operaciones;
    @Autowired MovimientoCajaRepository movimientos;
    private Usuario usuario;
    private Producto producto;
    private TurnoCaja turno;
    private OfflineDTO.Sesion sesion;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setUsername(UUID.randomUUID().toString());
        usuario.setNombre("Caja");
        usuario.setApellido("Prueba");
        usuario.setPassword("test-only");
        usuario.setRol(Usuario.Rol.EMPLEADO);
        usuarios.saveAndFlush(usuario);
        producto = new Producto();
        producto.setCodigo(UUID.randomUUID().toString());
        producto.setNombre("Producto offline");
        producto.setPrecioCompra(new BigDecimal("2.00"));
        producto.setPrecioVenta(new BigDecimal("10.00"));
        producto.setStock(10);
        productos.saveAndFlush(producto);
        turno = new TurnoCaja();
        turno.setFolio("TEST-" + UUID.randomUUID().toString().substring(0, 12));
        turno.setUsuarioApertura(usuario);
        turno.setFondoInicial(new BigDecimal("100.00"));
        turnos.saveAndFlush(turno);
        sesion = service.preparar(new OfflineDTO.Preparar(UUID.randomUUID().toString()), usuario.getUsername());
    }

    private OfflineDTO.Operacion operacion(long secuencia, OfflineDTO.Tipo tipo, String monto) {
        return new OfflineDTO.Operacion(UUID.randomUUID().toString(), sesion.id(), sesion.dispositivo(),
                secuencia, Instant.now(), tipo, new BigDecimal(monto), "Prueba",
                tipo == OfflineDTO.Tipo.VENTA ? List.of(new OfflineDTO.Linea(producto.getId(), 2)) : List.of());
    }

    @Test
    void lostConfirmationAndConcurrentRetriesCreateExactlyOneSale() throws Exception {
        var request = operacion(1, OfflineDTO.Tipo.VENTA, "20.00");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> service.sincronizar(request, usuario.getUsername()));
            var second = executor.submit(() -> service.sincronizar(request, usuario.getUsername()));
            assertEquals(first.get(15, TimeUnit.SECONDS).folio(), second.get(15, TimeUnit.SECONDS).folio());
        } finally { executor.shutdownNow(); }
        assertEquals(8, productos.findById(producto.getId()).orElseThrow().getStock());
        assertTrue(operaciones.existsById(request.id()));
        assertEquals(1, ventas.findByUsuarioId(usuario.getId()).size());
    }

    @Test
    void changedPayloadAndOutOfOrderRequestsAreRejected() {
        var request = operacion(1, OfflineDTO.Tipo.VENTA, "20.00");
        service.sincronizar(request, usuario.getUsername());
        var changed = new OfflineDTO.Operacion(request.id(), request.sesion(), request.dispositivo(),
                request.secuencia(), request.captura(), request.tipo(), new BigDecimal("21.00"),
                request.descripcion(), request.lineas());
        assertThrows(BusinessException.class, () -> service.sincronizar(changed, usuario.getUsername()));
        assertThrows(BusinessException.class, () -> service.sincronizar(
                operacion(3, OfflineDTO.Tipo.APORTACION, "5.00"), usuario.getUsername()));
        assertEquals(8, productos.findById(producto.getId()).orElseThrow().getStock());
    }

    @Test
    void invalidAmountRollsBackStockAndCanBeReconciledWithoutLosingSequence() {
        var invalid = operacion(1, OfflineDTO.Tipo.VENTA, "19.00");
        assertThrows(BusinessException.class, () -> service.sincronizar(invalid, usuario.getUsername()));
        assertEquals(10, productos.findById(producto.getId()).orElseThrow().getStock());
        assertFalse(operaciones.existsById(invalid.id()));
        service.sincronizar(operacion(1, OfflineDTO.Tipo.VENTA, "20.00"), usuario.getUsername());
    }

    @Test
    void frozenPricesMovementsAndCloseReconcileExactlyOnce() {
        producto.setPrecioVenta(new BigDecimal("15.00"));
        productos.saveAndFlush(producto);
        service.sincronizar(operacion(1, OfflineDTO.Tipo.VENTA, "20.00"), usuario.getUsername());
        service.sincronizar(operacion(2, OfflineDTO.Tipo.GASTO, "5.00"), usuario.getUsername());
        var close = operacion(3, OfflineDTO.Tipo.CIERRE, "114.00");
        service.sincronizar(close, usuario.getUsername());
        service.sincronizar(close, usuario.getUsername());
        TurnoCaja cerrado = turnos.findById(turno.getId()).orElseThrow();
        assertEquals(TurnoCaja.Estado.CERRADO, cerrado.getEstado());
        assertEquals(new BigDecimal("115.00"), cerrado.getEfectivoEsperado());
        assertEquals(new BigDecimal("-1.00"), cerrado.getDiferencia());
        assertEquals(1, movimientos.findByTurnoOrderByFechaAsc(turno).size());
    }

    @Test
    void otherDeviceCannotPrepareTheSameShift() {
        assertThrows(BusinessException.class, () -> service.preparar(
                new OfflineDTO.Preparar(UUID.randomUUID().toString()), usuario.getUsername()));
        var request = operacion(1, OfflineDTO.Tipo.VENTA, "20.00");
        var otherDevice = new OfflineDTO.Operacion(request.id(), request.sesion(), UUID.randomUUID().toString(),
                request.secuencia(), request.captura(), request.tipo(), request.monto(),
                request.descripcion(), request.lineas());
        assertThrows(BusinessException.class, () -> service.sincronizar(otherDevice, usuario.getUsername()));
    }

    @Test
    void insufficientServerStockPreservesSequenceAndDoesNotRecordACashSale() {
        producto.setStock(1);
        productos.saveAndFlush(producto);
        var sale = operacion(1, OfflineDTO.Tipo.VENTA, "20.00");
        assertThrows(BusinessException.class, () -> service.sincronizar(sale, usuario.getUsername()));
        assertFalse(operaciones.existsById(sale.id()));
        assertEquals(1, productos.findById(producto.getId()).orElseThrow().getStock());
        assertEquals(0, ventas.findByUsuarioId(usuario.getId()).size());
    }

    @Test
    void cashOutflowCannotExceedAvailableCashAndClosedSessionCannotAcceptNewOperations() {
        assertThrows(BusinessException.class, () -> service.sincronizar(
                operacion(1, OfflineDTO.Tipo.RETIRO, "100.01"), usuario.getUsername()));
        service.sincronizar(operacion(1, OfflineDTO.Tipo.CIERRE, "100.00"), usuario.getUsername());
        assertThrows(BusinessException.class, () -> service.sincronizar(
                operacion(2, OfflineDTO.Tipo.APORTACION, "1.00"), usuario.getUsername()));
    }

    @Test
    void expiredCaptureIsRejectedButPreviouslyCapturedOperationCanSyncLater() {
        var sale = operacion(1, OfflineDTO.Tipo.VENTA, "20.00");
        var expired = new OfflineDTO.Operacion(sale.id(), sale.sesion(), sale.dispositivo(),
                sale.secuencia(), sesion.vence().plusSeconds(1), sale.tipo(), sale.monto(),
                sale.descripcion(), sale.lineas());
        assertThrows(BusinessException.class, () -> service.sincronizar(expired, usuario.getUsername()));
        service.sincronizar(sale, usuario.getUsername());
    }
}
