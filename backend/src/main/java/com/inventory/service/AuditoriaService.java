package com.inventory.service;

import com.inventory.entity.Movimiento;
import com.inventory.entity.Producto;
import com.inventory.entity.Sucursal;
import com.inventory.entity.Usuario;
import com.inventory.entity.enums.TipoMovimiento;
import com.inventory.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final MovimientoRepository movimientoRepository;

    @Transactional
    public void registrar(TipoMovimiento tipo, Producto producto, Sucursal sucursal, 
                          String motivo, String observaciones) {
        
        Usuario usuario = obtenerUsuarioActual();
        
        Movimiento m = Movimiento.builder()
                .tipo(tipo)
                .producto(producto)
                .sucursal(sucursal)
                .cantidad(0)
                .cantidadAntes(0)
                .cantidadDespues(0)
                .usuario(usuario)
                .motivo(motivo)
                .observaciones(observaciones)
                .build();
                
        movimientoRepository.save(m);
    }

    private Usuario obtenerUsuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Usuario ? (Usuario) principal : null;
    }
}
