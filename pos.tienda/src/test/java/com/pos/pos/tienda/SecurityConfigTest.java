package com.pos.pos.tienda;

import com.pos.pos.tienda.config.SecurityConfig;
import com.pos.pos.tienda.controller.ProductoController;
import com.pos.pos.tienda.security.JwtUtil;
import com.pos.pos.tienda.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductoController.class, SecurityConfigTest.SecuredEndpoints.class})
@Import({SecurityConfig.class, SecurityConfigTest.SecuredEndpoints.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void employeeCanReadProductsForCheckout() throws Exception {
        when(productoService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void employeeCannotChangeProductPricesOrStock() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/productos/1/stock?cantidad=10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void employeeCanCreateSaleButCannotCancelOrReadReports() throws Exception {
        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/ventas/1/cancelar"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/ventas/reporte"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO")
    void managerCanCancelSaleAndReadReports() throws Exception {
        mockMvc.perform(patch("/api/ventas/1/cancelar"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ventas/reporte")
                        .param("inicio", "2026-10-01T00:00:00")
                        .param("fin", "2026-10-31T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void employeeCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/devoluciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administratorCanManageUsers() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void authenticatedEmployeeCanOpenCashShift() throws Exception {
        mockMvc.perform(post("/api/caja/abrir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @RestController
    static class SecuredEndpoints {
        @PostMapping("/api/ventas")
        void createSale() {
        }

        @PatchMapping("/api/ventas/{id}/cancelar")
        void cancelSale() {
        }

        @GetMapping("/api/ventas/reporte")
        void report() {
        }

        @GetMapping("/api/usuarios")
        void users() {
        }

        @PostMapping("/api/compras")
        void createPurchase() {
        }

        @PostMapping("/api/devoluciones")
        void createReturn() {
        }

        @PostMapping("/api/caja/abrir")
        void openCashShift() {
        }
    }
}
