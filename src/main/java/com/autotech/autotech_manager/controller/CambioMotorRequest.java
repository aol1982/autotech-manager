package com.autotech.autotech_manager.controller;

public record CambioMotorRequest(
    Integer id_orden,
    Integer id_vehiculo,
    String serie_motor_anterior,
    Integer id_motor_nuevo,
    String destino_motor_retirado,
    String resultados_pruebas
) {}