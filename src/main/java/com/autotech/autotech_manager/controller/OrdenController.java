package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.OrdenTrabajo;
import com.autotech.autotech_manager.repository.OrdenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
            // Validaciones preventivas
            if (req.id_vehiculo() == null) {
                return ResponseEntity.badRequest().body("Error: El ID del vehículo es obligatorio.");
            }
            if (req.id_mecanico_responsable() == null) {
                return ResponseEntity.badRequest().body("Error: Debe asignar un mecánico.");
            }
            if (req.kilometraje_ingreso() == null || req.kilometraje_ingreso() < 0) {
                return ResponseEntity.badRequest().body("Error: El kilometraje es inválido.");
            }

            // Verificar si el mecánico está activo
            List<Integer> activos = jdbc.queryForList("SELECT activo FROM Mecanicos WHERE id_mecanico = ?", Integer.class, req.id_mecanico_responsable());
            if (activos.isEmpty() || activos.get(0) == null || activos.get(0) == 0) {
                return ResponseEntity.badRequest().body("Error: No se puede asignar un mecánico inactivo o inexistente.");
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
        try {
            var ordenOpt = repo.buscarPorId(id);
            if (ordenOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String estadoActual = ordenOpt.get().estado();
            
            if (estadoActual.equals("Finalizada") || estadoActual.equals("Cancelada")) {
                return ResponseEntity.badRequest().body("La orden ya está cerrada y no puede modificarse.");
            }

            // Validación especial si se intenta finalizar
            if (nuevoEstado.equals("Finalizada")) {
                List<Integer> cambiosMotor = jdbc.queryForList(
                    "SELECT COUNT(*) FROM DetalleOrden d INNER JOIN Servicios s ON d.id_servicio = s.id_servicio WHERE d.id_orden = ? AND s.nombre LIKE '%Cambio de Motor%'", 
                    Integer.class, id);
                
                if (!cambiosMotor.isEmpty() && cambiosMotor.get(0) != null && cambiosMotor.get(0) > 0) {
                    List<Integer> controles = jdbc.queryForList("SELECT COUNT(*) FROM ControlesCalidad WHERE id_orden = ? AND resultado = 'Aprobado'", Integer.class, id);
                    if (controles.isEmpty() || controles.get(0) == null || controles.get(0) == 0) {
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al cambiar estado: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/mecanico")
    public ResponseEntity<String> asignarMecanico(@PathVariable int id, @RequestParam int idMecanico) {
        try {
            List<Integer> activos = jdbc.queryForList("SELECT activo FROM Mecanicos WHERE id_mecanico = ?", Integer.class, idMecanico);
            if (activos.isEmpty() || activos.get(0) == null || activos.get(0) == 0) {
                return ResponseEntity.badRequest().body("Error: El mecánico seleccionado está inactivo o no existe.");
            }
            
            jdbc.update("UPDATE OrdenesTrabajo SET id_mecanico_responsable = ? WHERE id_orden = ?", idMecanico, id);
            return ResponseEntity.ok("Mecánico asignado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al asignar mecánico: " + e.getMessage());
        }
    }
}