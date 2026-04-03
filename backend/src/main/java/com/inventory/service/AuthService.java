package com.inventory.service;

import com.inventory.dto.LoginRequest;
import com.inventory.dto.LoginResponse;
import com.inventory.entity.Usuario;
import com.inventory.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioDetailsService userDetailsService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = (Usuario) userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(usuario);

        return new LoginResponse(
                token,
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol().name(),
                usuario.getSucursal() != null ? usuario.getSucursal().getId() : null
        );
    }
}
