package com.autotech.autotech_manager.model;

import java.time.LocalDateTime;

public record OrdenTrabajo(
    Integer id_orden,
    Integer id_vehiculo,
    LocalDateTime fecha_ingreso,
    Integer kilometraje_ingreso,
    String sintomas,
    String diagnostico_preliminar,
    Integer id_mecanico_responsable,
    String estado
) {}