package com.logistics.system.configuration.repository;

import com.logistics.system.configuration.domain.ModuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ModuleConfigRepository extends JpaRepository<ModuleConfig, Long> {

    @Query("SELECT mc FROM ModuleConfig mc JOIN FETCH mc.module WHERE mc.module.moduleKey = :moduleKey")
    Optional<ModuleConfig> findByModuleKey(String moduleKey);

    @Query("SELECT mc FROM ModuleConfig mc JOIN FETCH mc.module")
    List<ModuleConfig> findAllWithModule();

    @Query("SELECT mc FROM ModuleConfig mc JOIN FETCH mc.module WHERE mc.enabled = true")
    List<ModuleConfig> findAllEnabled();
}
