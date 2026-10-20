package com.logistics.system.configuration.repository;

import com.logistics.system.configuration.domain.ModuleCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuleCatalogRepository extends JpaRepository<ModuleCatalog, Long> {
    Optional<ModuleCatalog> findByModuleKey(String moduleKey);
}
