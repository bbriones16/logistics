package com.logistics.system.configuration.service;

import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.configuration.domain.ModuleConfig;
import com.logistics.system.configuration.domain.ModuleConfigAudit;
import com.logistics.system.configuration.dto.ModuleDto;
import com.logistics.system.configuration.repository.ModuleConfigAuditRepository;
import com.logistics.system.configuration.repository.ModuleConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ModuleConfigService {

    private final ModuleConfigRepository configRepository;
    private final ModuleConfigAuditRepository auditRepository;

    public ModuleConfigService(ModuleConfigRepository configRepository,
                               ModuleConfigAuditRepository auditRepository) {
        this.configRepository = configRepository;
        this.auditRepository = auditRepository;
    }

    public List<ModuleDto.ConfigResponse> listAll() {
        return configRepository.findAllWithModule().stream()
                .map(this::toConfigResponse)
                .toList();
    }

    public ModuleDto.ConfigResponse getByKey(String moduleKey) {
        return toConfigResponse(findConfig(moduleKey));
    }

    @Transactional
    public ModuleDto.ConfigResponse update(String moduleKey, ModuleDto.UpdateRequest request, String updatedBy) {
        ModuleConfig config = findConfig(moduleKey);

        String previousValue = serializeState(config);

        config.setEnabled(request.enabled());
        config.setConfigurationJson(request.configurationJson());
        config.setEffectiveFrom(request.effectiveFrom());
        config.setEffectiveUntil(request.effectiveUntil());
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(Instant.now());

        configRepository.save(config);

        auditRepository.save(new ModuleConfigAudit(
                moduleKey,
                previousValue,
                serializeState(config),
                updatedBy,
                request.reason()
        ));

        return toConfigResponse(config);
    }

    public List<ModuleDto.AuditResponse> getAuditLog() {
        return auditRepository.findAllByOrderByChangedAtDesc().stream()
                .map(a -> new ModuleDto.AuditResponse(
                        a.getId(), a.getModuleKey(), a.getPreviousValue(),
                        a.getNewValue(), a.getChangedBy(), a.getChangedAt(), a.getReason()))
                .toList();
    }

    public List<ModuleDto.MeModuleResponse> getEnabledModulesForUser() {
        return configRepository.findAllEnabled().stream()
                .map(mc -> new ModuleDto.MeModuleResponse(
                        mc.getModule().getModuleKey(),
                        mc.getModule().getName(),
                        mc.getModule().getDescription(),
                        mc.getModule().getModuleType()))
                .toList();
    }

    public boolean isModuleEnabled(String moduleKey) {
        return configRepository.findByModuleKey(moduleKey)
                .map(ModuleConfig::isEnabled)
                .orElse(false);
    }

    private ModuleConfig findConfig(String moduleKey) {
        return configRepository.findByModuleKey(moduleKey)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleKey));
    }

    private String serializeState(ModuleConfig config) {
        return "{\"enabled\":" + config.isEnabled() + "}";
    }

    private ModuleDto.ConfigResponse toConfigResponse(ModuleConfig mc) {
        return new ModuleDto.ConfigResponse(
                mc.getModule().getModuleKey(),
                mc.getModule().getName(),
                mc.getModule().getDescription(),
                mc.getModule().getModuleType(),
                mc.isEnabled(),
                mc.getConfigurationJson(),
                mc.getEffectiveFrom(),
                mc.getEffectiveUntil(),
                mc.getUpdatedBy(),
                mc.getUpdatedAt(),
                mc.getVersion()
        );
    }
}
