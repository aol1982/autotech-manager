package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.OrdenTrabajo;
import com.autotech.autotech_manager.repository.OrdenRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {

    private final JdbcTemplate jdbcTemplate;
    private final OrdenRepository ordenRepo;

    public PingController(JdbcTemplate jdbcTemplate, OrdenRepository ordenRepo) {
        this.jdbcTemplate = jdbcTemplate;
        this.ordenRepo = ordenRepo;
    }

    @GetMapping("/ping-db")
    public Map<String, Object> pingDb() {
        Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Map.of("conexion", "OK", "resultado", resultado);
    }

    @GetMapping("/tablas")
    public List<Map<String, Object>> listarTablas() {
        return jdbcTemplate.queryForList("SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'");
    }

    @GetMapping("/ordenes-prueba")
    public List<OrdenTrabajo> ordenesPrueba() {
        return ordenRepo.listar();
    }

    @GetMapping("/servicios")
    public List<Map<String, Object>> listarServicios() {
        return jdbcTemplate.queryForList("SELECT * FROM Servicios");
    }
}