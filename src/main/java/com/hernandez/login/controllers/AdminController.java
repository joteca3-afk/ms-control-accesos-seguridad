package com.hernandez.login.controllers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String getDashboardData() {
        return "Bienvenido al Panel de Administración. Este contenido es altamente confidencial y solo visible para ADMINS.";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios() {
        return "Lista de todos los usuarios registrados en el sistema (Solo accesible por Administradores).";
    }
}