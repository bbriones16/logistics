package com.logistics.system.configuration.service;

import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.configuration.domain.ModuleCatalog;
import com.logistics.system.configuration.domain.ModuleConfig;
import com.logistics.system.configuration.domain.ModuleConfigAudit;
import com.logistics.system.configuration.domain.ModuleType;
import com.logistics.system.configuration.dto.ModuleDto;
import com.logistics.system.configuration.repository.ModuleConfigAuditRepository;
import com.logistics.system.configuration.repository.ModuleConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleConfigServiceTest {

    @Mock private ModuleConfigRepository configRepository;
    @Mock private ModuleConfigAuditRepository auditRepository;

    @InjectMocks
    private ModuleConfigService service;

    private ModuleCatalog catalog;
    private ModuleConfig config;

    @BeforeEach
    void setUp() {
        catalog = new ModuleCatalog();
        setField(catalog, "moduleKey", "INTERNATIONAL_FREIGHT");
        setField(catalog, "name", "International Freight");
        setField(catalog, "description", "Freight forwarding");
        setField(catalog, "moduleType", ModuleType.BUSINESS);
        setField(catalog, "active", true);

        config = new ModuleConfig();
        setField(config, "module", catalog);
        config.setEnabled(false);
    }

    @Test
    void listAll_returnsMappedResponses() {
        when(configRepository.findAllWithModule()).thenReturn(List.of(config));

        List<ModuleDto.ConfigResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).moduleKey()).isEqualTo("INTERNATIONAL_FREIGHT");
        assertThat(result.get(0).enabled()).isFalse();
    }

    @Test
    void getByKey_existingKey_returnsResponse() {
        when(configRepository.findByModuleKey("INTERNATIONAL_FREIGHT")).thenReturn(Optional.of(config));

        ModuleDto.ConfigResponse result = service.getByKey("INTERNATIONAL_FREIGHT");

        assertThat(result.moduleKey()).isEqualTo("INTERNATIONAL_FREIGHT");
    }

    @Test
    void getByKey_unknownKey_throwsResourceNotFoundException() {
        when(configRepository.findByModuleKey("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByKey("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_enablesModule_savesConfigAndAuditRecord() {
        when(configRepository.findByModuleKey("INTERNATIONAL_FREIGHT")).thenReturn(Optional.of(config));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModuleDto.UpdateRequest request = new ModuleDto.UpdateRequest(true, null, null, null, "Enabling freight");
        ModuleDto.ConfigResponse result = service.update("INTERNATIONAL_FREIGHT", request, "admin@logistics.com");

        assertThat(result.enabled()).isTrue();

        ArgumentCaptor<ModuleConfigAudit> auditCaptor = ArgumentCaptor.forClass(ModuleConfigAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        ModuleConfigAudit audit = auditCaptor.getValue();
        assertThat(audit.getModuleKey()).isEqualTo("INTERNATIONAL_FREIGHT");
        assertThat(audit.getChangedBy()).isEqualTo("admin@logistics.com");
        assertThat(audit.getReason()).isEqualTo("Enabling freight");
        assertThat(audit.getPreviousValue()).contains("false");
        assertThat(audit.getNewValue()).contains("true");
    }

    @Test
    void update_disablesModule_recordsPreviousEnabledState() {
        config.setEnabled(true);
        when(configRepository.findByModuleKey("INTERNATIONAL_FREIGHT")).thenReturn(Optional.of(config));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update("INTERNATIONAL_FREIGHT", new ModuleDto.UpdateRequest(false, null, null, null, null), "admin");

        ArgumentCaptor<ModuleConfigAudit> captor = ArgumentCaptor.forClass(ModuleConfigAudit.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getPreviousValue()).contains("true");
        assertThat(captor.getValue().getNewValue()).contains("false");
    }

    @Test
    void update_unknownKey_throwsResourceNotFoundException() {
        when(configRepository.findByModuleKey("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("UNKNOWN",
                new ModuleDto.UpdateRequest(true, null, null, null, null), "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAuditLog_returnsMappedAuditEntries() {
        ModuleConfigAudit audit = new ModuleConfigAudit(
                "INTERNATIONAL_FREIGHT", "{\"enabled\":false}", "{\"enabled\":true}", "admin", "test");
        when(auditRepository.findAllByOrderByChangedAtDesc()).thenReturn(List.of(audit));

        List<ModuleDto.AuditResponse> result = service.getAuditLog();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).moduleKey()).isEqualTo("INTERNATIONAL_FREIGHT");
        assertThat(result.get(0).changedBy()).isEqualTo("admin");
    }

    @Test
    void getEnabledModulesForUser_returnsOnlyEnabledModules() {
        config.setEnabled(true);
        when(configRepository.findAllEnabled()).thenReturn(List.of(config));

        List<ModuleDto.MeModuleResponse> result = service.getEnabledModulesForUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).moduleKey()).isEqualTo("INTERNATIONAL_FREIGHT");
    }

    @Test
    void isModuleEnabled_enabledModule_returnsTrue() {
        config.setEnabled(true);
        when(configRepository.findByModuleKey("INTERNATIONAL_FREIGHT")).thenReturn(Optional.of(config));

        assertThat(service.isModuleEnabled("INTERNATIONAL_FREIGHT")).isTrue();
    }

    @Test
    void isModuleEnabled_disabledModule_returnsFalse() {
        when(configRepository.findByModuleKey("INTERNATIONAL_FREIGHT")).thenReturn(Optional.of(config));

        assertThat(service.isModuleEnabled("INTERNATIONAL_FREIGHT")).isFalse();
    }

    @Test
    void isModuleEnabled_unknownKey_returnsFalse() {
        when(configRepository.findByModuleKey("UNKNOWN")).thenReturn(Optional.empty());

        assertThat(service.isModuleEnabled("UNKNOWN")).isFalse();
    }

    // Reflective field setter to avoid adding setters to domain entities
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
