/**
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * OpenDJ, but without permissive modify, shortcut attributes, with manual matching rules, etc.
 * Also has additional search filter.
 *
 * @author semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOpenDjDumber extends TestOpenDj {

    private static final int INITIAL_SYNC_TOKEN = 21;

    @Override
    protected File getBaseDir() {
        return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "opendj-dumber");
    }

    @Override
    protected boolean hasAssociationShortcut() {
        return false;
    }

    @Override
    protected int getInitialSyncToken() {
        return INITIAL_SYNC_TOKEN;
    }

    @Override
    protected boolean isUsingGroupShortcutAttribute() {
        return false;
    }

    /**
     * Test for additional search filter.
     * MID-4925
     */
    @Test
    @Override
    public void test350SeachInvisibleAccount() throws Exception {
        final String TEST_NAME = "test350SeachInvisibleAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        createBilboEntry();

        SearchResultList<PrismObject<ShadowType>> shadows = searchBilbo(TEST_NAME);

        assertEquals("Unexpected search result: "+shadows, 0, shadows.size());
    }
}
