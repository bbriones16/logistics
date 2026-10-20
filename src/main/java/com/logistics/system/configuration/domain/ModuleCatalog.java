package com.logistics.system.configuration.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "module_catalog")
public class ModuleCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_key", nullable = false, unique = true)
    private String moduleKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false)
    private ModuleType moduleType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public String getModuleKey() { return moduleKey; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ModuleType getModuleType() { return moduleType; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
