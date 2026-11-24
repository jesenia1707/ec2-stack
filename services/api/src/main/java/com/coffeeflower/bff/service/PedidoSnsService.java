package com.coffeeflower.bff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class PedidoSnsService {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${SNS_TOPIC_ARN:}")
    private String topicArn;

    public PedidoSnsService(
            SnsClient snsClient,
            ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public void publicarPedido(Object pedido) {
        if (topicArn == null || topicArn.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar SNS_TOPIC_ARN");
        }

        try {
            Map<String, Object> evento = new HashMap<>();
            evento.put("type", "pedido.creado");
            evento.put("body", pedido);

            String mensaje = objectMapper.writeValueAsString(evento);

            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject("Nuevo pedido Coffee Flower")
                    .message(mensaje)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al publicar el pedido en AWS SNS", e);
        }
    }
}