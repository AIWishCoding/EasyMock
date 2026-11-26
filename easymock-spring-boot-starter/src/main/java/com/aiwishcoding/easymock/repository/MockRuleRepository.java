package com.aiwishcoding.easymock.repository;

import com.aiwishcoding.easymock.model.MockRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockRuleRepository extends JpaRepository<MockRule, Long> {

    List<MockRule> findByUrlAndEnabledTrueOrderByPriorityDesc(String url);

    List<MockRule> findAllByEnabledTrueOrderByPriorityDescIdAsc();

    List<MockRule> findAllByOrderByUrlAscPriorityDesc();
}
