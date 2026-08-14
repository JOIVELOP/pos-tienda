package com.pos.pos.tienda.dto;


import java.math.BigDecimal;
import java.util.List;

public class ReporteDTO {
    private BigDecimal totalVentas;
    private Long cantidadVentas;
    private BigDecimal promedioVenta;
    private List<ProductoVendidoDTO> productosMasVendidos;
    private List<VentaDiariaDTO> ventasPorDia;
    private List<VentaHorariaDTO> ventasPorHora;

    public List<VentaHorariaDTO> getVentasPorHora() {
        return ventasPorHora;
    }

    public void setVentasPorHora(List<VentaHorariaDTO> ventasPorHora) {
        this.ventasPorHora = ventasPorHora;
    }

    public static class ProductoVendidoDTO {
        private String nombreProducto;
        private Long cantidadVendida;
        private BigDecimal totalGenerado;

        public String getNombreProducto() {
            return nombreProducto;
        }

        public void setNombreProducto(String nombreProducto) {
            this.nombreProducto = nombreProducto;
        }

        public Long getCantidadVendida() {
            return cantidadVendida;
        }

        public void setCantidadVendida(Long cantidadVendida) {
            this.cantidadVendida = cantidadVendida;
        }

        public BigDecimal getTotalGenerado() {
            return totalGenerado;
        }

        public void setTotalGenerado(BigDecimal totalGenerado) {
            this.totalGenerado = totalGenerado;
        }
    }

    public static class VentaHorariaDTO {
        private Integer hora;
        private Long cantidadVentas;
        private BigDecimal totalVentas;

        public Integer getHora() {
            return hora;
        }

        public void setHora(Integer hora) {
            this.hora = hora;
        }

        public Long getCantidadVentas() {
            return cantidadVentas;
        }

        public void setCantidadVentas(Long cantidadVentas) {
            this.cantidadVentas = cantidadVentas;
        }

        public BigDecimal getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(BigDecimal totalVentas) {
            this.totalVentas = totalVentas;
        }
    }

    public static class VentaDiariaDTO {
        private String fecha;
        private Long cantidadVentas;
        private BigDecimal totalVentas;

        public String getFecha() {
            return fecha;
        }

        public void setFecha(String fecha) {
            this.fecha = fecha;
        }

        public Long getCantidadVentas() {
            return cantidadVentas;
        }

        public void setCantidadVentas(Long cantidadVentas) {
            this.cantidadVentas = cantidadVentas;
        }

        public BigDecimal getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(BigDecimal totalVentas) {
            this.totalVentas = totalVentas;
        }
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public Long getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(Long cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }

    public BigDecimal getPromedioVenta() {
        return promedioVenta;
    }

    public void setPromedioVenta(BigDecimal promedioVenta) {
        this.promedioVenta = promedioVenta;
    }

    public List<ProductoVendidoDTO> getProductosMasVendidos() {
        return productosMasVendidos;
    }

    public void setProductosMasVendidos(List<ProductoVendidoDTO> productosMasVendidos) {
        this.productosMasVendidos = productosMasVendidos;
    }

    public List<VentaDiariaDTO> getVentasPorDia() {
        return ventasPorDia;
    }

    public void setVentasPorDia(List<VentaDiariaDTO> ventasPorDia) {
        this.ventasPorDia = ventasPorDia;
    }
}