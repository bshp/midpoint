/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Selenide;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.evolveum.midpoint.testing.schrodinger.TestBase;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Created by matus on 5/21/2018.
 */
public class SynchronizationTests extends TestBase {

    private static File CSV_TARGET_FILE;


    private static final File CSV_INITIAL_SOURCE_FILE = new File("./src/test/resources/midpoint-groups-authoritative-initial.csv");
    private static final File CSV_UPDATED_SOURCE_FILE = new File("./src/test/resources/midpoint-groups-authoritative-updated.csv");

    private static final String RESOURCE_AND_SYNC_TASK_SETUP_DEPENDENCY = "setUpResourceAndSynchronizationTask";
    private static final String NEW_USER_AND_ACCOUNT_CREATED_DEPENDENCY = "newResourceAccountUserCreated";
    private static final String NEW_USER_ACCOUNT_CREATED_LINKED_DEPENDENCY = "newResourceAccountCreatedLinked";
    private static final String LINKED_USER_ACCOUNT_MODIFIED = "alreadyLinkedResourceAccountModified";
    private static final String LINKED_USER_ACCOUNT_DELETED = "alreadyLinkedResourceAccountDeleted";
    private static final String RESOURCE_ACCOUNT_CREATED_WHEN_UNREACHABLE = "resourceAccountCreatedWhenResourceUnreachable";

    private static final String FILE_RESOUCE_NAME = "midpoint-advanced-sync.csv";
    private static final String DIRECTORY_CURRENT_TEST = "synchronizationTests";

    @Test(priority = 0)
    public void setUpResourceAndSynchronizationTask() throws ConfigurationException, IOException {

        initTestDirectory(DIRECTORY_CURRENT_TEST);

        CSV_TARGET_FILE = new File(CSV_TARGET_DIR, FILE_RESOUCE_NAME);
        FileUtils.copyFile(CSV_INITIAL_SOURCE_FILE,CSV_TARGET_FILE);

        importObject(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_FILE,true);
        importObject(ScenariosCommons.USER_TEST_RAPHAEL_FILE,true);

        //changeResourceFilePath(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME, ScenariosCommons.CSV_SOURCE_OLDVALUE, CSV_TARGET_FILE.getAbsolutePath(), true);

        changeResourceAttribute(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, CSV_TARGET_FILE.getAbsolutePath(), true);


        refreshResourceSchema(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME);
        ListResourcesPage listResourcesPage = basicPage.listResources();
        listResourcesPage
                .table()
                    .clickByName(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                        .clickAccountsTab()
                        .liveSyncTask()
                            .clickCreateNew()
                                .basicTable()
                                    .addAttributeValue("Task name","LiveSyncTest")
                            .and()
                                .schedulingTable()
                                    .clickCheckBox("Recurring task")
                                    .addAttributeValue("Schedule interval (seconds)","1")
                            .and()
                                .clickSave()
                                    .feedback()
                                    .isSuccess();
    }


    @Test (priority = 1, dependsOnMethods = {RESOURCE_AND_SYNC_TASK_SETUP_DEPENDENCY})
    public void newResourceAccountUserCreated() throws IOException {

    FileUtils.copyFile(ScenariosCommons.CSV_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(3000);

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertTrue(
              usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                    .updateSearch()
                .and()
                .currentTableContains(ScenariosCommons.TEST_USER_DON_NAME)
        );
    }

    @Test (priority = 2, dependsOnMethods = {NEW_USER_AND_ACCOUNT_CREATED_DEPENDENCY})
    public void protectedAccountAdded(){

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertFalse(
              usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(ScenariosCommons.TEST_USER_PROTECTED_NAME)
                    .updateSearch()
                .and()
                .currentTableContains(ScenariosCommons.TEST_USER_PROTECTED_NAME)
        );
        ListResourcesPage resourcesPage = basicPage.listResources();

        Assert.assertTrue(

           resourcesPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                        .clickAccountsTab()
                        .clickSearchInResource()
                            .table()
                            .currentTableContains(ScenariosCommons.TEST_USER_PROTECTED_NAME)
        );

    }


    @Test (priority = 3, dependsOnMethods = {NEW_USER_AND_ACCOUNT_CREATED_DEPENDENCY})
    public void newResourceAccountCreatedLinked() throws IOException {

        ListUsersPage usersPage = basicPage.listUsers();
        usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                    .updateSearch()
                .and()
                    .clickByName(ScenariosCommons.TEST_USER_DON_NAME)
                        .selectTabProjections()
                            .table()
                                    .selectHeaderCheckBox()
                        .and()
                            .clickHeaderActionDropDown()
                                .delete()
                                .clickYes()
                        .and()
                    .and()
                        .clickSave()
                            .feedback()
                            .isSuccess();

        FileUtils.copyFile(ScenariosCommons.CSV_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(MidPoint.TIMEOUT_EXTRA_LONG_1_M);

        usersPage = basicPage.listUsers();
        Assert.assertTrue(
              usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                    .updateSearch()
                .and()
                .clickByName(ScenariosCommons.TEST_USER_DON_NAME)
                      .selectTabProjections()
                        .table()
                        .containsText(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
        );

    }

    @Test (priority = 4, dependsOnMethods = {NEW_USER_ACCOUNT_CREATED_LINKED_DEPENDENCY})
    public void alreadyLinkedResourceAccountModified() throws IOException {

        FileUtils.copyFile(CSV_UPDATED_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(10000);

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertTrue(
            usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(ScenariosCommons.TEST_USER_DON_NAME)
                        .selectTabBasic()
                            .form()
                                .compareInputAttributeValue("givenName","Donato")
        );
    }

    @Test (priority = 5, dependsOnMethods = {LINKED_USER_ACCOUNT_MODIFIED})
    public void alreadyLinkedResourceAccountDeleted() throws IOException {

        FileUtils.copyFile(CSV_INITIAL_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(3000);

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertFalse(
                usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                        .updateSearch()
                    .and()
                    .currentTableContains(ScenariosCommons.TEST_USER_DON_NAME)
        );
    }

    @Test (priority = 6, dependsOnMethods = {RESOURCE_AND_SYNC_TASK_SETUP_DEPENDENCY})
    public void resourceAccountDeleted(){

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertFalse(
        usersPage
                .table()
                        .search()
                            .byName()
                            .inputValue("raphael")
                        .updateSearch()
                    .and()
                        .clickByName("raphael")
                            .selectTabProjections()
                                .table()
                                .currentTableContains(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
        );

        ListResourcesPage resourcesPage = basicPage.listResources();
        Assert.assertFalse(
           resourcesPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                        .clickAccountsTab()
                        .clickSearchInResource()
                            .table()
                            .selectCheckboxByName("raphael")
                                .clickCog()
                                    .clickDelete()
                            .clickYes()
                        .and()
                        .clickSearchInResource()
                            .table()
                            .currentTableContains("raphael")
        );

        usersPage = basicPage.listUsers();
        Assert.assertFalse(
                         usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue("raphael")
                        .updateSearch()
                    .and()
                        .clickByName("raphael")
                            .selectTabProjections()
                                .table()
                                .currentTableContains(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
        );
    }



@Test(priority = 7, dependsOnMethods = {LINKED_USER_ACCOUNT_DELETED})
    public void resourceAccountCreatedWhenResourceUnreachable() throws IOException {

        changeResourceAttribute(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME,  ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, CSV_TARGET_FILE.getAbsolutePath()+"err", false);

        FileUtils.copyFile(ScenariosCommons.CSV_SOURCE_FILE,CSV_TARGET_FILE);

        Selenide.sleep(3000);

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertFalse(
                usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                        .updateSearch()
                    .and()
                    .currentTableContains(ScenariosCommons.TEST_USER_DON_NAME)
        );

    changeResourceAttribute(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, CSV_TARGET_FILE.getAbsolutePath(), true);


    ListTasksPage  tasksPage = basicPage.listTasks();
        tasksPage
            .table()
                .clickByName("LiveSyncTest")
                .clickResume();


        Selenide.sleep(3000);

        usersPage = basicPage.listUsers();
        Assert.assertTrue(
                usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.TEST_USER_DON_NAME)
                        .updateSearch()
                    .and()
                    .currentTableContains(ScenariosCommons.TEST_USER_DON_NAME)
        );
    }

    @Test (priority = 8, dependsOnMethods = {RESOURCE_ACCOUNT_CREATED_WHEN_UNREACHABLE})
    public void resourceAccountCreatedWhenResourceUnreachableToBeLinked() throws IOException {

        ListUsersPage listUsersPage= basicPage.listUsers();
        Assert.assertTrue(
            listUsersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                        .updateSearch()
                    .and()
                        .clickByName(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                            .selectTabProjections()
                                .table()
                                .selectCheckboxByName(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                            .and()
                                .clickHeaderActionDropDown()
                                    .delete()
                                    .clickYes()
                            .and()
                        .and()
                        .clickSave()
                            .feedback()
                    .isSuccess()
        );

        changeResourceAttribute(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME , ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, CSV_TARGET_FILE.getAbsolutePath()+"err",false);

        FileUtils.copyFile(ScenariosCommons.CSV_SOURCE_FILE,CSV_TARGET_FILE);

        changeResourceAttribute(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME , ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, CSV_TARGET_FILE.getAbsolutePath(),true);


        ListTasksPage  tasksPage = basicPage.listTasks();
        tasksPage
                .table()
                    .clickByName("LiveSyncTest")
                    .clickResume();

        Selenide.sleep(3000);

        listUsersPage = basicPage.listUsers();
        Assert.assertTrue(
                listUsersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                            .selectTabProjections()
                                .table()
                        .currentTableContains(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
        );
    }
}
