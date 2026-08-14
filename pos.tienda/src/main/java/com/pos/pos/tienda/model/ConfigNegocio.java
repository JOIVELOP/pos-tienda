package com.pos.pos.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "T_CONFIG_NEGOCIO")
public class ConfigNegocio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre_negocio", nullable = false, length = 150)
    private String nombreNegocio;

    @Size(max = 13)
    @Column(length = 13)
    private String rfc;

    @Size(max = 255)
    private String direccion;

    @Size(max = 15)
    @Column(length = 15)
    private String telefono;

    @Email
    @Size(max = 100)
    @Column(length = 100)
    private String email;

    @Size(max = 255)
    @Column(name = "logo_url")
    private String logoUrl;

    @Size(max = 255)
    @Column(name = "mensaje_ticket")
    private String mensajeTicket;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public void setNombreNegocio(String nombreNegocio) {
        this.nombreNegocio = nombreNegocio;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getMensajeTicket() {
        return mensajeTicket;
    }

    public void setMensajeTicket(String mensajeTicket) {
        this.mensajeTicket = mensajeTicket;
    }
}
