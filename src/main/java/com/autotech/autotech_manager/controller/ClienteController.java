package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.Cliente;
import com.autotech.autotech_manager.repository.ClienteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository repo;

    public ClienteController(ClienteRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Cliente> listar() {
        return repo.listarActivos();
    }

    // NUEVO ENDPOINT PARA GUARDAR CLIENTES
    @PostMapping
    public ResponseEntity<String> crear(@RequestBody Cliente cliente) {
        try {
            // Si el cliente viene sin estado activo, lo ponemos en 1 por defecto
            Cliente nuevo = new Cliente(
                null, 
                cliente.nombre(), 
                cliente.telefono(), 
                cliente.correo(), 
                cliente.direccion(), 
                1
            );
            repo.insertar(nuevo);
            return ResponseEntity.ok("Cliente registrado exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar cliente: " + e.getMessage());
        }
    }
}