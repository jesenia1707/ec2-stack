package com.coffeeflower.bff.controller;

import com.coffeeflower.bff.model.PedidoEntity;
import com.coffeeflower.bff.model.PedidoItemEntity;
import com.coffeeflower.bff.repository.PedidoRepository;
import com.coffeeflower.bff.repository.PedidoItemRepository;
import com.coffeeflower.bff.service.PedidoService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosController {

        private final PedidoService pedidoService;
        private final PedidoRepository pedidoRepository;
        private final PedidoItemRepository pedidoItemRepository;

        public PedidosController(
                        PedidoService pedidoService,
                        PedidoRepository pedidoRepository,
                        PedidoItemRepository pedidoItemRepository) {
                this.pedidoService = pedidoService;
                this.pedidoRepository = pedidoRepository;
                this.pedidoItemRepository = pedidoItemRepository;
        }

        @GetMapping
        public List<PedidoResponse> listarPedidos() {
                return pedidoRepository.findAll()
                                .stream()
                                .map(pedido -> {
                                        List<PedidoItemRequest> items = pedidoItemRepository
                                                        .findByPedidoId(pedido.getId())
                                                        .stream()
                                                        .map(item -> new PedidoItemRequest(
                                                                        item.getProductoId(),
                                                                        item.getNombre(),
                                                                        item.getCantidad(),
                                                                        item.getPrecio()))
                                                        .toList();

                                        return new PedidoResponse(
                                                        pedido.getId(),
                                                        pedido.getCliente(),
                                                        items,
                                                        pedido.getTotal(),
                                                        pedido.getEstado(),
                                                        pedido.getCreadoEn() != null
                                                                        ? pedido.getCreadoEn().toString()
                                                                        : null);
                                })
                                .toList();
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public PedidoResponse crearPedido(
                        @RequestBody PedidoRequest request) {

                if (request.cliente() == null
                                || request.cliente().isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "El cliente es obligatorio");
                }

                if (request.items() == null || request.items().isEmpty()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "El pedido debe contener productos");
                }

                if (request.total() == null || request.total().signum() < 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "El total del pedido no es válido");
                }

                List<PedidoItemEntity> detalles = request.items().stream()
                                .map(item -> {
                                        if (item.nombre() == null
                                                        || item.nombre().isBlank()
                                                        || item.cantidad() <= 0
                                                        || item.precio() == null
                                                        || item.precio().signum() < 0) {
                                                throw new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                "Hay productos con datos inválidos");
                                        }

                                        PedidoItemEntity detalle = new PedidoItemEntity();
                                        detalle.setProductoId(convertirId(item.productoId()));
                                        detalle.setNombre(item.nombre());
                                        detalle.setCantidad(item.cantidad());
                                        detalle.setPrecio(item.precio());
                                        return detalle;
                                })
                                .toList();

                try {
                        PedidoEntity pedido = pedidoService.crearPedido(
                                        request.cliente(),
                                        detalles,
                                        request.total());

                        return new PedidoResponse(
                                        pedido.getId(),
                                        pedido.getCliente(),
                                        request.items(),
                                        pedido.getTotal(),
                                        pedido.getEstado(),
                                        pedido.getCreadoEn() != null
                                                        ? pedido.getCreadoEn().toString()
                                                        : null);

                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        e.getMessage());
                }
        }

        private Long convertirId(Object id) {
                if (id == null || id.toString().isBlank()) {
                        return null;
                }

                try {
                        return Long.valueOf(id.toString());
                } catch (NumberFormatException e) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "El identificador del producto no es válido");
                }
        }

        public record PedidoItemRequest(
                        Object productoId,
                        String nombre,
                        int cantidad,
                        BigDecimal precio) {
        }

        public record PedidoRequest(
                        String cliente,
                        List<PedidoItemRequest> items,
                        BigDecimal total) {
        }

        public record PedidoResponse(
                        Long id,
                        String cliente,
                        List<PedidoItemRequest> items,
                        BigDecimal total,
                        String estado,
                        String creadoEn) {
        }
}
