package com.inventory.dto;

import java.util.List;

public record RecepcionRequest(List<Integer> cantidades, String accionFaltante, String notasFaltante) {}
