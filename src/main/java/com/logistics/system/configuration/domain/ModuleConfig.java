package com.logistics.system.configuration.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "module_config")
public class ModuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false, unique = true)
    private ModuleCatalog module;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "configuration_json")
    private String configurationJson;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    public Long getId() { return id; }
    public ModuleCatalog getModule() { return module; }
    public boolean isEnabled() { return enabled; }
    public String getConfigurationJson() { return configurationJson; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveUntil() { return effectiveUntil; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public void setEffectiveUntil(Instant effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
