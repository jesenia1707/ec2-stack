
package com.coffeeflower.bff.controller;

import com.coffeeflower.bff.service.InformeComprasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class InformeComprasController {

    private final InformeComprasService informeService;

    public InformeComprasController(
            InformeComprasService informeService) {
        this.informeService = informeService;
    }

    @PostMapping("/compras")
    public ResponseEntity<Map<String, Object>> generarInformeMensual() {
        try {
            Map<String, Object> resultado = informeService.generarYEnviarInformeMensual();

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "mensaje",
                            "No se pudo generar o enviar el informe mensual.",
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Error interno del servidor."));
        }
    }
}
