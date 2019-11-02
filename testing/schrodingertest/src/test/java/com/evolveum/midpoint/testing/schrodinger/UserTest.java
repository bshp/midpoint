/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserTest extends TestBase {

    private static final String LOCALIZATION_TEST_USER_NAME_ORIG = "localizationTestUserName";
    private static final String LOCALIZATION_TEST_USER_NAME_DE = "localizationTestUserNameDe";
    private static final String LOCALIZATION_VALUE = "de";

    @Test(enabled = false)
    public void createUser() {

        //@formatter:off
        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                    .addAttributeValue("name", "jdoe222323")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "john")
                    .addAttributeValue(UserType.F_FAMILY_NAME, "doe")
                    .and()
                .and()
            .clickSave();

//        user.selectTabProjections().and()
//            .selectTabPersonas().and()
//            .selectTabAssignments().and()
//            .selectTabTasks().and()
//            .selectTabDelegations().and()
//            .selectTabDelegatedToMe().and()
        //@formatter:on

        screenshot("create");

        ListUsersPage users = user.listUsers();

        // todo validation
    }

    @Test //covers MID-5845
    public void isLocalizedPolystringValueDisplayed(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(
                user.selectTabBasic()
                        .form()
                        .addAttributeValue("name", LOCALIZATION_TEST_USER_NAME_ORIG)
                        .setPolyStringLocalizedValue(UserType.F_NAME, LOCALIZATION_VALUE, LOCALIZATION_TEST_USER_NAME_DE)
                        .and()
                        .and()
                        .clickSave()
                        .feedback()
                        .isSuccess()
        );

        basicPage.loggedUser().logout();
        LoginPage loginPage = midPoint.login();
        loginPage.login(midPoint.getUsername(),midPoint.getPassword(), LOCALIZATION_VALUE);

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertTrue(
                usersPage
                        .table()
                            .search()
                            .byName()
                            .inputValue(LOCALIZATION_TEST_USER_NAME_ORIG)
                            .updateSearch()
                        .and()
                        .clickByName(LOCALIZATION_TEST_USER_NAME_ORIG)
                            .selectTabBasic()
                                .form()
                                .compareInputAttributeValue("name", LOCALIZATION_TEST_USER_NAME_DE)
        );
    }

}
