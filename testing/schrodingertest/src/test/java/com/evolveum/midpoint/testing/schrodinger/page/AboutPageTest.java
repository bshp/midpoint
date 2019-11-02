/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by matus on 3/16/2018.
 */
public class AboutPageTest extends TestBase {

    private static final String VERSION_EXPECTED = "3.8-SNAPSHOT"; // Static value, should be changed each version change.
    private static final String HIBERNATE_DIALECT_EXPECTED = "org.hibernate.dialect.H2Dialect";
    private static final String CONNID_VERSION_EXPECTED = "1.4.3.11"; // Static value, should be changed each version change.
    private static final String REINDEX_REPO_TASK_CATEGORY_EXPECTED = "Utility";
    private static final String REINDEX_REPO_TASK_DISPLAY_NAME_EXPECTED = "Reindex repository objects";

    private static final String PROPERTY_JVM_NAME_XMX = "-Xmx";

    private AboutPage aboutPage;

    @BeforeMethod
    private void openPage() {
        aboutPage = basicPage.aboutPage();
    }

    @Test
    public void checkMidpointVersion() {
        Assert.assertEquals(aboutPage.version(), VERSION_EXPECTED);
    }

    @Test
    public void checkGitDescribeValue() {
        Assert.assertTrue(!aboutPage
                .gitDescribe()
                .isEmpty()
        );
    }

    @Test
    public void checkBuildAt() {
        Assert.assertTrue(!aboutPage
                .buildAt()
                .isEmpty()
        );
    }

    @Test // TODO fix select the right element
    public void checkHibernateDialect() {
        Assert.assertEquals(aboutPage.hibernateDialect(), HIBERNATE_DIALECT_EXPECTED);
    }

    @Test
    public void checkConnIdVersion() {
        Assert.assertEquals(aboutPage.connIdFrameworkVersion(), CONNID_VERSION_EXPECTED);
    }

    @Test
    public void repoSelfTestFeedbackPositive() {

        Assert.assertTrue(aboutPage
                .repositorySelfTest()
                .feedback()
                .isSuccess()
        );
    }

    @Test
    public void reindexRepositoryObjectsFeedbackInfo() {
        Assert.assertTrue(aboutPage
                .reindexRepositoryObjects()
                .feedback()
                .isInfo()
        );

    }

    @Test
    public void checkReindexRepositoryObjectsCategory() {

        Assert.assertEquals(aboutPage
                        .reindexRepositoryObjects()
                        .feedback()
                        .clickShowTask()
                        .utility()
                , REINDEX_REPO_TASK_CATEGORY_EXPECTED);
    }

    @Test
    public void checkReindexRepositoryObjectsDisplayName() {
        Assert.assertEquals(aboutPage
                        .reindexRepositoryObjects()
                            .feedback()
                                .clickShowTask()
                                    .and()
                                        .summary()
                                        .fetchDisplayName()
                , REINDEX_REPO_TASK_DISPLAY_NAME_EXPECTED);
    }

    @Test
    public void checkJVMPropertiesMidpointHome(){

        Assert.assertTrue(
        !aboutPage.getJVMproperty(TestBase.PROPERTY_NAME_MIDPOINT_HOME).isEmpty()
    );

    }

    @Test
    public void checkJVMPropertiesXmx(){

        Assert.assertTrue(
                !aboutPage.getJVMproperty(PROPERTY_JVM_NAME_XMX).isEmpty()
        );

    }
    @Test
    public void checkSystemProperty(){
        Assert.assertTrue(
                !aboutPage.getSystemProperty(TestBase.PROPERTY_NAME_USER_HOME).isEmpty()
        );
    }
}
