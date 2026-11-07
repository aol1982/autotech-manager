package com.autotech.autotech_manager.controller;

import com.autotech.autotech_manager.model.Vehiculo;
import com.autotech.autotech_manager.repository.VehiculoRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoRepository repo;

    public VehiculoController(VehiculoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Vehiculo> listar() {
        return repo.listarTodos();
    }
}