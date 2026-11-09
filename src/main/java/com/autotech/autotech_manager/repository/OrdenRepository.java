package com.autotech.autotech_manager.repository;

import com.autotech.autotech_manager.model.OrdenTrabajo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrdenRepository {

    private final JdbcTemplate jdbc;

    public OrdenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<OrdenTrabajo> mapper = (rs, rowNum) -> new OrdenTrabajo(
        rs.getInt("id_orden"),
        rs.getInt("id_vehiculo"),
        rs.getTimestamp("fecha_ingreso") == null ? null : rs.getTimestamp("fecha_ingreso").toLocalDateTime(),
        rs.getInt("kilometraje_ingreso"),
        rs.getString("sintomas"),
        rs.getString("diagnostico_preliminar"),
        rs.getObject("id_mecanico_responsable") == null ? null : rs.getInt("id_mecanico_responsable"),
        rs.getString("estado")
    );

    public List<OrdenTrabajo> listar() {
        return jdbc.query("SELECT * FROM OrdenesTrabajo ORDER BY fecha_ingreso DESC", mapper);
    }

    // NUEVO MÉTODO PARA REPORTES (Garantiza que el frontend reciba id_vehiculo)
    public List<Map<String, Object>> listarDTO() {
        String sql = """
            SELECT 
                o.id_orden, 
                o.id_vehiculo, 
                o.estado, 
                c.nombre AS cliente, 
                v.placa, 
                CONCAT(v.marca, ' ', v.model) AS vehiculo,
                m.nombre_completo AS mecanico
            FROM OrdenesTrabajo o
            INNER JOIN Vehiculos v ON o.id_vehiculo = v.id_vehiculo
            INNER JOIN Clientes c ON v.id_cliente = c.id_cliente
            LEFT JOIN Mecanicos m ON o.id_mecanico_responsable = m.id_mecanico
            ORDER BY o.fecha_ingreso DESC
            """;
        return jdbc.queryForList(sql);
    }

    public Optional<OrdenTrabajo> buscarPorId(int id) {
        List<OrdenTrabajo> lista = jdbc.query("SELECT * FROM OrdenesTrabajo WHERE id_orden = ?", mapper, id);
        return lista.stream().findFirst();
    }

    public int insertarOrden(Integer idVehiculo, Integer km, String sintomas, String diag, Integer idMecanico) {
        String sql = """
            INSERT INTO OrdenesTrabajo 
            (id_vehiculo, fecha_ingreso, kilometraje_ingreso, sintomas, diagnostico_preliminar, id_mecanico_responsable, estado) 
            VALUES (?, GETDATE(), ?, ?, ?, ?, 'Recibida')
            """;
        return jdbc.update(sql, idVehiculo, km, sintomas, diag, idMecanico);
    }

    public int actualizarEstado(int idOrden, String nuevoEstado) {
        return jdbc.update("UPDATE OrdenesTrabajo SET estado = ? WHERE id_orden = ?", nuevoEstado, idOrden);
    }
}