package com.example.unis_rssol.domain.payroll.laborlaw;

import aj.org.objectweb.asm.commons.Remapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface LaborLawConfigRepository extends JpaRepository<LaborLawConfig,Long> {
    @Query("""
select c
from LaborLawConfig c
where c.configKey = :key
and :date between c.effectiveFrom and c.effectiveTo
""")
    Optional<LaborLawConfig> findByKeyAndDate(String minimumWage, LocalDate targetDate);

    @Query("""
        select c
        from LaborLawConfig c
        where c.configKey = :key
          and current_date between c.effectiveFrom and c.effectiveTo
    """)
    Optional<LaborLawConfig> findActiveConfig(String minimumWage);
}
