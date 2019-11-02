/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.RoleAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for bi-directional entitlement association synchronization.
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInboundOutboundAssociation extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "inbound-outbound-association");

    protected static final File USER_MANCOMB_FILE = new File(TEST_DIR, "user-mancomb.xml");
    protected static final String USER_MANCOMB_OID = "8e3a3770-cc60-11e8-8354-a7bb150473c1";
    protected static final String USER_MANCOMB_USERNAME = "mancomb";

    protected static final File ROLE_META_GROUP_FILE = new File(TEST_DIR, "role-meta-group.xml");
    protected static final String ROLE_META_GROUP_OID = "471a49a2-d8fe-11e8-9b6b-730d02c33833";

    protected static final File RESOURCE_DUMMY_DIR_FILE = new File(TEST_DIR, "resource-dummy-dir.xml");
    protected static final String RESOURCE_DUMMY_DIR_OID = "82230126-d85c-11e8-bc12-537988b7843a";
    protected static final String RESOURCE_DUMMY_DIR_NAME = "dir";

    protected static final File TASK_DUMMY_DIR_LIVESYNC_FILE = new File(TEST_DIR, "task-dumy-dir-livesync.xml");
    protected static final String TASK_DUMMY_DIR_LIVESYNC_OID = "7d79f012-d861-11e8-b788-07bda6c5bb24";

    public static final File OBJECT_TEMPLATE_ROLE_GROUP_FILE = new File(TEST_DIR, "object-template-role-group.xml");
    public static final String OBJECT_TEMPLATE_ROLE_GROUP_OID = "ef638872-cc69-11e8-8ee2-333f3bf7747f";

    public static final String SUBTYPE_GROUP = "group";

    private static final String ACCOUNT_GUYBRUSH_USERNAME = "guybrush";
    private static final String ACCOUNT_GUYBRUSH_FULLNAME = "Guybrush Threepwood";

    private static final String GROUP_PIRATES_NAME = "pirates";

    private static final QName ASSOCIATION_GROUP_QNAME = new QName(MidPointConstants.NS_RI, "group");

    private String rolePiratesOid;
    private String shadowGroupPiratesOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_DIR_NAME, RESOURCE_DUMMY_DIR_FILE, RESOURCE_DUMMY_DIR_OID, initTask, initResult);
        getDummyResourceDir().setSyncStyle(DummySyncStyle.SMART);

        importObjectFromFile(ROLE_META_GROUP_FILE, initResult);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_ROLE_GROUP_FILE, initResult);
        // subtype==employee: Make sure that this is not applied to administrator or other non-person accounts.
        setDefaultObjectTemplate(RoleType.COMPLEX_TYPE, SUBTYPE_GROUP, OBJECT_TEMPLATE_ROLE_GROUP_OID, initResult);

        addObject(TASK_DUMMY_DIR_LIVESYNC_FILE);

        importObjectFromFile(USER_MANCOMB_FILE, initResult);
    }

    @Test
    public void test100ImportGroupPirates() throws Exception {
        final String TEST_NAME = "test100ImportGroupPirates";
        displayTestTitle(TEST_NAME);

        DummyGroup group = new DummyGroup(GROUP_PIRATES_NAME);
        getDummyResourceDir().addGroup(group);

        // WHEN
        displayWhen(TEST_NAME);

        liveSyncDir();

        // THEN
        displayThen(TEST_NAME);

        display("dir after", getDummyResourceDir());

        RoleAsserter<Void> rolePiratesAsserter = assertRoleAfterByName(groupRoleName(GROUP_PIRATES_NAME));
        rolePiratesAsserter
            .assertSubtype(SUBTYPE_GROUP)
            .assertIdentifier(GROUP_PIRATES_NAME)
            .assignments()
                .assertAssignments(1)
                .assertRole(ROLE_META_GROUP_OID)
                .end();

        rolePiratesOid = rolePiratesAsserter.getOid();

        shadowGroupPiratesOid = rolePiratesAsserter
                .links()
                    .single()
                        .getOid();
    }

    @Test
    public void test110AssignJackDirAccount() throws Exception {
        final String TEST_NAME = "test110AssignJackDirAccount";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();
    }

    /**
     * Make sure situation is stable.
     */
    @Test
    public void test115Stability() throws Exception {
        final String TEST_NAME = "test110AssignJackDirAccount";
        displayTestTitle(TEST_NAME);

        // WHEN
        displayWhen(TEST_NAME);
        liveSyncDir();

        // THEN
        displayThen(TEST_NAME);

        display("dir after", getDummyResourceDir());

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1);
    }

    @Test
    public void test120AddJackToGroupPirates() throws Exception {
        final String TEST_NAME = "test120AddJackToGroupPirates";
        displayTestTitle(TEST_NAME);

        getDummyResourceDir().getGroupByName(GROUP_PIRATES_NAME)
            .addMember(USER_JACK_USERNAME);

        // "fake" modification of jack's account. Just to "motivate" it to be synchronized
        getDummyResourceDir().getAccountByUsername(USER_JACK_USERNAME)
            .replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        // WHEN
        displayWhen(TEST_NAME);

        liveSyncDir();

        // THEN
        displayThen(TEST_NAME);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(2)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * MID-4948
     */
    @Test
    public void test130JackUnassignRolePirates() throws Exception {
        final String TEST_NAME = "test120AddJackToGroupPirates";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, rolePiratesOid, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertNoRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();
    }

    @Test
    public void test140JackAssignRolePirates() throws Exception {
        final String TEST_NAME = "test140JackAssignRolePirates";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, rolePiratesOid, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(2)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
        .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * Unassign dir account. But there is still pirates group assignment,
     * therefore the account should be kept.
     * @throws Exception
     */
    @Test
    public void test142JackUnAssignDirAccount() throws Exception {
        final String TEST_NAME = "test140JackAssignRolePirates";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
        .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * MID-4948
     */
    @Test
    public void test149JackUnassignRolePirates() throws Exception {
        final String TEST_NAME = "test149JackUnassignRolePirates";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, rolePiratesOid, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(0);

        assertNoDummyAccount(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();
    }

    @Test
    public void test150AssignJackDirAccount() throws Exception {
        final String TEST_NAME = "test150AssignJackDirAccount";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();
    }

    @Test
    public void test152JackAssignRolePirates() throws Exception {
        final String TEST_NAME = "test152JackAssignRolePirates";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, rolePiratesOid, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(2)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
        .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * MID-4948
     */
    @Test
    public void test153JackUnassignRolePiratesPreview() throws Exception {
        final String TEST_NAME = "test153JackUnassignRolePiratesPreview";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> focusDelta = createAssignmentFocusDelta(
                UserType.class, USER_JACK_OID,
                FocusType.F_ASSIGNMENT,
                rolePiratesOid, RoleType.COMPLEX_TYPE,
                null, (Consumer<AssignmentType>)null, false);

        // WHEN
        displayWhen(TEST_NAME);
        ModelContext<UserType> previewContext = previewChanges(focusDelta, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertPreviewContext(previewContext)
            .projectionContexts()
                .single()
                    .assertNoPrimaryDelta()
                    .secondaryDelta()
                        .display()
                        .assertModify()
                        .container(ShadowType.F_ASSOCIATION)
                            .assertNoValuesToAdd()
                            .assertNoValuesToReplace()
                            .valuesToDelete()
                                .single()
                                    .assertPropertyEquals(ShadowAssociationType.F_NAME, ASSOCIATION_GROUP_QNAME)
                                    .assertRefEquals(ShadowAssociationType.F_SHADOW_REF, shadowGroupPiratesOid)
                                    .end()
                                .end()
                            .end()
                        .end()
                    .objectNew()
                        .display()
                        .assertNoItem(ShadowType.F_ASSOCIATION);

    }

    /**
     * MID-4948
     */
    @Test
    public void test154JackUnassignRolePirates() throws Exception {
        final String TEST_NAME = "test154JackUnassignRolePirates";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, rolePiratesOid, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertNoRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
        .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();
    }

    @Test
    public void test159JackUnassignDirAccount() throws Exception {
        final String TEST_NAME = "test159JackUnassignDirAccount";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(0);

        assertNoDummyAccount(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
            .assertNoMembers();
    }

    /**
     * MID-5635
     */
    @Test
    public void test200MancombAssignAccount() throws Exception {
        final String TEST_NAME = "test200MancombAssignAccount";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignAccountToUser(USER_MANCOMB_OID, RESOURCE_DUMMY_DIR_OID, "default", task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        display("dir after", getDummyResourceDir());
        assertUserAfter(USER_MANCOMB_OID)
                .assignments()
                    .assertAssignments(1);

        assertDummyAccount(RESOURCE_DUMMY_DIR_NAME, USER_MANCOMB_USERNAME);

    }

    private String groupRoleName(String groupName) {
        return "group:"+groupName;
    }


    private void liveSyncDir() throws CommonException {
        rerunTask(TASK_DUMMY_DIR_LIVESYNC_OID);
    }


    private DummyResource getDummyResourceDir() {
        return getDummyResource(RESOURCE_DUMMY_DIR_NAME);
    }

}
