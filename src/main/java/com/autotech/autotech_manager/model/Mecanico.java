package com.autotech.autotech_manager.model;

public record Mecanico(
    Integer id_mecanico,
    String nombre,
    String especialidad,
    Integer activo
) {}