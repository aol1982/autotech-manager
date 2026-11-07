package com.autotech.autotech_manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final JdbcTemplate jdbc;

    public UsuarioController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String user = body.get("username");
        String pass = body.get("password");

        try {
            String sql = "SELECT id_usuario, username, rol, nombre_completo FROM Usuarios WHERE username = ? AND password = ? AND activo = 1";
            Map<String, Object> result = jdbc.queryForMap(sql, user, pass);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Credenciales incorrectas");
        }
    }

    @GetMapping
    public List<Map<String, Object>> listarUsuarios() {
        return jdbc.queryForList("SELECT id_usuario, username, nombre_completo, rol, activo FROM Usuarios ORDER BY id_usuario");
    }

    @PostMapping
    public ResponseEntity<String> crearUsuario(@RequestBody Map<String, Object> body) {
        try {
            String sql = "INSERT INTO Usuarios (username, password, nombre_completo, rol, activo) VALUES (?, ?, ?, ?, 1)";
            jdbc.update(sql, body.get("username"), body.get("password"), body.get("nombre_completo"), body.get("rol"));
            return ResponseEntity.ok("Usuario creado exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear usuario: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizarUsuario(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            if (body.get("password") != null && !body.get("password").toString().isEmpty()) {
                String sql = "UPDATE Usuarios SET username = ?, password = ?, nombre_completo = ?, rol = ? WHERE id_usuario = ?";
                jdbc.update(sql, body.get("username"), body.get("password"), body.get("nombre_completo"), body.get("rol"), id);
            } else {
                String sql = "UPDATE Usuarios SET username = ?, nombre_completo = ?, rol = ? WHERE id_usuario = ?";
                jdbc.update(sql, body.get("username"), body.get("nombre_completo"), body.get("rol"), id);
            }
            return ResponseEntity.ok("Usuario actualizado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstado(@PathVariable int id) {
        try {
            jdbc.update("UPDATE Usuarios SET activo = CASE WHEN activo = 1 THEN 0 ELSE 1 END WHERE id_usuario = ?", id);
            return ResponseEntity.ok("Estado del usuario cambiado.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}