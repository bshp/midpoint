/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;

/**
 * Created by honchar.
 */
public class AbstractRoleAssignmentPanel extends AssignmentPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

    protected static final String DOT_CLASS = AbstractRoleAssignmentPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_TARGET_REF_OBJECT = DOT_CLASS + "loadAssignmentTargetRefObject";

    public AbstractRoleAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);
    }

//    protected Fragment initCustomButtonToolbar(String contentAreaId){
//        Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, this);
//
//        MultifunctionalButton newObjectIcon = getMultivalueContainerListPanel().getNewItemButton(ID_NEW_ITEM_BUTTON);
//        searchContainer.add(newObjectIcon);
//
//        AjaxIconButton showAllAssignmentsButton = new AjaxIconButton(ID_SHOW_ALL_ASSIGNMENTS_BUTTON, new Model<>("fa fa-address-card"),
//                createStringResource("AssignmentTablePanel.menu.showAllAssignments")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
//                showAllAssignments(ajaxRequestTarget);
//            }
//        };
//        searchContainer.addOrReplace(showAllAssignmentsButton);
//        showAllAssignmentsButton.setOutputMarkupId(true);
//        showAllAssignmentsButton.add(new VisibleEnableBehaviour(){
//
//            private static final long serialVersionUID = 1L;
//
//            public boolean isVisible(){
//                return showAllAssignmentsVisible();
//            }
//        });
//        return searchContainer;
//    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(
                createStringResource("AbstractRoleAssignmentPanel.relationLabel")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
                item.add(new Label(componentId, getRelationLabelValue(assignmentModel.getObject())));
            }
        });

        if (!OrgType.COMPLEX_TYPE.equals(getAssignmentType())) {
            columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getModel(), AssignmentType.F_TENANT_REF, ColumnType.STRING, getPageBase()));
            columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getModel(), AssignmentType.F_ORG_REF, ColumnType.STRING, getPageBase()));
        }

        columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(createStringResource("AbstractRoleAssignmentPanel.identifierLabel")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getIdentifierLabelModel(rowModel.getObject())));
            }
        });

        return columns;
    }

    private String getRelationLabelValue(PrismContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null || assignmentWrapper.getRealValue() == null
                || assignmentWrapper.getRealValue().getTargetRef() == null
                || assignmentWrapper.getRealValue().getTargetRef().getRelation() == null){
            return "";
        }
        return assignmentWrapper.getRealValue().getTargetRef().getRelation().getLocalPart();
    }


    protected void initCustomPaging(){
        getAssignmentsTabStorage().setPaging(getPrismContext().queryFactory()
                .createPaging(0, (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
    }

    protected ObjectQuery createObjectQuery() {
        Collection<QName> delegationRelations = getParentPage().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.DELEGATION);
        ObjectFilter deputyFilter = getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF)
                .ref(delegationRelations.toArray(new QName[0]))
                .buildFilter();

        QName targetType = getAssignmentType();
        RefFilter targetRefFilter = null;
        if (targetType != null){
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setType(targetType);
            ort.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
            targetRefFilter = (RefFilter) getParentPage().getPrismContext().queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(ort.asReferenceValue())
                    .buildFilter();
            targetRefFilter.setOidNullAsAny(true);
            targetRefFilter.setRelationNullAsAny(true);
        }
        ObjectQuery query = getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .not()
                .exists(AssignmentType.F_POLICY_RULE)
                .build();
        query.addFilter(getPrismContext().queryFactory().createNot(deputyFilter));
        query.addFilter(targetRefFilter);
        return query;
    }

    protected QName getAssignmentType(){
        return AbstractRoleType.COMPLEX_TYPE;
    }

    private <O extends ObjectType> IModel<String> getIdentifierLabelModel(PrismContainerValueWrapper<AssignmentType> assignmentContainer){
        if (assignmentContainer == null || assignmentContainer.getRealValue() == null){
            return Model.of("");
        }
        AssignmentType assignment = assignmentContainer.getRealValue();
        if (assignment.getTargetRef() == null){
            return Model.of("");
        }

        PrismObject<O> object = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(),
                getPageBase().createSimpleTask(OPERATION_LOAD_TARGET_REF_OBJECT), new OperationResult(OPERATION_LOAD_TARGET_REF_OBJECT));
        if (object == null || !(object.asObjectable() instanceof AbstractRoleType)){
            return Model.of("");
        }
        AbstractRoleType targetRefObject = (AbstractRoleType) object.asObjectable();
        if (StringUtils.isNotEmpty(targetRefObject.getIdentifier())){
            return Model.of(targetRefObject.getIdentifier());
        }
        if (targetRefObject.getDisplayName() != null && !targetRefObject.getName().getOrig().equals(targetRefObject.getDisplayName().getOrig())){
            return Model.of(targetRefObject.getName().getOrig());
        }
        return Model.of("");
    }

//    protected List<ObjectTypes> getObjectTypesList(){
//        return WebComponentUtil.createAssignableTypesList();
//    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        SearchFactory.addSearchRefDef(containerDef, AssignmentType.F_TARGET_REF, defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;
    }

}
