package com.evolveum.midpoint.testing.story.uuid;
/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.opends.server.types.DirectoryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractUuidTest extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "uuid");

    public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
    public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";

    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    protected static final String ATTR_ROOM_NUMBER = "roomNumber";
    protected static final String ATTR_MOBILE = "mobile";
    protected static final String ATTR_DEPARTMENT_NUMBER = "departmentNumber";

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    protected String userRappOid;
    protected String userMancombOid;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, getResourceOpenDjFile(), RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);

    }

    protected abstract File getResourceOpenDjFile();

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
        final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
    }

}
