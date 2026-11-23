
package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudRule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudRuleRepository
        extends JpaRepository<FraudRule, UUID> {

    Optional<FraudRule> findByRuleCode(String ruleCode);

    List<FraudRule> findByEnabledTrue();

    boolean existsByRuleCode(String ruleCode);
}
