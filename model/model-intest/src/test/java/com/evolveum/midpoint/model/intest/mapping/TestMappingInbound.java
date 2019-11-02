/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests inbound mappings. Uses live sync to do that.
 * These tests are much simpler and more focused than those in AbstractSynchronizationStoryTest.
 *
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingInbound extends AbstractMappingTest {

    protected static final File RESOURCE_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "resource-dummy-tea-green.xml");
    protected static final String RESOURCE_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-0000-00000000c404";
    protected static final String RESOURCE_DUMMY_TEA_GREEN_NAME = "tea-green";

    protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";

    protected static final String ACCOUNT_LEELOO_USERNAME = "leeloo";
    protected static final String ACCOUNT_LEELOO_FULL_NAME_MULTIPASS = "Leeloo Dallas Multipass";
    protected static final String ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI = "Leeloominaï Lekatariba Lamina-Tchaï Ekbat De Sebat";
    protected static final String ACCOUNT_LEELOO_PROOF_STRANGE = "Hereby and hèrěnow\nThis is a multi-line claim\nwith a sôme of špecial chäracters\nAnd even some CRLF file endings\r\nLike this\r\nAnd to be completely nuts, even some LFRC\n\rThis does not really proves anything\n   It is just trying to reproduce the problem\nIn addition to be quite long\nand ugly\nLorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\nUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\nDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.\nExcepteur sint occaecat cupidatat non proident,\nsunt in culpa qui officia deserunt mollit anim id est laborum.\nAnd so on …";

    protected static final File TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "task-dumy-tea-green-livesync.xml");
    protected static final String TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-5555-55550000c404";

    private static final String LOCKER_BIG_SECRET = "BIG secret";

    private ProtectedStringType mancombLocker;
    private String userLeelooOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        initDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME, RESOURCE_DUMMY_TEA_GREEN_FILE, RESOURCE_DUMMY_TEA_GREEN_OID,
                controller -> {
                    controller.extendSchemaPirate();
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, String.class, false, false)
                        .setSensitive(true);
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            DUMMY_ACCOUNT_ATTRIBUTE_PROOF_NAME, String.class, false, false);
                    controller.setSyncStyle(DummySyncStyle.SMART);
                },
                initTask, initResult);
    }

    @Test
    public void test010SanitySchema() throws Exception {
        final String TEST_NAME = "test010SanitySchema";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);

        /// WHEN
        displayWhen(TEST_NAME);
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_TEA_GREEN_OID, task);

        // THEN
        displayThen(TEST_NAME);
        TestUtil.assertSuccess(testResult);

        ResourceType resourceType = getDummyResourceType(RESOURCE_DUMMY_TEA_GREEN_NAME);
        ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
        display("Parsed resource schema (tea-green)", returnedSchema);
        ObjectClassComplexTypeDefinition accountDef = getDummyResourceController(RESOURCE_DUMMY_TEA_GREEN_NAME)
                .assertDummyResourceSchemaSanityExtended(returnedSchema, resourceType, false,
                        DummyResourceContoller.PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS + 2); // MID-5197

        ResourceAttributeDefinition<ProtectedStringType> lockerDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME);
        assertNotNull("No locker attribute definition", lockerDef);
        assertEquals("Wrong locker attribute definition type", ProtectedStringType.COMPLEX_TYPE, lockerDef.getTypeName());
    }

    @Test
    public void test100ImportLiveSyncTaskDummyTeaGreen() throws Exception {
        final String TEST_NAME = "test100ImportLiveSyncTaskDummyTeaGreen";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
        importSyncTask();

        // THEN
        displayThen(TEST_NAME);

        waitForSyncTaskStart();
    }

    @Test
    public void test110AddDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test110AddDummyTeaGreenAccountMancomb";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // Preconditions
        //assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, LOCKER_BIG_SECRET); // MID-5197

        /// WHEN
        displayWhen(TEST_NAME);

        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRun();

        // THEN
        displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        mancombLocker = assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
            .links()
                .single()
                    .assertOid(accountMancomb.getOid())
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED)
            .extension()
                .property(PIRACY_LOCKER)
                    .singleValue()
                        .protectedString()
                            .assertIsEncrypted()
                            .assertCompareCleartext(LOCKER_BIG_SECRET)
                            .getProtectedString();

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
    }

    /**
     * MID-5197
     */
    @Test
    public void test150UserReconcile() throws Exception {
        final String TEST_NAME = "test150UserReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Preconditions
        //assertUsers(5);

        /// WHEN
        displayWhen(TEST_NAME);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        // THEN
        displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
            .links()
                .single()
                    .assertOid(accountMancomb.getOid())
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED)
            .extension()
                .property(PIRACY_LOCKER)
                    .singleValue()
                        .protectedString()
                            .assertIsEncrypted()
                            .assertCompareCleartext(LOCKER_BIG_SECRET)
                            // Make sure that this is exactly the same content of protected string
                            // including all the randomized things (IV). If it is the same,
                            // there is a good chance we haven't had any phantom changes
                            // MID-5197
                            .assertEquals(mancombLocker);

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
    }

    @Test
    public void test300DeleteDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test300DeleteDummyTeaGreenAccountMancomb";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        display("Dummy (tea green) resource", getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).debugDump());

        // Make sure we have steady state
        waitForSyncTaskNextRun();

        // THEN
        displayThen(TEST_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_TEA_GREEN_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);

        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
            .assertFullName("Mancomb Seepgood")
            .links()
                .single()
                    .resolveTarget()
                        .assertTombstone()
                        .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

//        assertUsers(7 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
    }

    // Remove livesync task so it won't get into the way for next tests
    @Test
    public void test399DeleteDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test399DeleteDummyTeaGreenAccountMancomb";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
        deleteObject(TaskType.class, TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertNoObject(TaskType.class, TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID);
    }

    @Test
    public void test400AddUserLeeloo() throws Exception {
        final String TEST_NAME = "test400AddUserLeeloo";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Preconditions
        //assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_LEELOO_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_LEELOO_FULL_NAME_MULTIPASS);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        /// WHEN
        displayWhen(TEST_NAME);

        modelService.importFromResource(RESOURCE_DUMMY_TEA_GREEN_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);

        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        waitForTaskFinish(task, true);

        // THEN
        displayThen(TEST_NAME);

        userLeelooOid = assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED)
            .getOid();

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class); // metadata
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test402UserLeelooRecompute() throws Exception {
        final String TEST_NAME = "test402UserLeelooRecompute";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        recomputeUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test404UserLeelooReconcile() throws Exception {
        final String TEST_NAME = "test404UserLeelooReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        reconcileUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Changed Leeloo's full name. Reconcile should reflect that to user.
     * MID-5314
     */
    @Test
    public void test410UserLeeloominaiReconcile() throws Exception {
        final String TEST_NAME = "test410UserLeeloominaiReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).getAccountByUsername(ACCOUNT_LEELOO_USERNAME);
        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI);

        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        reconcileUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test412UserLeeloominaiRecompute() throws Exception {
        final String TEST_NAME = "test412UserLeeloominaiRecompute";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        recomputeUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test414UserLeeloominaiReconcile() throws Exception {
        final String TEST_NAME = "test414UserLeeloominaiReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        reconcileUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Changed Leeloo's full name. Reconcile should reflect that to user.
     * MID-5314
     */
    @Test
    public void test420UserLeelooStrangeReconcile() throws Exception {
        final String TEST_NAME = "test420UserLeelooStrangeReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).getAccountByUsername(ACCOUNT_LEELOO_USERNAME);
        account.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_PROOF_NAME, ACCOUNT_LEELOO_PROOF_STRANGE);

        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        reconcileUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
            .assertDescription(ACCOUNT_LEELOO_PROOF_STRANGE)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test424UserLeelooStrangeReconcile() throws Exception {
        final String TEST_NAME = "test424UserLeelooStrangeReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        displayWhen(TEST_NAME);

        reconcileUser(userLeelooOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
            .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
            .assertDescription(ACCOUNT_LEELOO_PROOF_STRANGE)
            .links()
                .single()
                    .end()
                .end()
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    protected void importSyncTask() throws FileNotFoundException {
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILE);
    }

    protected void waitForSyncTaskStart() throws Exception {
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

    protected void waitForSyncTaskNextRun() throws Exception {
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

}
