package com.inventoryiq.adapters.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Chequeo de disponibilidad del backend")
public class HealthController {
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public String health() {
        return "OK";
    }
}
