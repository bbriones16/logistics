package com.logistics.system.configuration.guard;

import com.logistics.system.configuration.service.ModuleConfigService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class ModuleAccessAspect {

    private final ModuleConfigService moduleConfigService;

    public ModuleAccessAspect(ModuleConfigService moduleConfigService) {
        this.moduleConfigService = moduleConfigService;
    }

    @Before("@within(requiresModule) || @annotation(requiresModule)")
    public void checkModuleEnabled(RequiresModule requiresModule) {
        if (!moduleConfigService.isModuleEnabled(requiresModule.value())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module '" + requiresModule.value() + "' is not enabled for this deployment");
        }
    }
}
