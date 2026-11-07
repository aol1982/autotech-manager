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

    @GetMapping("/{id}")
    public ResponseEntity<OrdenTrabajo> buscar(@PathVariable int id) {
        return repo.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody OrdenRequest req) {
        try {
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
            jdbc.update("UPDATE OrdenesTrabajo SET id_mecanico_responsable = ? WHERE id_orden = ?", idMecanico, id);
            return ResponseEntity.ok("Mecánico asignado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al asignar mecánico: " + e.getMessage());
        }
    }
}