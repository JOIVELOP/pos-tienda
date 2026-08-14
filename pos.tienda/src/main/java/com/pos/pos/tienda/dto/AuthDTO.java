package com.pos.pos.tienda.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDTO {
    private AuthDTO() {

    }

    public static class Login {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }


    public static class Response {
        private String token;
        private String username;
        private Long id;
        private String nombre;
        private String apellido;
        private String rol;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getApellido() {
            return apellido;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setApellido(String apellido) {
            this.apellido = apellido;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getRol() {
            return rol;
        }

        public void setRol(String rol) {
            this.rol = rol;
        }

        public Response(String token, Long id, String username, String nombre, String apellido, String rol) {
            this.token = token;
            this.id = id;
            this.username = username;
            this.nombre = nombre;
            this.apellido = apellido;
            this.rol = rol;
        }
    }
}
