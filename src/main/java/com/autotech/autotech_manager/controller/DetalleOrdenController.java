package com.autotech.autotech_manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/detalles")
public class DetalleOrdenController {

    private final JdbcTemplate jdbc;

    public DetalleOrdenController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/{idOrden}")
    public List<Map<String, Object>> listarDetalles(@PathVariable int idOrden) {
        String sql = """
            SELECT d.id_detalle, d.id_servicio, s.nombre AS servicio, d.costo_aplicado 
            FROM DetalleOrden d
            INNER JOIN Servicios s ON d.id_servicio = s.id_servicio
            WHERE d.id_orden = ?
            """;
        return jdbc.queryForList(sql, idOrden);
    }

    @PostMapping("/multiple")
    public ResponseEntity<String> agregarMultiples(@RequestBody Map<String, Object> body) {
        try {
            int idOrden = (int) body.get("id_orden");
            List<Integer> idServicios = (List<Integer>) body.get("id_servicios");

            for (Integer idServicio : idServicios) {
                Double costo = jdbc.queryForObject("SELECT costo_mano_obra FROM Servicios WHERE id_servicio = ?", Double.class, idServicio);
                jdbc.update("INSERT INTO DetalleOrden (id_orden, id_servicio, costo_aplicado) VALUES (?, ?, ?)", idOrden, idServicio, costo);
            }
            actualizarPresupuesto(idOrden);
            return ResponseEntity.ok("Servicios agregados a la orden.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/repuesto-individual")
    public ResponseEntity<String> agregarRepuestoIndividual(@RequestBody Map<String, Object> body) {
        try {
            int idOrden = (int) body.get("id_orden");
            String nombreRepuesto = (String) body.get("nombre");
            Double costo = Double.parseDouble(body.get("costo").toString());

            // 1. Registrar el repuesto en la tabla Repuestos si no existe
            Integer existeRepuesto = jdbc.queryForObject("SELECT COUNT(*) FROM Repuestos WHERE nombre = ?", Integer.class, nombreRepuesto);
            if (existeRepuesto != null && existeRepuesto == 0) {
                jdbc.update("INSERT INTO Repuestos (nombre, costo, stock_actual) VALUES (?, ?, 10)", nombreRepuesto, costo);
            }

            // 2. Buscar un servicio "Genérico" para asociar el repuesto (evita error de llave foránea)
            Integer idServicioGenerico = jdbc.query(
                "SELECT TOP 1 id_servicio FROM Servicios ORDER BY id_servicio ASC", 
                rs -> rs.next() ? rs.getInt("id_servicio") : null
            );

            if (idServicioGenerico == null) {
                return ResponseEntity.badRequest().body("Error: No hay servicios registrados para asociar el repuesto.");
            }

            // 3. Insertar en DetalleOrden
            jdbc.update("INSERT INTO DetalleOrden (id_orden, id_servicio, costo_aplicado) VALUES (?, ?, ?)", idOrden, idServicioGenerico, costo);
            
            actualizarPresupuesto(idOrden);
            return ResponseEntity.ok("Repuesto individual agregado.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al pedir el repuesto: " + e.getMessage());
        }
    }

    @DeleteMapping("/{idDetalle}")
    public ResponseEntity<String> eliminarDetalle(@PathVariable int idDetalle) {
        try {
            Integer idOrden = jdbc.queryForObject("SELECT id_orden FROM DetalleOrden WHERE id_detalle = ?", Integer.class, idDetalle);
            jdbc.update("DELETE FROM DetalleOrden WHERE id_detalle = ?", idDetalle);
            if (idOrden != null) actualizarPresupuesto(idOrden);
            return ResponseEntity.ok("Ítem eliminado.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/aplicar-descuento")
    public ResponseEntity<String> aplicarDescuento(@RequestBody Map<String, Object> body) {
        try {
            int idOrden = (int) body.get("id_orden");
            double descuento = Double.parseDouble(body.get("descuento").toString());

            Double subtotalActual = jdbc.queryForObject(
                "SELECT ISNULL(SUM(costo_aplicado), 0) FROM DetalleOrden WHERE id_orden = ?", 
                Double.class, idOrden);

            double impuestos = (subtotalActual - descuento) * 0.15;
            double totalFinal = (subtotalActual - descuento) + impuestos;

            Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM Presupuestos WHERE id_orden = ?", Integer.class, idOrden);
            
            if (existe != null && existe > 0) {
                jdbc.update("UPDATE Presupuestos SET subtotal = ?, descuentos = ?, impuestos = ?, total = ? WHERE id_orden = ?", 
                    subtotalActual, descuento, impuestos, totalFinal, idOrden);
            } else {
                jdbc.update("INSERT INTO Presupuestos (id_orden, subtotal, descuentos, impuestos, total, estado_aprobacion) VALUES (?, ?, ?, ?, ?, 'Pendiente')", 
                    idOrden, subtotalActual, descuento, impuestos, totalFinal);
            }

            return ResponseEntity.ok("Descuento aplicado exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al aplicar descuento: " + e.getMessage());
        }
    }

    private void actualizarPresupuesto(int idOrden) {
        Double subtotal = jdbc.queryForObject("SELECT ISNULL(SUM(costo_aplicado), 0) FROM DetalleOrden WHERE id_orden = ?", Double.class, idOrden);
        
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM Presupuestos WHERE id_orden = ?", Integer.class, idOrden);
        if (existe != null && existe > 0) {
            Double descuento = jdbc.queryForObject("SELECT descuentos FROM Presupuestos WHERE id_orden = ?", Double.class, idOrden);
            if (descuento == null) descuento = 0.0;
            double impuestos = (subtotal - descuento) * 0.15;
            double total = (subtotal - descuento) + impuestos;
            jdbc.update("UPDATE Presupuestos SET subtotal = ?, impuestos = ?, total = ? WHERE id_orden = ?", subtotal, impuestos, total, idOrden);
        } else {
            double impuestos = subtotal * 0.15;
            double total = subtotal + impuestos;
            jdbc.update("INSERT INTO Presupuestos (id_orden, subtotal, descuentos, impuestos, total, estado_aprobacion) VALUES (?, ?, 0, ?, ?, 'Pendiente')", idOrden, subtotal, impuestos, total);
        }
    }
}