package com.hernandez.login.models;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // Correo electrónico

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol; // "ROLE_VIAJERO", "ROLE_AGENCIA" o "ROLE_ADMIN"

    @Column(nullable = false)
    private String nombre; // Nombre completo o razón social de la agencia

    private String documento; // Cédula (para viajeros)
    private String telefono;
    
    // Campos exclusivos para Agencias (HU-01)
    private String nit;
    private String rnt;

    // Cumplimiento Ley 1581 de 2012 (RF20 / RNF9)
    @Column(nullable = false)
    private boolean aceptoTerminos;
    private LocalDateTime fechaAceptacionTerminos;

    // Control de estado y seguridad (HU-01 y HU-02)
    private boolean enabled = true;
    private int intentosFallidos = 0;
    private LocalDateTime bloqueoHasta;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getRnt() { return rnt; }
    public void setRnt(String rnt) { this.rnt = rnt; }

    public boolean isAceptoTerminos() { return aceptoTerminos; }
    public void setAceptoTerminos(boolean aceptoTerminos) { this.aceptoTerminos = aceptoTerminos; }

    public LocalDateTime getFechaAceptacionTerminos() { return fechaAceptacionTerminos; }
    public void setFechaAceptacionTerminos(LocalDateTime fechaAceptacionTerminos) { this.fechaAceptacionTerminos = fechaAceptacionTerminos; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public LocalDateTime getBloqueoHasta() { return bloqueoHasta; }
    public void setBloqueoHasta(LocalDateTime bloqueoHasta) { this.bloqueoHasta = bloqueoHasta; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        if (bloqueoHasta == null) return true;
        return bloqueoHasta.isBefore(LocalDateTime.now());
    }

    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
