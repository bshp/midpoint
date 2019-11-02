/**
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class TriggersAsserter<O extends ObjectType, OA extends PrismObjectAsserter<O,RA>, RA> extends AbstractAsserter<OA> {

    private List<TriggerType> triggers;
    private OA objectAsserter;

    public TriggersAsserter(OA objectAsserter) {
        super();
        this.objectAsserter = objectAsserter;
    }

    public TriggersAsserter(OA objectAsserter, String details) {
        super(details);
        this.objectAsserter = objectAsserter;
    }

    private PrismObject<O> getObject() {
        return objectAsserter.getObject();
    }

    List<TriggerType> getTriggers() {
        if (triggers == null) {
            triggers = getObject().asObjectable().getTrigger();
        }
        return triggers;
    }

    public TriggersAsserter<O,OA,RA> assertTriggers(int expected) {
        assertEquals("Wrong number of triggers in "+desc(), expected, getTriggers().size());
        return this;
    }

    public TriggersAsserter<O,OA,RA> assertAny() {
        assertFalse("No triggers in "+desc(), getTriggers().isEmpty());
        return this;
    }

    public TriggersAsserter<O,OA,RA> assertNone() {
        assertTriggers(0);
        return this;
    }

    public TriggerAsserter<TriggersAsserter<O,OA,RA>> single() {
        assertTriggers(1);
        return forTrigger(getTriggers().get(0));
    }

    public TriggerFinder<O,OA,RA> by() {
        return new TriggerFinder<>(this);
    }

    TriggerAsserter<TriggersAsserter<O,OA,RA>> forTrigger(TriggerType trigger) {
        TriggerAsserter<TriggersAsserter<O,OA,RA>> asserter = new TriggerAsserter<>(trigger, this, "trigger in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return "triggers of " + descWithDetails(getObject());
    }

    @Override
    public OA end() {
        return objectAsserter;
    }

}
