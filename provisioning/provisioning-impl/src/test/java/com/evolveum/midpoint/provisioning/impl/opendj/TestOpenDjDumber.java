/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Test for provisioning service implementation using embedded OpenDj instance.
 * This is the same test as TestOpenDj, but the OpenDJ resource configuration is
 * somehow dumber: no shortcut in associations, explicit duplicity checks, etc.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjDumber extends TestOpenDj {

    protected static final File RESOURCE_OPENDJ_DUMBER_FILE = new File(TEST_DIR, "resource-opendj-dumber.xml");

    private static Trace LOGGER = TraceManager.getTrace(TestOpenDjDumber.class);

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_DUMBER_FILE;
    }

    @Override
    protected int getNumberOfBaseContextShadows() {
        return 1;
    }

    @Override
    protected void assertConnectorOperationIncrement(int expectedIncrementSmart, int expectedIncrementDumb) {
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, expectedIncrementDumb);
    }

    /**
     * But "dumber" resource do not have any count simulation for groups.
     */
    @Override
    protected Integer getExpectedLdapGroupCountTest25x() {
        return null;
    }

    @Override
    protected void assertTimestampType(String attrName, ResourceAttributeDefinition<?> def) {
        assertEquals("Wrong "+attrName+"type", DOMUtil.XSD_STRING, def.getTypeName());
    }

    @Override
    protected void assertTimestamp(String attrName, Object timestampValue) {
        if (!(timestampValue instanceof String)) {
            fail("Wrong type of "+attrName+", expected String but was "+timestampValue.getClass());
        }
        String str = (String)timestampValue;
        assertTrue("Timestamp "+attrName+" does not start with 2: "+str, str.startsWith("2"));
        assertTrue("Timestamp "+attrName+" does not end with Z: "+str, str.endsWith("Z"));
    }

}
