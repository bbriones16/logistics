package com.logistics.system.configuration.controller;

import com.logistics.system.configuration.dto.ModuleDto;
import com.logistics.system.configuration.service.ModuleConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ModuleConfigController {

    private final ModuleConfigService service;

    public ModuleConfigController(ModuleConfigService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/modules")
    public List<ModuleDto.ConfigResponse> listAll() {
        return service.listAll();
    }

    @GetMapping("/api/admin/modules/{moduleKey}")
    public ModuleDto.ConfigResponse getByKey(@PathVariable String moduleKey) {
        return service.getByKey(moduleKey);
    }

    @PutMapping("/api/admin/modules/{moduleKey}")
    public ModuleDto.ConfigResponse update(
            @PathVariable String moduleKey,
            @RequestBody ModuleDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return service.update(moduleKey, request, principal.getUsername());
    }

    @GetMapping("/api/admin/modules/audit")
    public List<ModuleDto.AuditResponse> getAuditLog() {
        return service.getAuditLog();
    }

    @GetMapping("/api/me/modules")
    public ResponseEntity<List<ModuleDto.MeModuleResponse>> getMyModules() {
        return ResponseEntity.ok(service.getEnabledModulesForUser());
    }
}
