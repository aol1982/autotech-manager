package com.autotech.autotech_manager.repository;

import com.autotech.autotech_manager.model.Mecanico;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MecanicoRepository {

    private final JdbcTemplate jdbc;

    public MecanicoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Mecanico> mapper = (rs, rowNum) -> new Mecanico(
        rs.getInt("id_mecanico"),
        rs.getString("nombre"),
        rs.getString("especialidad"),
        rs.getInt("activo")
    );

    public List<Mecanico> listarActivos() {
        return jdbc.query("SELECT * FROM Mecanicos WHERE activo = 1 ORDER BY nombre", mapper);
    }
}