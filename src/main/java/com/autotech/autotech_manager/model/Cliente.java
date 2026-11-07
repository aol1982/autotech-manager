package com.autotech.autotech_manager.model;

public record Cliente(
    Integer id_cliente,
    String nombre,
    String telefono,
    String correo,
    String direccion,
    Integer activo
) {}