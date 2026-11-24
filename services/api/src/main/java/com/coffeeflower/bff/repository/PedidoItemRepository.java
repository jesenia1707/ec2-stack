package com.coffeeflower.bff.repository;

import com.coffeeflower.bff.model.PedidoItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoItemRepository extends JpaRepository<PedidoItemEntity, Long> {

    List<PedidoItemEntity> findByPedidoId(Long pedidoId);
}