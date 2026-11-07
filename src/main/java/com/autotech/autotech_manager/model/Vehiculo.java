package com.autotech.autotech_manager.model;

public record Vehiculo(
    Integer id_vehiculo,
    Integer id_cliente,
    String placa,
    String vin_chasis,
    String marca,
    String model,
    Integer anio,
    String color,
    Integer kilometraje,
    String combustible
) {}