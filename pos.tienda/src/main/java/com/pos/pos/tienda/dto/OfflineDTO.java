package com.pos.pos.tienda.dto;

import com.pos.pos.tienda.model.SesionOffline;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OfflineDTO {
    private OfflineDTO() {}

    public record Preparar(@NotBlank @Pattern(regexp = "[a-fA-F0-9-]{36}") String dispositivo) {}
    public record Sesion(String id, String dispositivo, Long usuario, Long turno,
                         String folioTurno, Instant vence, boolean cerrada, long secuencia,
                         BigDecimal efectivo, List<SesionOffline.Articulo> productos) {}
    public enum Tipo { VENTA, APORTACION, GASTO, RETIRO, CIERRE }
    public record Linea(@NotNull @Positive Long idProducto, @Positive int cantidad) {}
    public record Operacion(
            @NotBlank @Pattern(regexp = "[a-fA-F0-9-]{36}") String id,
            @NotBlank String sesion,
            @NotBlank String dispositivo,
            @Positive long secuencia,
            @NotNull Instant captura,
            @NotNull Tipo tipo,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal monto,
            @NotBlank @Size(max = 255) String descripcion,
            @NotNull @Size(max = 500) List<@NotNull @Valid Linea> lineas) {}
    public record Confirmacion(String id, String folio, long secuencia) {}
}
