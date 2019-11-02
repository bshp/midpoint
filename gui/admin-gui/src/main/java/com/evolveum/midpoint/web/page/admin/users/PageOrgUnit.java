/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractRoleMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.web.page.admin.users.component.OrgMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL,
                label = "PageOrgUnit.auth.orgUnit.label",
                description = "PageOrgUnit.auth.orgUnit.description") })
public class PageOrgUnit extends PageAdminAbstractRole<OrgType> implements ProgressReportingAwarePage {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);

    public PageOrgUnit() {
        super();
    }

    public PageOrgUnit(PageParameters parameters) {
        super(parameters);
    }

    public PageOrgUnit(final PrismObject<OrgType> role) {
        super(role);
    }

    public PageOrgUnit(final PrismObject<OrgType> userToEdit, boolean isNewObject) {
        super(userToEdit, isNewObject);
    }

    public PageOrgUnit(final PrismObject<OrgType> abstractRole, boolean isNewObject, boolean isReadonly) {
        super(abstractRole, isNewObject, isReadonly);
    }

    @Override
    protected OrgType createNewObject() {
        return new OrgType();
    }

    @Override
    public Class<OrgType> getCompileTimeClass() {
        return OrgType.class;
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageOrgTree.class;
    }

    @Override
    protected FocusSummaryPanel<OrgType> createSummaryPanel() {
        return new OrgSummaryPanel(ID_SUMMARY_PANEL, Model.of(getObjectModel().getObject().getObject().asObjectable()), this);
    }

    @Override
    protected AbstractObjectMainPanel<OrgType> createMainPanel(String id) {
        return new AbstractRoleMainPanel<OrgType>(id, getObjectModel(),
                getProjectionModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isFocusHistoryPage(){
                return PageOrgUnit.this.isFocusHistoryPage();
            }

            @Override
            protected void viewObjectHistoricalDataPerformed(AjaxRequestTarget target, PrismObject<OrgType> object, String date){
                PageOrgUnit.this.navigateToNext(new PageOrgUnitHistory(object, date));
            }

            @Override
            public OrgMemberPanel createMemberPanel(String panelId) {

                return new OrgMemberPanel(panelId, new Model<>(getObject().asObjectable())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected AvailableRelationDto getSupportedRelations() {
                        return getSupportedMembersTabRelations();
                    }

                };
            }

            @Override
            public OrgMemberPanel createGovernancePanel(String panelId) {

                return new OrgMemberPanel(panelId, new Model<>(getObject().asObjectable())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected AvailableRelationDto getSupportedRelations() {
                        return getSupportedGovernanceTabRelations();
                    }

                    @Override
                    protected Map<String, String> getAuthorizations(QName complexType) {
                        return getGovernanceTabAuthorizations();
                    }

                };
            }


        };
    }

}
