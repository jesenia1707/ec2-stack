package com.coffeeflower.bff.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coffeeflower.bff.service.KafkaPagosService;

/**
 * Endpoint de prueba del flujo Kafka (antes en el servicio api):
 *
 *   GET /topic-test?msj=test
 *
 * Publica en el tópico "pagos" y responde con topic/partición/offset.
 * Los 3 consumers (analiticas/notificaciones/pagos) reciben una copia (fan-out).
 */
@RestController
public class TopicTestController {

    private final KafkaPagosService kafkaPagosService;

    public TopicTestController(KafkaPagosService kafkaPagosService) {
        this.kafkaPagosService = kafkaPagosService;
    }

    @GetMapping("/topic-test")
    public Map<String, Object> topicTest(@RequestParam(defaultValue = "test") String msj) throws Exception {
        return kafkaPagosService.publicar(msj);
    }
}
