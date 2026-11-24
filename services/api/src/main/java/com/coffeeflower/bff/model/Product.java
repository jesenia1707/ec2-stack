package com.coffeeflower.bff.model;

import java.math.BigDecimal;

public record Product(Long id, String nombre, String descripcion, BigDecimal precio, String categoria, String emoji) {}
