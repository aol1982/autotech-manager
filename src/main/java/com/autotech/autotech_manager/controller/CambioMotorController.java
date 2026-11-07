package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.Motor;
import com.autotech.autotech_manager.model.Vehiculo;
import com.autotech.autotech_manager.repository.MotorRepository;
import com.autotech.autotech_manager.repository.VehiculoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cambios-motor")
public class CambioMotorController {

    private final JdbcTemplate jdbc;
    private final MotorRepository motorRepo;
    private final VehiculoRepository vehiculoRepo;

    public CambioMotorController(JdbcTemplate jdbc, MotorRepository motorRepo, VehiculoRepository vehiculoRepo) {
        this.jdbc = jdbc;
        this.motorRepo = motorRepo;
        this.vehiculoRepo = vehiculoRepo;
    }

    @PostMapping
    @Transactional // Si falla algo, todo se revierte en la base de datos
    public ResponseEntity<String> registrarCambio(@RequestBody CambioMotorRequest req) {
        try {
            // 1. Validar que el motor nuevo exista y esté disponible
            Motor motorNuevo = motorRepo.buscarPorId(req.id_motor_nuevo())
                    .orElseThrow(() -> new RuntimeException("El motor seleccionado no existe en el inventario."));

            if (!"Disponible".equalsIgnoreCase(motorNuevo.estado())) {
                throw new RuntimeException("El motor no está disponible. Estado actual: " + motorNuevo.estado());
            }

            if (motorNuevo.numero_serie() == null || motorNuevo.numero_serie().isBlank()) {
                throw new RuntimeException("El motor no tiene número de serie registrado.");
            }

            // 2. Validar compatibilidad con el vehículo (Caso Toyota Hilux 2015)
            Vehiculo vehiculo = vehiculoRepo.buscarPorId(req.id_vehiculo())
                    .orElseThrow(() -> new RuntimeException("El vehículo no existe."));

            String vehiculoStr = vehiculo.marca() + " " + vehiculo.model() + " " + vehiculo.anio();
            if (!vehiculoStr.equalsIgnoreCase(motorNuevo.modelo_compatible())) {
                throw new RuntimeException("Motor incompatible. El motor es para '" + 
                    motorNuevo.modelo_compatible() + "' y el vehículo es un '" + vehiculoStr + "'.");
            }

            // 3. Registrar el cambio en la tabla CambiosMotor (trazabilidad)
            String sql = """
                INSERT INTO CambiosMotor 
                (id_orden, id_vehiculo, serie_motor_anterior, id_motor_nuevo, destino_motor_retirado, resultados_pruebas) 
                VALUES (?, ?, ?, ?, ?, ?)
                """;
            jdbc.update(sql, req.id_orden(), req.id_vehiculo(), req.serie_motor_anterior(), 
                        req.id_motor_nuevo(), req.destino_motor_retirado(), req.resultados_pruebas());

            // 4. Actualizar el estado del motor a 'Instalado' en el inventario
            motorRepo.actualizarEstadoMotor(req.id_motor_nuevo(), "Instalado");

            return ResponseEntity.ok("Cambio de motor registrado exitosamente. Trazabilidad completada.");
            
        } catch (RuntimeException e) {
            // Si hay error, la anotación @Transactional hace el ROLLBACK automáticamente
            return ResponseEntity.badRequest().body("Error al registrar el cambio: " + e.getMessage());
        }
    }
}