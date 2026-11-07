package com.autotech.autotech_manager.model;

import java.math.BigDecimal;

public record Servicio(
    Integer id_servicio,
    String codigo,
    String nombre,
    String descripcion,
    Integer nivel,
    BigDecimal costo_mano_obra,
    String tiempo_estimado
) {}