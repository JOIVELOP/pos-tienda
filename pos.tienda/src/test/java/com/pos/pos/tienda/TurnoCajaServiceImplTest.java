package com.pos.pos.tienda;

import com.pos.pos.tienda.dto.TurnoCajaDTO;
import com.pos.pos.tienda.model.MovimientoCaja;
import com.pos.pos.tienda.model.TurnoCaja;
import com.pos.pos.tienda.model.Usuario;
import com.pos.pos.tienda.repository.MovimientoCajaRepository;
import com.pos.pos.tienda.repository.TurnoCajaRepository;
import com.pos.pos.tienda.repository.UsuarioRepository;
import com.pos.pos.tienda.repository.VentaRepository;
import com.pos.pos.tienda.service.impl.TurnoCajaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoCajaServiceImplTest {
    @Mock private TurnoCajaRepository turnoRepository;
    @Mock private MovimientoCajaRepository movimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private VentaRepository ventaRepository;

    private TurnoCajaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TurnoCajaServiceImpl(turnoRepository, movimientoRepository,
                usuarioRepository, ventaRepository);
    }

    @Test
    void closingCalculatesCashAndDifferenceFromSalesAndMovements() {
        Usuario usuario = new Usuario();
        usuario.setUsername("cajera");
        TurnoCaja turno = new TurnoCaja();
        turno.setId(8L);
        turno.setFolio("CAJ-0000000008");
        turno.setEstado(TurnoCaja.Estado.ABIERTO);
        turno.setFondoInicial(new BigDecimal("100.00"));
        turno.setUsuarioApertura(usuario);
        when(turnoRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(turno));
        when(ventaRepository.sumCashSalesByTurnoId(8L)).thenReturn(new BigDecimal("50.00"));
        when(movimientoRepository.sumMontoByTurnoAndTipo(turno, MovimientoCaja.Tipo.ENTRADA))
                .thenReturn(new BigDecimal("10.00"));
        when(movimientoRepository.sumMontoByTurnoAndTipo(turno, MovimientoCaja.Tipo.SALIDA))
                .thenReturn(new BigDecimal("5.00"));
        when(movimientoRepository.findByTurnoOrderByFechaAsc(turno)).thenReturn(List.of());
        when(turnoRepository.save(any(TurnoCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TurnoCajaDTO.CierreRequest request = new TurnoCajaDTO.CierreRequest();
        request.setEfectivoContado(new BigDecimal("150.00"));

        TurnoCajaDTO result = service.cerrar(8L, request, "cajera");

        assertEquals(new BigDecimal("155.00"), result.getEfectivoEsperado());
        assertEquals(new BigDecimal("-5.00"), result.getDiferencia());
        assertEquals("CERRADO", result.getEstado());
        verify(turnoRepository).save(turno);
    }

    @Test
    void cashOutflowCannotExceedAvailableCash() {
        Usuario usuario = new Usuario();
        usuario.setUsername("cajera");
        TurnoCaja turno = new TurnoCaja();
        turno.setId(8L);
        turno.setEstado(TurnoCaja.Estado.ABIERTO);
        turno.setFondoInicial(new BigDecimal("50.00"));
        turno.setUsuarioApertura(usuario);
        when(turnoRepository.findOpenByUsernameForUpdate(
                "cajera", TurnoCaja.Estado.ABIERTO)).thenReturn(Optional.of(turno));
        when(ventaRepository.sumCashSalesByTurnoId(8L)).thenReturn(BigDecimal.ZERO);
        when(movimientoRepository.sumMontoByTurnoAndTipo(turno, MovimientoCaja.Tipo.ENTRADA))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoRepository.sumMontoByTurnoAndTipo(turno, MovimientoCaja.Tipo.SALIDA))
                .thenReturn(BigDecimal.ZERO);

        TurnoCajaDTO.MovimientoRequest request = new TurnoCajaDTO.MovimientoRequest();
        request.setTipo(MovimientoCaja.Tipo.SALIDA);
        request.setConcepto("GASTO");
        request.setDescripcion("Compra de bolsas");
        request.setMonto(new BigDecimal("50.01"));

        assertThrows(RuntimeException.class, () -> service.registrarMovimiento(request, "cajera"));
        verify(movimientoRepository, never()).save(any(MovimientoCaja.class));
    }

    @Test
    void managerCanReviewAllEmployeeShifts() {
        Usuario manager = new Usuario();
        manager.setUsername("encargado");
        manager.setActivo(true);
        manager.setRol(Usuario.Rol.ENCARGADO);
        Usuario cashier = new Usuario();
        cashier.setUsername("cajera");
        TurnoCaja turno = new TurnoCaja();
        turno.setId(8L);
        turno.setEstado(TurnoCaja.Estado.CERRADO);
        turno.setFolio("CAJ-0000000008");
        turno.setFondoInicial(new BigDecimal("100.00"));
        turno.setUsuarioApertura(cashier);
        when(usuarioRepository.findByUsername("encargado")).thenReturn(Optional.of(manager));
        when(turnoRepository.findAllByOrderByAperturaDesc()).thenReturn(List.of(turno));
        when(movimientoRepository.findByTurnoOrderByFechaAsc(turno)).thenReturn(List.of());

        List<TurnoCajaDTO> result = service.historial("encargado");

        assertEquals(1, result.size());
        assertEquals("cajera", result.get(0).getUsuarioApertura());
        verify(turnoRepository, never()).findByUsuarioAperturaUsernameOrderByAperturaDesc("encargado");
    }
}
