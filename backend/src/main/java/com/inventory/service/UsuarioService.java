package com.inventory.service;

import com.inventory.dto.UsuarioRequest;
import com.inventory.dto.UsuarioResponse;
import com.inventory.entity.Sucursal;
import com.inventory.entity.Usuario;
import com.inventory.entity.enums.RolUsuario;
import com.inventory.repository.SucursalRepository;
import com.inventory.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con email: " + req.email());
        }

        Sucursal sucursal = null;
        if (req.sucursalId() != null) {
            sucursal = sucursalRepository.findById(req.sucursalId())
                    .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + req.sucursalId()));
        }

        RolUsuario rol;
        try {
            rol = RolUsuario.valueOf(req.rol());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + req.rol());
        }

        Usuario usuario = Usuario.builder()
                .nombre(req.nombre())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .rol(rol)
                .sucursal(sucursal)
                .build();

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req) {
        Usuario usuario = buscarPorId(id);

        if (!usuario.getEmail().equals(req.email()) && usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con email: " + req.email());
        }

        Sucursal sucursal = null;
        if (req.sucursalId() != null) {
            sucursal = sucursalRepository.findById(req.sucursalId())
                    .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
        }

        usuario.setNombre(req.nombre());
        usuario.setEmail(req.email());
        if (req.password() != null && !req.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        usuario.setRol(com.inventory.entity.enums.RolUsuario.valueOf(req.rol()));
        usuario.setSucursal(sucursal);

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse desactivar(Long id, String motivo) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        usuario.setMotivoDesactivacion(motivo);
        return toResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getRol().name(),
                u.getSucursal() != null ? u.getSucursal().getId() : null,
                u.getSucursal() != null ? u.getSucursal().getNombre() : null,
                u.getActivo(), u.getMotivoDesactivacion(), u.getCreatedAt()
        );
    }
}
