package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.OrdenTrabajo;
import com.autotech.autotech_manager.repository.OrdenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenRepository repo;
    private final JdbcTemplate jdbc;

    public OrdenController(OrdenRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<OrdenTrabajo> listar() {
        return repo.listar();
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody OrdenRequest req) {
        try {
            Integer activo = jdbc.queryForObject("SELECT activo FROM Mecanicos WHERE id_mecanico = ?", Integer.class, req.id_mecanico_responsable());
            if (activo == null || activo == 0) {
                return ResponseEntity.badRequest().body("Error: No se puede asignar un mecánico inactivo.");
            }

            repo.insertarOrden(
                req.id_vehiculo(), 
                req.kilometraje_ingreso(), 
                req.sintomas(), 
                req.diagnostico_preliminar(), 
                req.id_mecanico_responsable()
            );
            return ResponseEntity.ok("Orden creada exitosamente en estado 'Recibida'.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear la orden: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstado(@PathVariable int id, @RequestParam String nuevoEstado) {
        var ordenOpt = repo.buscarPorId(id);
        if (ordenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String estadoActual = ordenOpt.get().estado();
        
        if (estadoActual.equals("Finalizada") || estadoActual.equals("Cancelada")) {
            return ResponseEntity.badRequest().body("La orden ya está cerrada y no puede modificarse.");
        }

        if (nuevoEstado.equals("Finalizada")) {
            Integer tieneCambioMotor = jdbc.queryForObject(
                "SELECT COUNT(*) FROM DetalleOrden d INNER JOIN Servicios s ON d.id_servicio = s.id_servicio WHERE d.id_orden = ? AND s.nombre LIKE '%Cambio de Motor%'", 
                Integer.class, id);
            
            if (tieneCambioMotor != null && tieneCambioMotor > 0) {
                Integer controles = jdbc.queryForObject("SELECT COUNT(*) FROM ControlesCalidad WHERE id_orden = ? AND resultado = 'Aprobado'", Integer.class, id);
                if (controles == null || controles == 0) {
                    return ResponseEntity.badRequest().body("Error: No se puede finalizar la orden. Falta el Control de Calidad aprobado para el cambio de motor.");
                }
            }
        }
        
        List<String> estadosValidos = List.of(
            "Recibida", "Diagnostico", "Entrega Presupuesto", 
            "Aprobación", "Solicitud y Envío Materiales", 
            "Repuestos en Proceso", "Finalizada", "Cancelada"
        );
        
        if (!estadosValidos.contains(nuevoEstado)) {
            return ResponseEntity.badRequest().body("Estado no válido.");
        }

        repo.actualizarEstado(id, nuevoEstado);
        return ResponseEntity.ok("Estado actualizado a: " + nuevoEstado);
    }

    @PutMapping("/{id}/mecanico")
    public ResponseEntity<String> asignarMecanico(@PathVariable int id, @RequestParam int idMecanico) {
        try {
            Integer activo = jdbc.queryForObject("SELECT activo FROM Mecanicos WHERE id_mecanico = ?", Integer.class, idMecanico);
            if (activo == null || activo == 0) {
                return ResponseEntity.badRequest().body("Error: El mecánico seleccionado está inactivo.");
            }
            
            jdbc.update("UPDATE OrdenesTrabajo SET id_mecanico_responsable = ? WHERE id_orden = ?", idMecanico, id);
            return ResponseEntity.ok("Mecánico asignado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al asignar mecánico: " + e.getMessage());
        }
    }
}