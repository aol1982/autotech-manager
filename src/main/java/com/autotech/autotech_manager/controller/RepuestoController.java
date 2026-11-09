package com.autotech.autotech_manager.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repuestos")
public class RepuestoController {

    private final JdbcTemplate jdbc;

    public RepuestoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> listarRepuestos() {
        String sql = "SELECT id_repuesto, nombre, costo, stock_actual FROM Repuestos ORDER BY nombre ASC";
        return jdbc.queryForList(sql);
    }
}