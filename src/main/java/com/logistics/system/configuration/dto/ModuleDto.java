package com.logistics.system.configuration.dto;

import com.logistics.system.configuration.domain.ModuleType;
import java.time.Instant;

public class ModuleDto {

    public record CatalogResponse(
            String moduleKey,
            String name,
            String description,
            ModuleType moduleType,
            boolean active
    ) {}

    public record ConfigResponse(
            String moduleKey,
            String name,
            String description,
            ModuleType moduleType,
            boolean enabled,
            String configurationJson,
            Instant effectiveFrom,
            Instant effectiveUntil,
            String updatedBy,
            Instant updatedAt,
            Long version
    ) {}

    public record UpdateRequest(
            boolean enabled,
            String configurationJson,
            Instant effectiveFrom,
            Instant effectiveUntil,
            String reason
    ) {}

    public record AuditResponse(
            Long id,
            String moduleKey,
            String previousValue,
            String newValue,
            String changedBy,
            Instant changedAt,
            String reason
    ) {}

    public record MeModuleResponse(
            String moduleKey,
            String name,
            String description,
            ModuleType moduleType
    ) {}
}
