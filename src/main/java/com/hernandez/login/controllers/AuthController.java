package com.hernandez.login.controllers;

import com.hernandez.login.models.Usuario;
import com.hernandez.login.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> datos) {
        String correo = (String) datos.get("username");
        String password = (String) datos.get("password");
        String nombre = (String) datos.get("nombre");
        String telefono = (String) datos.get("telefono");
        String rol = (String) datos.getOrDefault("rol", "ROLE_VIAJERO");
        Boolean aceptoTerminos = (Boolean) datos.get("aceptoTerminos");

        // 1. Validaciones básicas obligatorias
        if (correo == null || correo.isBlank() || password == null || password.isBlank() || nombre == null || nombre.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre, correo y contraseña son obligatorios."));
        }

        // 2. Validación de Habeas Data / Términos (RF20 / RNF9)
        if (aceptoTerminos == null || !aceptoTerminos) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes aceptar los términos y condiciones (Ley 1581 de 2012) para registrarte."));
        }

        // 3. Verificar correo duplicado
        if (usuarioRepository.findByUsername(correo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este correo electrónico ya se encuentra registrado."));
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUsername(correo);
        nuevoUsuario.setPassword(passwordEncoder.encode(password));
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setTelefono(telefono);
        nuevoUsuario.setRol(rol);
        nuevoUsuario.setAceptoTerminos(true);
        nuevoUsuario.setFechaAceptacionTerminos(LocalDateTime.now());

        // 4. Lógica específica por rol (HU-01)
        if ("ROLE_AGENCIA".equals(rol)) {
            String nit = (String) datos.get("nit");
            String rnt = (String) datos.get("rnt");

            if (nit == null || nit.isBlank() || rnt == null || rnt.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Las agencias deben ingresar su NIT y Registro Nacional de Turismo (RNT)."));
            }

            nuevoUsuario.setNit(nit);
            nuevoUsuario.setRnt(rnt);
            nuevoUsuario.setEnabled(false); // Queda pendiente hasta validación del Admin
        } else {
            nuevoUsuario.setDocumento((String) datos.get("documento"));
            nuevoUsuario.setEnabled(true); // Viajeros quedan activos de inmediato
        }

        usuarioRepository.save(nuevoUsuario);

        String mensajeRespuesta = "ROLE_AGENCIA".equals(rol)
            ? "Registro exitoso. Tu agencia quedó en estado 'pendiente de aprobación' hasta verificar el RNT."
            : "Registro exitoso. Ya puedes iniciar sesión en Huila Travel Expedition.";

        return ResponseEntity.ok(Map.of(
            "mensaje", mensajeRespuesta,
            "estado", nuevoUsuario.isEnabled() ? "ACTIVO" : "PENDIENTE_APROBACION"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        String correo = credenciales.get("username");
        String password = credenciales.get("password");

        Optional<Usuario> optUsuario = usuarioRepository.findByUsername(correo);

        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, password)
            );

            if (optUsuario.isPresent()) {
                Usuario u = optUsuario.get();
                u.setIntentosFallidos(0);
                u.setBloqueoHasta(null);
                usuarioRepository.save(u);

                return ResponseEntity.ok(Map.of(
                    "mensaje", "Inicio de sesión exitoso",
                    "nombre", u.getNombre(),
                    "username", u.getUsername(),
                    "rol", u.getRol()
                ));
            }
            return ResponseEntity.ok(Map.of("mensaje", "Autenticado"));

        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                .body(Map.of("error", "Cuenta bloqueada por 15 minutos tras 5 intentos fallidos."));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Tu cuenta de agencia está pendiente de aprobación por el administrador."));
        } catch (BadCredentialsException e) {
            if (optUsuario.isPresent()) {
                Usuario u = optUsuario.get();
                int intentos = u.getIntentosFallidos() + 1;
                u.setIntentosFallidos(intentos);
                if (intentos >= 5) {
                    u.setBloqueoHasta(LocalDateTime.now().plusMinutes(15));
                }
                usuarioRepository.save(u);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Correo o contraseña incorrectos."));
        }
    }
}
