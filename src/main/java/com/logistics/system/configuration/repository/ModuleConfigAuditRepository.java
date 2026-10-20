package com.logistics.system.configuration.repository;

import com.logistics.system.configuration.domain.ModuleConfigAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleConfigAuditRepository extends JpaRepository<ModuleConfigAudit, Long> {
    List<ModuleConfigAudit> findAllByOrderByChangedAtDesc();
}
