package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.Cliente;
import com.autotech.autotech_manager.repository.ClienteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository repo;
    private final JdbcTemplate jdbc;

    public ClienteController(ClienteRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Cliente> listar() {
        return repo.listarActivos();
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody Map<String, Object> body) {
        try {
            // 1. Insertar Cliente
            String sqlCliente = "INSERT INTO Clientes (nombre, telefono, correo, direccion, activo) VALUES (?, ?, ?, ?, 1)";
            jdbc.update(sqlCliente, 
                body.get("nombre"), 
                body.get("telefono"), 
                body.get("correo"), 
                body.get("direccion")
            );

            // 2. Obtener el ID del cliente recién insertado
            Integer idCliente = jdbc.queryForObject("SELECT TOP 1 id_cliente FROM Clientes ORDER BY id_cliente DESC", Integer.class);

            // 3. Insertar Vehículo si se enviaron los datos
            if (body.get("placa") != null && !body.get("placa").toString().isEmpty()) {
                String sqlVehiculo = """
                    INSERT INTO Vehiculos (id_cliente, placa, vin_chasis, marca, model, anio, color, kilometraje, combustible) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                jdbc.update(sqlVehiculo,
                    idCliente,
                    body.get("placa"),
                    body.get("vin_chasis"),
                    body.get("marca"),
                    body.get("model"),
                    body.get("anio"),
                    body.get("color"),
                    body.get("kilometraje"),
                    body.get("combustible")
                );
            }
            return ResponseEntity.ok("Cliente y Vehículo registrados exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar: " + e.getMessage());
        }
    }
}