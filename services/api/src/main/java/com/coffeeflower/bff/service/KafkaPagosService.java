package com.coffeeflower.bff.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Productor Kafka del BFF. Publica en el tópico "pagos" el mismo payload que
 * publicaba el antiguo servicio api (com.ssasport.api.TopicTestController),
 * de modo que los 3 consumers del stack siguen recibiendo el mensaje sin cambios.
 */
@Service
public class KafkaPagosService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic:pagos}")
    private String topic;

    @Value("${app.kafka.origen:api}")
    private String origen;

    public KafkaPagosService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica un mensaje y devuelve la confirmación (partición/offset) para el
     * endpoint GET /topic-test.
     */
    public Map<String, Object> publicar(String mensaje) throws Exception {
        Map<String, String> payload = Map.of(
                "mensaje", mensaje,
                "origen", origen,
                "fecha", Instant.now().toString());
        String json = objectMapper.writeValueAsString(payload);

        // Envío síncrono (con timeout) para poder responder con partición/offset.
        SendResult<String, String> result = kafkaTemplate.send(topic, json).get(10, TimeUnit.SECONDS);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "enviado");
        response.put("topico", topic);
        response.put("particion", result.getRecordMetadata().partition());
        response.put("offset", result.getRecordMetadata().offset());
        response.put("mensaje", mensaje);
        return response;
    }
}
