package com.autotech.autotech_manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final JdbcTemplate jdbcTemplate;

    public ReporteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/total-orden/{id}")
    public ResponseEntity<?> calcularTotal(@PathVariable int id) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_CalcularTotalPresupuesto");
        MapSqlParameterSource in = new MapSqlParameterSource().addValue("id_orden", id);
        Map<String, Object> out = jdbcCall.execute(in);
        BigDecimal total = (BigDecimal) out.get("total");
        return ResponseEntity.ok("El total calculado por el SP para la orden " + id + " es: L. " + total);
    }

    @PostMapping("/pagar")
    public ResponseEntity<String> pagar(@RequestParam int idOrden, @RequestParam BigDecimal monto) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_RegistrarPago");
        MapSqlParameterSource in = new MapSqlParameterSource().addValue("id_orden", idOrden).addValue("monto", monto);
        Map<String, Object> out = jdbcCall.execute(in);
        return ResponseEntity.ok((String) out.get("mensaje"));
    }

    @GetMapping("/historial-ordenes")
    public List<Map<String, Object>> historialOrdenes() {
        String sql = "SELECT * FROM vw_HistorialOrdenes ORDER BY id_orden DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/historial-motores")
    public List<Map<String, Object>> historialMotores() {
        String sql = """
            SELECT 
                cm.id_cambio,
                cm.id_orden,
                v.placa,
                cm.serie_motor_anterior,
                m.numero_serie AS nueva_serie,
                m.modelo_compatible,
                cm.destino_motor_retirado,
                cm.resultados_pruebas
            FROM CambiosMotor cm
            INNER JOIN Vehiculos v ON cm.id_vehiculo = v.id_vehiculo
            INNER JOIN Motores m ON cm.id_motor_nuevo = m.id_motor
            """;
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/presupuestos")
    public List<Map<String, Object>> listarPresupuestos() {
        String sql = """
            SELECT 
                p.id_presupuesto,
                o.id_orden,
                c.nombre AS cliente,
                v.placa,
                p.subtotal,
                p.descuentos,
                p.impuestos,
                p.total,
                o.estado AS estado_orden,
                (SELECT ISNULL(SUM(monto), 0) FROM Pagos WHERE id_orden = o.id_orden) AS total_pagado
            FROM Presupuestos p
            INNER JOIN OrdenesTrabajo o ON p.id_orden = o.id_orden
            INNER JOIN Vehiculos v ON o.id_vehiculo = v.id_vehiculo
            INNER JOIN Clientes c ON v.id_cliente = c.id_cliente
            ORDER BY p.id_presupuesto DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }
}