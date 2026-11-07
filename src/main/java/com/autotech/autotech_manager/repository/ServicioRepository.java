package com.autotech.autotech_manager.repository;

import com.autotech.autotech_manager.model.Servicio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ServicioRepository {

    private final JdbcTemplate jdbc;

    public ServicioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Servicio> mapper = (rs, rowNum) -> new Servicio(
        rs.getInt("id_servicio"),
        rs.getString("codigo"),
        rs.getString("nombre"),
        rs.getString("descripcion"),
        rs.getInt("nivel"),
        rs.getBigDecimal("costo_mano_obra"),
        rs.getString("tiempo_estimado")
    );

    public List<Servicio> listar() {
        return jdbc.query("SELECT * FROM Servicios ORDER BY nivel, nombre", mapper);
    }

    public List<Servicio> listarPorNivel(int nivel) {
        return jdbc.query("SELECT * FROM Servicios WHERE nivel = ? ORDER BY nombre", mapper, nivel);
    }
}