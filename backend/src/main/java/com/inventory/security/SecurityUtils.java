package com.inventory.security;

import com.inventory.entity.Usuario;
import com.inventory.entity.enums.RolUsuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Usuario getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario) {
            return (Usuario) principal;
        }
        return null;
    }

    public boolean isAdmin() {
        Usuario user = getCurrentUser();
        return user != null && user.getRol() == RolUsuario.ADMINISTRADOR;
    }

    public Long getCurrentSucursalId() {
        Usuario user = getCurrentUser();
        return (user != null && user.getSucursal() != null) ? user.getSucursal().getId() : null;
    }

    public void validateSucursalAccess(Long sucursalId) {
        if (isAdmin()) return;
        
        Long userSucursalId = getCurrentSucursalId();
        if (userSucursalId == null || !userSucursalId.equals(sucursalId)) {
            throw new RuntimeException("Acceso denegado: No tiene permisos para acceder a esta sucursal.");
        }
    }
}
