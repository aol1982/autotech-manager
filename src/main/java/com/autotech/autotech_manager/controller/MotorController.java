package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.Motor;
import com.autotech.autotech_manager.repository.MotorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/motores")
public class MotorController {

    private final MotorRepository repo;

    public MotorController(MotorRepository repo) {
        this.repo = repo;
    }

    // GET /api/motores  -> lista todos los motores disponibles en inventario
    @GetMapping
    public List<Motor> listarDisponibles() {
        return repo.listarDisponibles();
    }

    // GET /api/motores/{id} -> consulta un motor por id
    @GetMapping("/{id}")
    public ResponseEntity<Motor> buscarPorId(@PathVariable int id) {
        return repo.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/motores/{id}/compatibilidad?marca=Toyota&model=Hilux&anio=2015
    // Valida si el motor es compatible con un vehículo concreto.
    // Este endpoint es la base del caso obligatorio de sustitución de motor.
    @GetMapping("/{id}/compatibilidad")
    public ResponseEntity<?> validarCompatibilidad(
            @PathVariable int id,
            @RequestParam String marca,
            @RequestParam String model,
            @RequestParam int anio) {

        var optMotor = repo.buscarPorId(id);
        if (optMotor.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Motor no encontrado con id " + id);
        }

        Motor motor = optMotor.get();

        // Regla 1: el motor debe tener número de serie
        if (motor.numero_serie() == null || motor.numero_serie().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("El motor no tiene número de serie registrado.");
        }

        // Regla 2: el motor debe estar disponible
        if (!"Disponible".equalsIgnoreCase(motor.estado())) {
            return ResponseEntity.badRequest()
                    .body("El motor no está disponible. Estado actual: " + motor.estado());
        }

        // Regla 3: compatibilidad declarada en el campo modelo_compatible
        // Formato esperado: "Toyota Hilux 2015" (marca + model + anio)
        String esperado = (marca + " " + model + " " + anio).trim();
        if (!esperado.equalsIgnoreCase(motor.modelo_compatible().trim())) {
            return ResponseEntity.badRequest()
                    .body("Motor incompatible. El motor es para '" + motor.modelo_compatible() +
                          "' y el vehículo es '" + esperado + "'.");
        }

        return ResponseEntity.ok("Motor compatible y disponible para " + esperado);
    }
}