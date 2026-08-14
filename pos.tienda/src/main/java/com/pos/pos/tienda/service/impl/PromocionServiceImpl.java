package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.PromocionDTO;
import com.pos.pos.tienda.model.Producto;
import com.pos.pos.tienda.model.Promocion;
import com.pos.pos.tienda.repository.ProductoRepository;
import com.pos.pos.tienda.repository.PromocionRepository;
import com.pos.pos.tienda.service.PromocionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromocionServiceImpl implements PromocionService {

    private static final String PROMOCION_NO_ENCONTRADA = "Promoción no encontrada: ";

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;

    public PromocionServiceImpl(PromocionRepository promocionRepository,
                                ProductoRepository productoRepository) {
        this.promocionRepository = promocionRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public Map<String, Object> calcularPrecioConPromocion(Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new BusinessException("Producto no encontrado"));

        List<Promocion> promociones = promocionRepository
                .findPromocionVigenteByProducto(idProducto, LocalDate.now());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("precioOriginal", producto.getPrecioVenta());
        resultado.put("tienePromocion", false);
        resultado.put("precioFinal", producto.getPrecioVenta());
        resultado.put("descuento", BigDecimal.ZERO);
        resultado.put("descripcionPromo", "");

        if (!promociones.isEmpty()) {
            Promocion promo = promociones.get(0);
            BigDecimal precio = producto.getPrecioVenta();
            BigDecimal descuento = BigDecimal.ZERO;

            switch (promo.getTipo()) {
                case PORCENTAJE:
                    descuento = precio.multiply(promo.getValor())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    break;
                case MONTO_FIJO:
                    descuento = promo.getValor();
                    break;
                case DOS_X_UNO:
                    descuento = BigDecimal.ZERO;
                    break;
            }

            resultado.put("tienePromocion", true);
            resultado.put("precioFinal", precio.subtract(descuento));
            resultado.put("descuento", descuento);
            resultado.put("descripcionPromo", promo.getNombre());
            resultado.put("tipo", promo.getTipo().name());
        }

        return resultado;
    }
    @Override
    public List<PromocionDTO> findAll() {
        return promocionRepository.findByActivoTrue()
                .stream().map(this::toDTO).toList();
    }

    @Override
    public List<PromocionDTO> findVigentes() {
        return promocionRepository.findPromocionesVigentes(LocalDate.now())
                .stream().map(this::toDTO).toList();
    }

    @Override
    public List<PromocionDTO> findByProducto(Long idProducto) {
        return promocionRepository.findByProductoId(idProducto)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public PromocionDTO save(PromocionDTO dto) {
        return toDTO(promocionRepository.save(toEntity(dto)));
    }

    @Override
    public PromocionDTO update(Long id, PromocionDTO dto) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PROMOCION_NO_ENCONTRADA + id));
        promocion.setNombre(dto.getNombre());
        promocion.setTipo(dto.getTipo());
        promocion.setValor(dto.getValor());
        promocion.setFechaInicio(dto.getFechaInicio());
        promocion.setFechaFin(dto.getFechaFin());
        promocion.setActivo(dto.getActivo());
        return toDTO(promocionRepository.save(promocion));
    }

    @Override
    public void delete(Long id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PROMOCION_NO_ENCONTRADA + id));
        promocion.setActivo(false);
        promocionRepository.save(promocion);
    }

    private PromocionDTO toDTO(Promocion p) {
        PromocionDTO dto = new PromocionDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setTipo(p.getTipo());
        dto.setValor(p.getValor());
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFin(p.getFechaFin());
        dto.setActivo(p.getActivo());
        if (p.getProducto() != null) {
            dto.setIdProducto(p.getProducto().getId());
            dto.setNombreProducto(p.getProducto().getNombre());
        }
        return dto;
    }

    private Promocion toEntity(PromocionDTO dto) {
        Promocion promocion = new Promocion();
        promocion.setNombre(dto.getNombre());
        promocion.setTipo(dto.getTipo());
        promocion.setValor(dto.getValor());
        promocion.setFechaInicio(dto.getFechaInicio());
        promocion.setFechaFin(dto.getFechaFin());
        promocion.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        if (dto.getIdProducto() != null) {
            productoRepository.findById(dto.getIdProducto())
                    .ifPresent(promocion::setProducto);
        }
        return promocion;
    }
}
