package com.autotech.autotech_manager.repository;

import com.autotech.autotech_manager.model.Vehiculo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VehiculoRepository {

    private final JdbcTemplate jdbc;

    public VehiculoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Vehiculo> mapper = (rs, rowNum) -> new Vehiculo(
        rs.getInt("id_vehiculo"),
        rs.getInt("id_cliente"),
        rs.getString("placa"),
        rs.getString("vin_chasis"),
        rs.getString("marca"),
        rs.getString("model"),
        rs.getInt("anio"),
        rs.getString("color"),
        rs.getInt("kilometraje"),
        rs.getString("combustible")
    );

    public List<Vehiculo> listarTodos() {
        return jdbc.query("SELECT * FROM Vehiculos ORDER BY placa", mapper);
    }

    public Optional<Vehiculo> buscarPorId(int id) {
        List<Vehiculo> lista = jdbc.query("SELECT * FROM Vehiculos WHERE id_vehiculo = ?", mapper, id);
        return lista.stream().findFirst();
    }
    
    public List<Vehiculo> listarPorCliente(int idCliente) {
        return jdbc.query("SELECT * FROM Vehiculos WHERE id_cliente = ? ORDER BY placa", mapper, idCliente);
    }
}