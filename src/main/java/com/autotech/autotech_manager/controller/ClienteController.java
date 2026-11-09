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

    @GetMapping("/{id}/vehiculos")
    public List<Map<String, Object>> listarVehiculosCliente(@PathVariable int id) {
        return jdbc.queryForList("SELECT * FROM Vehiculos WHERE id_cliente = ?", id);
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody Map<String, Object> body) {
        try {
            String sqlCliente = "INSERT INTO Clientes (nombre, telefono, correo, direccion, activo) VALUES (?, ?, ?, ?, 1)";
            jdbc.update(sqlCliente, body.get("nombre"), body.get("telefono"), 
                body.get("correo") != null ? body.get("correo") : "N/A", 
                body.get("direccion") != null ? body.get("direccion") : "N/A");

            Integer idCliente = jdbc.queryForObject("SELECT TOP 1 id_cliente FROM Clientes ORDER BY id_cliente DESC", Integer.class);

            if (body.get("placa") != null && !body.get("placa").toString().isEmpty()) {
                String sqlVehiculo = "INSERT INTO Vehiculos (id_cliente, placa, vin_chasis, marca, model, anio, color, kilometraje, combustible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbc.update(sqlVehiculo, idCliente, body.get("placa"), body.get("vin_chasis"), body.get("marca"), 
                    body.get("model"), body.get("anio"), body.get("color"), body.get("kilometraje"), body.get("combustible"));
            }
            return ResponseEntity.ok("Cliente y Vehículo registrados exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String sql = "UPDATE Clientes SET nombre = ?, telefono = ?, correo = ?, direccion = ? WHERE id_cliente = ?";
            jdbc.update(sql, body.get("nombre"), body.get("telefono"), body.get("correo"), body.get("direccion"), id);
            return ResponseEntity.ok("Cliente actualizado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/desactivar")
    public ResponseEntity<String> desactivar(@PathVariable int id) {
        try {
            jdbc.update("UPDATE Clientes SET activo = 0 WHERE id_cliente = ?", id);
            return ResponseEntity.ok("Cliente desactivado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al desactivar: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/vehiculos")
    public ResponseEntity<String> agregarVehiculo(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String sql = "INSERT INTO Vehiculos (id_cliente, placa, vin_chasis, marca, model, anio, color, kilometraje, combustible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbc.update(sql, id, body.get("placa"), body.get("vin_chasis"), body.get("marca"), 
                body.get("model"), body.get("anio"), body.get("color"), body.get("kilometraje"), body.get("combustible"));
            return ResponseEntity.ok("Vehículo agregado al cliente correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al agregar vehículo: " + e.getMessage());
        }
    }
}