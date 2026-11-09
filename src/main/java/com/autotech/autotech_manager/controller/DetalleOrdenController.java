package com.autotech.autotech_manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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

    private boolean estaOrdenCerrada(int idOrden) {
        String estado = jdbc.queryForObject("SELECT estado FROM OrdenesTrabajo WHERE id_orden = ?", String.class, idOrden);
        return "Finalizada".equals(estado) || "Cancelada".equals(estado);
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
            if (estaOrdenCerrada(idOrden)) {
                return ResponseEntity.badRequest().body("No se pueden modificar los servicios de una orden finalizada o cancelada.");
            }

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
    @Transactional
    public ResponseEntity<String> agregarRepuestoIndividual(@RequestBody Map<String, Object> body) {
        try {
            int idOrden = (int) body.get("id_orden");
            if (estaOrdenCerrada(idOrden)) {
                return ResponseEntity.badRequest().body("No se pueden modificar los repuestos de una orden finalizada.");
            }

            String nombreRepuesto = (String) body.get("nombre");
            Double costo = Double.parseDouble(body.get("costo").toString());

            List<Map<String, Object>> repuestos = jdbc.queryForList("SELECT id_repuesto, stock_actual FROM Repuestos WHERE nombre = ?", nombreRepuesto);
            
            if (repuestos.isEmpty()) {
                return ResponseEntity.badRequest().body("El repuesto no existe en el inventario.");
            }
            
            int idRepuesto = ((Number) repuestos.get(0).get("id_repuesto")).intValue();
            int stockActual = ((Number) repuestos.get(0).get("stock_actual")).intValue();

            if (stockActual <= 0) {
                return ResponseEntity.badRequest().body("No hay stock suficiente para el repuesto: " + nombreRepuesto);
            }

            jdbc.update("UPDATE Repuestos SET stock_actual = stock_actual - 1 WHERE id_repuesto = ?", idRepuesto);
            jdbc.update("INSERT INTO MovimientosInventario (id_repuesto, id_orden, tipo_movimiento, cantidad, fecha) VALUES (?, ?, 'SALIDA', 1, GETDATE())", idRepuesto, idOrden);

            Integer idServicioGenerico = jdbc.query("SELECT TOP 1 id_servicio FROM Servicios ORDER BY id_servicio ASC", 
                rs -> rs.next() ? rs.getInt("id_servicio") : null);

            jdbc.update("INSERT INTO DetalleOrden (id_orden, id_servicio, costo_aplicado) VALUES (?, ?, ?)", idOrden, idServicioGenerico, costo);
            actualizarPresupuesto(idOrden);
            
            return ResponseEntity.ok("Repuesto agregado y stock actualizado.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al pedir el repuesto: " + e.getMessage());
        }
    }

    @PostMapping("/registrar-pago")
    public ResponseEntity<String> registrarPago(@RequestBody Map<String, Object> body) {
        try {
            int idOrden = (int) body.get("id_orden");
            double montoPago = Double.parseDouble(body.get("monto").toString());

            List<Map<String, Object>> presupuestos = jdbc.queryForList("SELECT total FROM Presupuestos WHERE id_orden = ?", idOrden);
            if (presupuestos.isEmpty()) {
                return ResponseEntity.badRequest().body("La orden no tiene un presupuesto generado.");
            }
            double totalPresupuesto = ((Number) presupuestos.get(0).get("total")).doubleValue();

            Double totalPagadoObj = jdbc.queryForObject("SELECT ISNULL(SUM(monto), 0) FROM Pagos WHERE id_orden = ?", Double.class, idOrden);
            double totalPagado = totalPagadoObj != null ? totalPagadoObj : 0.0;

            if ((totalPagado + montoPago) > totalPresupuesto) {
                return ResponseEntity.badRequest().body("Error: El pago excede el saldo pendiente. Saldo actual: L. " + (totalPresupuesto - totalPagado));
            }

            jdbc.update("INSERT INTO Pagos (id_orden, monto, fecha_pago, tipo_pago) VALUES (?, ?, GETDATE(), 'Efectivo')", idOrden, montoPago);
            
            double nuevoSaldo = totalPresupuesto - (totalPagado + montoPago);
            return ResponseEntity.ok("Pago registrado. Saldo pendiente: L. " + nuevoSaldo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar pago: " + e.getMessage());
        }
    }

    @DeleteMapping("/{idDetalle}")
    public ResponseEntity<String> eliminarDetalle(@PathVariable int idDetalle) {
        try {
            Integer idOrden = jdbc.queryForObject("SELECT id_orden FROM DetalleOrden WHERE id_detalle = ?", Integer.class, idDetalle);
            if (idOrden != null && estaOrdenCerrada(idOrden)) {
                return ResponseEntity.badRequest().body("No se pueden eliminar ítems de una orden cerrada.");
            }

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
            if (estaOrdenCerrada(idOrden)) {
                return ResponseEntity.badRequest().body("No se puede aplicar descuento a una orden cerrada.");
            }

            double descuento = Double.parseDouble(body.get("descuento").toString());
            Double subtotalActual = jdbc.queryForObject("SELECT ISNULL(SUM(costo_aplicado), 0) FROM DetalleOrden WHERE id_orden = ?", Double.class, idOrden);

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