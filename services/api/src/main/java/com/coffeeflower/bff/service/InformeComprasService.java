package com.coffeeflower.bff.service;

import com.coffeeflower.bff.model.PedidoEntity;
import com.coffeeflower.bff.model.PedidoItemEntity;
import com.coffeeflower.bff.repository.PedidoItemRepository;
import com.coffeeflower.bff.repository.PedidoRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InformeComprasService {

        private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");

        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("MM/yyyy");

        private final PedidoRepository pedidoRepository;
        private final PedidoItemRepository itemRepository;
        private final SnsClient snsClient;
        private final ObjectMapper objectMapper;

        @Value("${AWS_SNS_TOPIC_ARN:${SNS_TOPIC_ARN:}}")
        private String topicArn;

        public InformeComprasService(
                        PedidoRepository pedidoRepository,
                        PedidoItemRepository itemRepository,
                        SnsClient snsClient,
                        ObjectMapper objectMapper) {

                this.pedidoRepository = pedidoRepository;
                this.itemRepository = itemRepository;
                this.snsClient = snsClient;
                this.objectMapper = objectMapper;
        }

        /**
         * Calcula el resumen mensual de ventas y publica una solicitud
         * JSON en SNS para que la Lambda genere y envíe el informe.
         */
        @Transactional(readOnly = true)
        public Map<String, Object> generarYEnviarInformeMensual()
                        throws Exception {

                // 1. Verificar la configuración de SNS.
                if (topicArn == null || topicArn.isBlank()) {
                        throw new IllegalStateException(
                                        "Falta configurar AWS_SNS_TOPIC_ARN.");
                }

                // 2. Definir el periodo mensual en la zona horaria de Chile.
                LocalDate hoy = LocalDate.now(ZONA_CHILE);
                LocalDate inicioMes = hoy.withDayOfMonth(1);
                LocalDate inicioMesSiguiente = inicioMes.plusMonths(1);

                Instant desde = inicioMes
                                .atStartOfDay(ZONA_CHILE)
                                .toInstant();

                Instant hasta = inicioMesSiguiente
                                .atStartOfDay(ZONA_CHILE)
                                .toInstant();

                // 3. Obtener los pedidos del mes.
                List<PedidoEntity> pedidosMes = pedidoRepository.findAll()
                                .stream()
                                .filter(pedido -> pedido.getCreadoEn() != null)
                                .filter(pedido -> {
                                        Instant fecha = pedido.getCreadoEn();

                                        return !fecha.isBefore(desde)
                                                        && fecha.isBefore(hasta);
                                })
                                .filter(pedido -> pedido.getEstado() == null
                                                || (!pedido.getEstado()
                                                                .equalsIgnoreCase("CANCELADO")
                                                                && !pedido.getEstado()
                                                                                .equalsIgnoreCase("CANCELLED")))
                                .toList();

                // 4. Calcular el total de ventas.
                BigDecimal totalVentas = pedidosMes.stream()
                                .map(PedidoEntity::getTotal)
                                .filter(total -> total != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 5. Calcular las unidades vendidas.
                int totalUnidades = 0;

                for (PedidoEntity pedido : pedidosMes) {
                        List<PedidoItemEntity> items = itemRepository.findByPedidoId(pedido.getId());

                        totalUnidades += items.stream()
                                        .mapToInt(item -> item.getCantidad() == null
                                                        ? 0
                                                        : item.getCantidad())
                                        .sum();
                }

                // 6. Formatear los valores monetarios.
                NumberFormat formatoCLP = NumberFormat.getNumberInstance(
                                Locale.forLanguageTag("es-CL"));

                formatoCLP.setMaximumFractionDigits(0);
                formatoCLP.setMinimumFractionDigits(0);

                String totalFormateado = formatoCLP.format(totalVentas);
                String periodo = inicioMes.format(FORMATO_PERIODO);
                String fechaInicio = inicioMes.format(FORMATO_FECHA);
                String fechaFin = hoy.format(FORMATO_FECHA);

                // 7. Preparar los parámetros que usará la Lambda.
                Map<String, Object> params = new HashMap<>();
                params.put("desde", inicioMes.toString());
                params.put("hasta", hoy.toString());

                // 8. Crear el cuerpo de la solicitud con los campos obligatorios.
                Map<String, Object> body = new HashMap<>();
                body.put("requestId", UUID.randomUUID().toString());
                body.put("requester", "coffee-flower-bff");
                body.put("reportType", "monthly");
                body.put("params", params);

                Map<String, Object> mensajeJson = new HashMap<>();
                mensajeJson.put("type", "report.requested");
                mensajeJson.put("body", body);

                String mensajeSerializado;

                try {
                        mensajeSerializado = objectMapper.writeValueAsString(mensajeJson);
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException(
                                        "No fue posible serializar la solicitud del informe.",
                                        e);
                }

                // 9. Publicar la solicitud JSON en SNS.
                PublishRequest solicitud = PublishRequest.builder()
                                .topicArn(topicArn)
                                .subject(
                                                "Coffee Flower - Solicitud de informe mensual - "
                                                                + periodo)
                                .message(mensajeSerializado)
                                .build();

                PublishResponse respuesta = snsClient.publish(solicitud);

                // 10. Devolver el resultado del endpoint.
                Map<String, Object> resultado = new HashMap<>();

                resultado.put(
                                "mensaje",
                                "Solicitud de informe publicada correctamente en Amazon SNS.");

                resultado.put("periodo", periodo);
                resultado.put("fechaInicio", fechaInicio);
                resultado.put("fechaFin", fechaFin);
                resultado.put("cantidadPedidos", pedidosMes.size());
                resultado.put("unidadesVendidas", totalUnidades);
                resultado.put("totalVentas", totalVentas);
                resultado.put("totalVentasFormateado", totalFormateado);
                resultado.put("snsMessageId", respuesta.messageId());

                return resultado;
        }

        /**
         * Compatibilidad con controladores que utilizan generarYEnviar().
         */
        @Transactional(readOnly = true)
        public int generarYEnviar() throws Exception {
                Map<String, Object> resultado = generarYEnviarInformeMensual();

                return ((Number) resultado.get("cantidadPedidos")).intValue();
        }
}