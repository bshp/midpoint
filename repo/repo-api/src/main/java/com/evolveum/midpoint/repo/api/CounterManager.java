/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 *
 */
public interface CounterManager {

    CounterSpecification getCounterSpec(TaskType task, String policyRuleId, PolicyRuleType policyRule);
    void cleanupCounters(String taskOid);
    Collection<CounterSpecification> listCounters();
    void removeCounter(CounterSpecification counterSpecification);
    void resetCounters(String taskOid);
}
