package com.autotech.autotech_manager.repository;

import com.autotech.autotech_manager.model.Motor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MotorRepository {

    private final JdbcTemplate jdbc;

    public MotorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Motor> mapper = (rs, rowNum) -> new Motor(
        rs.getInt("id_motor"),
        rs.getString("numero_serie"),
        rs.getString("modelo_compatible"),
        rs.getString("estado")
    );

    public List<Motor> listarDisponibles() {
        return jdbc.query("SELECT * FROM Motores WHERE estado = 'Disponible'", mapper);
    }

    public Optional<Motor> buscarPorId(int id) {
        List<Motor> lista = jdbc.query("SELECT * FROM Motores WHERE id_motor = ?", mapper, id);
        return lista.stream().findFirst();
    }

    public int actualizarEstadoMotor(int idMotor, String nuevoEstado) {
        return jdbc.update("UPDATE Motores SET estado = ? WHERE id_motor = ?", nuevoEstado, idMotor);
    }
}