package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.TurnoCajaDTO;
import com.pos.pos.tienda.model.MovimientoCaja;
import com.pos.pos.tienda.model.TurnoCaja;
import com.pos.pos.tienda.model.Usuario;
import com.pos.pos.tienda.repository.MovimientoCajaRepository;
import com.pos.pos.tienda.repository.TurnoCajaRepository;
import com.pos.pos.tienda.repository.UsuarioRepository;
import com.pos.pos.tienda.repository.VentaRepository;
import com.pos.pos.tienda.service.TurnoCajaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TurnoCajaServiceImpl implements TurnoCajaService {
    private static final Logger log = LoggerFactory.getLogger(TurnoCajaServiceImpl.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final TurnoCajaRepository turnoRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;

    public TurnoCajaServiceImpl(TurnoCajaRepository turnoRepository,
                                MovimientoCajaRepository movimientoRepository,
                                UsuarioRepository usuarioRepository,
                                VentaRepository ventaRepository) {
        this.turnoRepository = turnoRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.ventaRepository = ventaRepository;
    }

    @Override
    @Transactional
    public TurnoCajaDTO abrir(TurnoCajaDTO.AperturaRequest request, String username) {
        Usuario usuario = usuarioRepository.findByUsernameForUpdate(username)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));
        if (turnoRepository.findOpenByUsername(username, TurnoCaja.Estado.ABIERTO).isPresent()) {
            throw new BusinessException("Ya existe un turno de caja abierto para este usuario");
        }

        TurnoCaja turno = new TurnoCaja();
        turno.setFolio("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        turno.setFondoInicial(request.getFondoInicial());
        turno.setUsuarioApertura(usuario);
        turnoRepository.saveAndFlush(turno);
        turno.setFolio(String.format("CAJ-%010d", turno.getId()));
        turnoRepository.save(turno);
        log.info("Turno de caja abierto: folio={}, usuario={}, fondoInicial={}",
                turno.getFolio(), username, turno.getFondoInicial());
        return toDTO(turno);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoCajaDTO> turnoActual(String username) {
        return turnoRepository.findOpenByUsername(username, TurnoCaja.Estado.ABIERTO)
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoCajaDTO> historial(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));
        if (usuario.getRol() == Usuario.Rol.ADMINISTRADOR
                || usuario.getRol() == Usuario.Rol.ENCARGADO) {
            return turnoRepository.findAllByOrderByAperturaDesc()
                    .stream().map(this::toDTO).toList();
        }
        return turnoRepository.findByUsuarioAperturaUsernameOrderByAperturaDesc(username)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public TurnoCajaDTO registrarMovimiento(TurnoCajaDTO.MovimientoRequest request,
                                             String username) {
        TurnoCaja turno = turnoRepository.findOpenByUsernameForUpdate(
                        username, TurnoCaja.Estado.ABIERTO)
                .orElseThrow(() -> new BusinessException("Abre un turno de caja antes de registrar movimientos"));
        Usuario usuario = turno.getUsuarioApertura();
        validarConcepto(request);
        if (request.getTipo() == MovimientoCaja.Tipo.SALIDA) {
            BigDecimal efectivoDisponible = calcularEfectivoEsperado(turno);
            if (request.getMonto().compareTo(efectivoDisponible) > 0) {
                throw new BusinessException("El efectivo disponible en caja es insuficiente para esta salida");
            }
        }

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setTipo(request.getTipo());
        movimiento.setConcepto(request.getConcepto().trim().toUpperCase());
        movimiento.setDescripcion(request.getDescripcion().trim());
        movimiento.setMonto(request.getMonto());
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTurno(turno);
        movimiento.setUsuario(usuario);
        movimientoRepository.save(movimiento);
        log.info("Movimiento de caja: turno={}, tipo={}, concepto={}, monto={}, usuario={}",
                turno.getFolio(), movimiento.getTipo(), movimiento.getConcepto(),
                movimiento.getMonto(), username);
        return toDTO(turno);
    }

    @Override
    @Transactional
    public TurnoCajaDTO cerrar(Long id, TurnoCajaDTO.CierreRequest request, String username) {
        TurnoCaja turno = turnoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Turno de caja no encontrado: " + id));
        if (turno.getEstado() != TurnoCaja.Estado.ABIERTO) {
            throw new BusinessException("El turno de caja ya está cerrado");
        }
        if (!turno.getUsuarioApertura().getUsername().equals(username)) {
            throw new BusinessException("Solo quien abrió el turno puede cerrarlo");
        }

        BigDecimal esperado = calcularEfectivoEsperado(turno);

        turno.setCierre(LocalDateTime.now());
        turno.setEfectivoEsperado(esperado);
        turno.setEfectivoContado(request.getEfectivoContado());
        turno.setDiferencia(request.getEfectivoContado().subtract(esperado));
        turno.setUsuarioCierre(turno.getUsuarioApertura());
        turno.setEstado(TurnoCaja.Estado.CERRADO);
        turnoRepository.save(turno);
        log.info("Turno de caja cerrado: folio={}, usuario={}, esperado={}, contado={}, diferencia={}",
                turno.getFolio(), username, esperado, request.getEfectivoContado(), turno.getDiferencia());
        return toDTO(turno);
    }

    private void validarConcepto(TurnoCajaDTO.MovimientoRequest request) {
        String concepto = request.getConcepto().trim().toUpperCase();
        if (concepto.equals("APORTACION") && request.getTipo() != MovimientoCaja.Tipo.ENTRADA) {
            throw new BusinessException("Una aportación debe ser una entrada de efectivo");
        }
        if ((concepto.equals("RETIRO") || concepto.equals("GASTO"))
                && request.getTipo() != MovimientoCaja.Tipo.SALIDA) {
            throw new BusinessException("Retiros y gastos deben ser salidas de efectivo");
        }
        if (!List.of("APORTACION", "RETIRO", "GASTO", "AJUSTE").contains(concepto)) {
            throw new BusinessException("Concepto de movimiento inválido");
        }
    }

    private TurnoCajaDTO toDTO(TurnoCaja turno) {
        BigDecimal ventasEfectivo = zeroIfNull(ventaRepository.sumCashSalesByTurnoId(turno.getId()));
        BigDecimal entradas = zeroIfNull(movimientoRepository.sumMontoByTurnoAndTipo(
                turno, MovimientoCaja.Tipo.ENTRADA));
        BigDecimal salidas = zeroIfNull(movimientoRepository.sumMontoByTurnoAndTipo(
                turno, MovimientoCaja.Tipo.SALIDA));
        TurnoCajaDTO dto = new TurnoCajaDTO();
        dto.setId(turno.getId());
        dto.setFolio(turno.getFolio());
        dto.setEstado(turno.getEstado().name());
        dto.setApertura(turno.getApertura());
        dto.setCierre(turno.getCierre());
        dto.setUsuarioApertura(turno.getUsuarioApertura().getUsername());
        dto.setUsuarioCierre(turno.getUsuarioCierre() == null
                ? null : turno.getUsuarioCierre().getUsername());
        dto.setFondoInicial(turno.getFondoInicial());
        dto.setVentasEfectivo(ventasEfectivo);
        dto.setEntradas(entradas);
        dto.setSalidas(salidas);
        dto.setEfectivoEsperado(turno.getEfectivoEsperado() != null
                ? turno.getEfectivoEsperado()
                : turno.getFondoInicial().add(ventasEfectivo).add(entradas).subtract(salidas));
        dto.setEfectivoContado(turno.getEfectivoContado());
        dto.setDiferencia(turno.getDiferencia());
        dto.setMovimientos(movimientoRepository.findByTurnoOrderByFechaAsc(turno).stream()
                .map(this::toMovimientoDTO).toList());
        return dto;
    }

    private BigDecimal calcularEfectivoEsperado(TurnoCaja turno) {
        BigDecimal ventasEfectivo = zeroIfNull(ventaRepository.sumCashSalesByTurnoId(turno.getId()));
        BigDecimal entradas = zeroIfNull(movimientoRepository.sumMontoByTurnoAndTipo(
                turno, MovimientoCaja.Tipo.ENTRADA));
        BigDecimal salidas = zeroIfNull(movimientoRepository.sumMontoByTurnoAndTipo(
                turno, MovimientoCaja.Tipo.SALIDA));
        return turno.getFondoInicial().add(ventasEfectivo).add(entradas).subtract(salidas);
    }

    private TurnoCajaDTO.MovimientoDTO toMovimientoDTO(MovimientoCaja movimiento) {
        TurnoCajaDTO.MovimientoDTO dto = new TurnoCajaDTO.MovimientoDTO();
        dto.setId(movimiento.getId());
        dto.setTipo(movimiento.getTipo());
        dto.setConcepto(movimiento.getConcepto());
        dto.setDescripcion(movimiento.getDescripcion());
        dto.setMonto(movimiento.getMonto());
        dto.setFecha(movimiento.getFecha());
        dto.setUsuario(movimiento.getUsuario().getUsername());
        return dto;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
