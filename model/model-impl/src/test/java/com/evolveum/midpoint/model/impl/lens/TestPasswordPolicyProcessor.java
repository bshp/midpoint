/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordPolicyProcessor extends AbstractLensTest {

    private static final String BASE_PATH = "src/test/resources/lens";

    private static final String PASSWORD_HISTORY_POLICY_OID = "policy00-0000-0000-0000-000000000003";
    private static final String PASSWORD_HISTORY_POLICY_NAME = "password-policy-history.xml";
    private static final File PASSWORD_HISTORY_POLICY_FILE = new File(BASE_PATH,
            PASSWORD_HISTORY_POLICY_NAME);

    private static final String PASSWORD_NO_HISTORY_POLICY_OID = "policy00-0000-0000-0000-000000000004";
    private static final String PASSWORD_NO_HISTORY_POLICY_NAME = "password-policy-no-history.xml";
    private static final File PASSWORD_NO_HISTORY_POLICY_FILE = new File(BASE_PATH,
            PASSWORD_NO_HISTORY_POLICY_NAME);

    private static final String PASSWORD1 = "ch4nGedPa33word1";
    private static final String PASSWORD2 = "ch4nGedPa33word2";
    private static final String PASSWORD3 = "ch4nGedPa33word3";


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(PASSWORD_HISTORY_POLICY_FILE, initResult);
        repoAddObjectFromFile(PASSWORD_NO_HISTORY_POLICY_FILE, initResult);

        deleteObject(UserType.class, USER_JACK_OID);
    }

    @Test
    public void test000initPasswordPolicyForHistory() throws Exception {
        final String TEST_NAME = "test000initPasswordPolicyForHistory";
        initPasswordPolicy(TEST_NAME, 3, PASSWORD_HISTORY_POLICY_OID);

    }

    @Test
    public void test100CreateUserWithPassword() throws Exception {
        final String TEST_NAME = "test100CreateUserWithPassword";
        TestUtil.displayTestTitle(TEST_NAME);
        // WHEN
        addObject(USER_JACK_FILE);

        // THEN
        PrismObject<UserType> jack = getObject(UserType.class, USER_JACK_OID);
        assertNotNull("User Jack was not found.", jack);

        assertPasswordHistoryEntries(jack);
    }

    @Test
    public void test101ModifyUserPassword() throws Exception {
        final String TEST_NAME = "test101ModifyUserPassword";
        TestUtil.displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        modifyUserChangePassword(USER_JACK_OID, PASSWORD1, task, result);

        // THEN
        PrismObject<UserType> jack = getObject(UserType.class, USER_JACK_OID);
        assertNotNull("User Jack was not found.", jack);

        UserType jackType = jack.asObjectable();
        CredentialsType credentialsType = jackType.getCredentials();
        assertNotNull("No credentials set for user Jack", credentialsType);

        PasswordType passwordType = credentialsType.getPassword();
        assertNotNull("No password set for user Jack", passwordType);
        ProtectedStringType passwordAfterChange = passwordType.getValue();
        assertNotNull("Password musn't be null", passwordAfterChange);
        assertPasswords(PASSWORD1, passwordAfterChange);
        assertPasswordHistoryEntries(passwordType,
                USER_JACK_PASSWORD);

    }

    @Test
    public void test102ModifyUserPassword() throws Exception {
        final String TEST_NAME = "test102ModifyUserPassword";
        TestUtil.displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        modifyUserChangePassword(USER_JACK_OID, PASSWORD2, task, result);

        // THEN
        PrismObject<UserType> jack = getObject(UserType.class, USER_JACK_OID);
        assertNotNull("User Jack was not found.", jack);

        UserType jackType = jack.asObjectable();
        CredentialsType credentialsType = jackType.getCredentials();
        assertNotNull("No credentials set for user Jack", credentialsType);

        PasswordType passwordType = credentialsType.getPassword();
        assertNotNull("No password set for user Jack", passwordType);
        ProtectedStringType passwordAfterChange = passwordType.getValue();
        assertNotNull("Password musn't be null", passwordAfterChange);
        assertPasswords(PASSWORD2, passwordAfterChange);
        assertPasswordHistoryEntries(passwordType,
                USER_JACK_PASSWORD, PASSWORD1);

    }

    @Test
    public void test103ModifyUserPasswordAgain() throws Exception {
        final String TEST_NAME = "test103ModifyUserPasswordAgain";
        TestUtil.displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        modifyUserChangePassword(USER_JACK_OID, PASSWORD3, task, result);

        // THEN
        PrismObject<UserType> jackAfterSecondChange = getObject(UserType.class, USER_JACK_OID);
        assertNotNull("User Jack was not found.", jackAfterSecondChange);

        UserType jackTypeAfterSecondChange = jackAfterSecondChange.asObjectable();
        CredentialsType credentialsTypeAfterSecondChange = jackTypeAfterSecondChange.getCredentials();
        assertNotNull("No credentials set for user Jack", credentialsTypeAfterSecondChange);

        PasswordType passwordTypeAfterSecondChnage = credentialsTypeAfterSecondChange.getPassword();
        assertNotNull("No password set for user Jack", passwordTypeAfterSecondChnage);
        ProtectedStringType passwordAfterSecondChange = passwordTypeAfterSecondChnage.getValue();
        assertNotNull("Password musn't be null", passwordAfterSecondChange);

        assertPasswords(PASSWORD3, passwordAfterSecondChange);

        assertPasswordHistoryEntries(passwordTypeAfterSecondChnage,
                PASSWORD1, PASSWORD2);
    }

    @Test
    public void test111ModifyUserPasswordOldPassword1() throws Exception {
        doTestModifyUserPasswordExpectFailure("test111ModifyUserPasswordOldPassword1", PASSWORD1);
    }

    @Test
    public void test112ModifyUserPasswordOldPassword2() throws Exception {
        doTestModifyUserPasswordExpectFailure("test112ModifyUserPasswordOldPassword2", PASSWORD2);
    }

    @Test
    public void test113ModifyUserPasswordSamePassword3() throws Exception {
        doTestModifyUserPasswordExpectFailure("test113ModifyUserPasswordSamePassword3", PASSWORD3);
    }

    public void doTestModifyUserPasswordExpectFailure(final String TEST_NAME, String password) throws Exception {
        Task task = taskManager.createTaskInstance(TEST_NAME);
        TestUtil.displayTestTitle(TEST_NAME);
        OperationResult result = task.getResult();

        try {
            // WHEN
            modifyUserChangePassword(USER_JACK_OID, password, task, result);

            fail("Expected PolicyViolationException but didn't get one.");
        } catch (PolicyViolationException ex) {
            // this is expected
            display("expected exception", ex);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    @Test
    public void test200initNoHistoryPasswordPolicy() throws Exception {
        String title = "test200initNoHistoryPasswordPolicy";
        initPasswordPolicy(title, 0, PASSWORD_NO_HISTORY_POLICY_OID);
    }

    @Test
    public void test201deleteUserJack() throws Exception {
        final String TEST_NAME = "test201deleteUserJack";
        TestUtil.displayTestTitle(TEST_NAME);

        // WHEN
        deleteObject(UserType.class, USER_JACK_OID);

        try {
            getObject(UserType.class, USER_JACK_OID);
            fail("Unexpected user Jack, should be deleted.");
        } catch (ObjectNotFoundException ex) {
            // this is OK;
        }

    }

    @Test
    public void test202createUserJackNoPasswordHistory() throws Exception {
        final String TEST_NAME = "test202createUserJackNoPasswordHistory";
        TestUtil.displayTestTitle(TEST_NAME);

        // WHEN
        addObject(USER_JACK_FILE);

        // THEN
        PrismObject<UserType> userJack = getObject(UserType.class, USER_JACK_OID);
        assertNotNull("Expected to find user Jack, but no one exists here", userJack);

        UserType userJackType = userJack.asObjectable();
        CredentialsType credentials = userJackType.getCredentials();
        assertNotNull("User Jack has no credentials", credentials);

        PasswordType password = credentials.getPassword();
        assertNotNull("User Jack has no password", password);

        List<PasswordHistoryEntryType> historyEntries = password.getHistoryEntry();
        assertEquals("Expected no history entries, but found: " + historyEntries.size(), 0,
                historyEntries.size());

    }

    @Test
    public void test203modifyUserJackPasswordNoPasswordHistory() throws Exception {
        final String TEST_NAME = "test203modifyUserJackPasswordNoPasswordHistory";
        modifyUserJackPasswordNoHistory(TEST_NAME);

    }

    @Test
    public void test204modifyUserJackSamePasswordNoPasswordHistory() throws Exception {
        final String TEST_NAME = "test204modifyUserJackSamePasswordNoPasswordHistory";
        modifyUserJackPasswordNoHistory(TEST_NAME);

    }

    private void modifyUserJackPasswordNoHistory(String TEST_NAME) throws Exception{
        TestUtil.displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        ProtectedStringType newValue = new ProtectedStringType();
        newValue.setClearValue("n0Hist0ryEntr7");

        modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, PATH_CREDENTIALS_PASSWORD_VALUE, task, result, newValue);

        // THEN
        PrismObject<UserType> userJack = getObject(UserType.class, USER_JACK_OID);
        assertNotNull("Expected to find user Jack, but no one exists here", userJack);

        UserType userJackType = userJack.asObjectable();
        CredentialsType credentials = userJackType.getCredentials();
        assertNotNull("User Jack has no credentials", credentials);

        PasswordType password = credentials.getPassword();
        assertNotNull("User Jack has no password", password);

        List<PasswordHistoryEntryType> historyEntries = password.getHistoryEntry();
        assertEquals("Expected no history entries, but found: " + historyEntries.size(), 0,
                historyEntries.size());

    }


    private void assertPasswords(String password, ProtectedStringType passwordAfterChange) throws SchemaException, EncryptionException {
        ProtectedStringType protectedStringType = new ProtectedStringType();
        protectedStringType.setClearValue(password);
        AssertJUnit.assertTrue("Password doesn't match",
                protector.compareCleartext(protectedStringType, passwordAfterChange));
    }

    private void initPasswordPolicy(String title, int historyLength, String passwordPolicyOid) throws Exception {
        display(title);
        Task task = createTask(title);
        OperationResult result = task.getResult();

//        ObjectReferenceType passwordPolicyRef = ObjectTypeUtil.createObjectRef(passwordPolicyOid,
//                ObjectTypes.PASSWORD_POLICY);
        modifyObjectReplaceProperty(SecurityPolicyType.class, SECURITY_POLICY_OID, SchemaConstants.PATH_CREDENTIALS_PASSWORD_HISTORY_LENGTH, task, result, historyLength);
    }

}
