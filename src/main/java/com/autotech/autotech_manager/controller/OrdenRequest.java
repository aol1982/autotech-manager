package com.autotech.autotech_manager.controller;

public record OrdenRequest(
    Integer id_vehiculo,
    Integer kilometraje_ingreso,
    String sintomas,
    String diagnostico_preliminar,
    Integer id_mecanico_responsable
) {}