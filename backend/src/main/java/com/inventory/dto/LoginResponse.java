package com.inventory.dto;

public record LoginResponse(
        String token,
        String email,
        String nombre,
        String rol,
        Long sucursalId
) {}
