package com.logistics.system.configuration.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "module_config_audit")
public class ModuleConfigAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_key", nullable = false)
    private String moduleKey;

    @Column(name = "previous_value")
    private String previousValue;

    @Column(name = "new_value", nullable = false)
    private String newValue;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    private String reason;

    public ModuleConfigAudit() {}

    public ModuleConfigAudit(String moduleKey, String previousValue, String newValue, String changedBy, String reason) {
        this.moduleKey = moduleKey;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getModuleKey() { return moduleKey; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
}
