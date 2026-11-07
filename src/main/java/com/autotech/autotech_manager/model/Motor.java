package com.autotech.autotech_manager.model;

public record Motor(
    Integer id_motor,
    String numero_serie,
    String modelo_compatible,
    String estado
) {}