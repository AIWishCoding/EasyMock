package com.clic.easymock.repository;

import com.clic.easymock.model.MockRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockRuleRepository extends JpaRepository<MockRule, Long> {

    List<MockRule> findByUrlAndEnabledTrueOrderByPriorityDesc(String url);

    List<MockRule> findAllByOrderByUrlAscPriorityDesc();
}
