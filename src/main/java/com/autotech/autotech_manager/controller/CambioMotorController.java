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
    @Transactional
    public ResponseEntity<String> registrarCambio(@RequestBody CambioMotorRequest req) {
        try {
            // 1. Validaciones defensivas para evitar NullPointerException
            if (req.id_orden() == null) {
                return ResponseEntity.badRequest().body("Error: El ID de la orden es obligatorio.");
            }
            if (req.id_vehiculo() == null) {
                return ResponseEntity.badRequest().body("Error: El ID del vehículo es obligatorio (no fue enviado por el frontend).");
            }
            if (req.id_motor_nuevo() == null) {
                return ResponseEntity.badRequest().body("Error: Debe seleccionar un motor nuevo.");
            }

            // 2. Validar que el motor exista y esté disponible
            Motor motorNuevo = motorRepo.buscarPorId(req.id_motor_nuevo())
                    .orElseThrow(() -> new RuntimeException("El motor seleccionado no existe en el inventario."));

            if (!"Disponible".equalsIgnoreCase(motorNuevo.estado())) {
                throw new RuntimeException("El motor no está disponible. Estado actual: " + motorNuevo.estado());
            }

            if (motorNuevo.numero_serie() == null || motorNuevo.numero_serie().isBlank()) {
                throw new RuntimeException("El motor no tiene número de serie registrado.");
            }

            // 3. Validar que el vehículo exista y sea compatible
            Vehiculo vehiculo = vehiculoRepo.buscarPorId(req.id_vehiculo())
                    .orElseThrow(() -> new RuntimeException("El vehículo no existe en la base de datos."));

            String vehiculoStr = vehiculo.marca() + " " + vehiculo.model() + " " + vehiculo.anio();
            if (!vehiculoStr.equalsIgnoreCase(motorNuevo.modelo_compatible())) {
                throw new RuntimeException("Motor incompatible. El motor es para '" + 
                    motorNuevo.modelo_compatible() + "' y el vehículo es un '" + vehiculoStr + "'.");
            }

            // 4. Insertar el cambio de motor
            String sql = """
                INSERT INTO CambiosMotor 
                (id_orden, id_vehiculo, serie_motor_anterior, id_motor_nuevo, destino_motor_retirado, resultados_pruebas) 
                VALUES (?, ?, ?, ?, ?, ?)
                """;
            jdbc.update(sql, req.id_orden(), req.id_vehiculo(), req.serie_motor_anterior(), 
                        req.id_motor_nuevo(), req.destino_motor_retirado(), req.resultados_pruebas());

            // 5. Actualizar inventario y generar garantía
            motorRepo.actualizarEstadoMotor(req.id_motor_nuevo(), "Instalado");

            jdbc.update("INSERT INTO Garantias (id_orden, descripcion) VALUES (?, ?)", 
                req.id_orden(), "Garantía por sustitución de motor. Serie: " + motorNuevo.numero_serie());

            return ResponseEntity.ok("Cambio de motor registrado y garantía generada. Trazabilidad completada.");
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error inesperado: " + e.getMessage());
        }
    }

    @PostMapping("/control-calidad")
    public ResponseEntity<String> registrarControlCalidad(@RequestBody CambioMotorRequest req) {
        try {
            if (req.id_orden() == null) {
                return ResponseEntity.badRequest().body("Error: El ID de la orden es obligatorio.");
            }

            Integer cambios = jdbc.queryForObject("SELECT COUNT(*) FROM CambiosMotor WHERE id_orden = ?", Integer.class, req.id_orden());
            if (cambios == null || cambios == 0) {
                return ResponseEntity.badRequest().body("No se puede aprobar el control de calidad. No hay cambio de motor registrado.");
            }

            jdbc.update("INSERT INTO ControlesCalidad (id_orden, resultado, observaciones) VALUES (?, 'Aprobado', 'Pruebas de compresión y escáner OK.')", req.id_orden());
            
            return ResponseEntity.ok("Control de calidad aprobado. La orden puede ser finalizada.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}