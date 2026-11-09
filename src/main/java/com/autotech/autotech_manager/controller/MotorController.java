package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.Motor;
import com.autotech.autotech_manager.repository.MotorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/motores")
public class MotorController {

    private final MotorRepository repo;
    private final JdbcTemplate jdbc;

    public MotorController(MotorRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Motor> listarDisponibles() {
        return repo.listarDisponibles();
    }

    @PostMapping
    public ResponseEntity<String> crearMotor(@RequestBody Map<String, Object> body) {
        try {
            String sql = "INSERT INTO Motores (numero_serie, modelo_compatible, estado) VALUES (?, ?, 'Disponible')";
            jdbc.update(sql, body.get("numero_serie"), body.get("modelo_compatible"));
            return ResponseEntity.ok("Motor ingresado al inventario exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar el motor: " + e.getMessage());
        }
    }
}