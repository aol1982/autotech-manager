package com.autotech.autotech_manager.repository;

import com.autotech.autotech_manager.model.Cliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository {

    private final JdbcTemplate jdbc;

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Cliente> mapper = (rs, rowNum) -> new Cliente(
        rs.getInt("id_cliente"),
        rs.getString("nombre"),
        rs.getString("telefono"),
        rs.getString("correo"),
        rs.getString("direccion"),
        rs.getInt("activo")
    );

    public List<Cliente> listarActivos() {
        return jdbc.query("SELECT * FROM Clientes WHERE activo = 1 ORDER BY nombre", mapper);
    }

    public Optional<Cliente> buscarPorId(int id) {
        List<Cliente> lista = jdbc.query("SELECT * FROM Clientes WHERE id_cliente = ?", mapper, id);
        return lista.stream().findFirst();
    }

    public int insertar(Cliente c) {
        return jdbc.update(
            "INSERT INTO Clientes (nombre, telefono, correo, direccion, activo) VALUES (?, ?, ?, ?, ?)",
            c.nombre(), c.telefono(), c.correo(), c.direccion(), c.activo() == null ? 1 : c.activo()
        );
    }

    public int actualizar(Cliente c) {
        return jdbc.update(
            "UPDATE Clientes SET nombre = ?, telefono = ?, correo = ?, direccion = ? WHERE id_cliente = ?",
            c.nombre(), c.telefono(), c.correo(), c.direccion(), c.id_cliente()
        );
    }

    public int desactivar(int id) {
        return jdbc.update("UPDATE Clientes SET activo = 0 WHERE id_cliente = ?", id);
    }
}