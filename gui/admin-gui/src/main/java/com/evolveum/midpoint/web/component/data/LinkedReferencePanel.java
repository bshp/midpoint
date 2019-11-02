/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;


/**
 * Created by honchar
 */
public class LinkedReferencePanel<O extends ObjectType> extends BasePanel<ObjectReferenceType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "nameLink";
    private static final String ID_NAME_TEXT = "nameLinkText";

    private static final String DOT_CLASS = LinkedReferencePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCED_OBJECT = DOT_CLASS + "loadReferencedObject";

    IModel<ObjectType> referencedObjectModel = null;

    public LinkedReferencePanel(String id, IModel<ObjectReferenceType> objectReferenceModel){
        super(id, objectReferenceModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initReferencedObjectModel();
        initLayout();
    }

    public void initReferencedObjectModel() {
            referencedObjectModel = new LoadableModel<ObjectType>() {
                @Override
                protected ObjectType load() {
                    if (getModelObject() == null || getModelObject().getType() == null){
                        return null;
                    }
                    if (getModelObject().asReferenceValue() != null && getModelObject().asReferenceValue().getObject() != null
                            && getModelObject().asReferenceValue().getObject().asObjectable() instanceof ObjectType ){
                        return (ObjectType)getModelObject().asReferenceValue().getObject().asObjectable();
                    }
                    if (StringUtils.isNotEmpty(getModelObject().getOid()) && getModelObject().getType() != null) {
                        PageBase pageBase = LinkedReferencePanel.this.getPageBase();
                        OperationResult result = new OperationResult(OPERATION_LOAD_REFERENCED_OBJECT);
                        PrismObject<ObjectType> referencedObject = WebModelServiceUtils.loadObject(getModelObject(), pageBase,
                                pageBase.createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT), result);
                        return referencedObject != null ? referencedObject.asObjectable() : null;
                    }
                    return null;
                }
            };
    }

    private void initLayout(){
        setOutputMarkupId(true);

        DisplayType displayType = WebComponentUtil.getDisplayTypeForObject(referencedObjectModel.getObject(), null, getPageBase());
        if (displayType == null){
            displayType = new DisplayType();
        }
        if (displayType.getIcon() == null && referencedObjectModel.getObject() != null){
            displayType.setIcon(WebComponentUtil.createIconType(WebComponentUtil.createDefaultBlackIcon(
                    WebComponentUtil.classToQName(getPageBase().getPrismContext(), referencedObjectModel.getObject().getClass()))));
        }
        ImagePanel imagePanel = new ImagePanel(ID_ICON, displayType);
        imagePanel.setOutputMarkupId(true);
//        imagePanel.add(new VisibleBehaviour(() -> displayType != null && displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass())));
        add(imagePanel);

        AjaxLink<ObjectType> nameLink = new AjaxLink<ObjectType>(ID_NAME, referencedObjectModel) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (referencedObjectModel != null && referencedObjectModel.getObject() != null) {
                    WebComponentUtil.dispatchToObjectDetailsPage(referencedObjectModel.getObject().asPrismObject(),
                            LinkedReferencePanel.this);
                }
            }
        };
        nameLink.add(new EnableBehaviour(() ->
                referencedObjectModel != null && referencedObjectModel.getObject() != null
                && !new QName("dummy").getLocalPart().equals(
                        referencedObjectModel.getObject().asPrismContainer().getElementName().getLocalPart())));
        nameLink.setOutputMarkupId(true);
        add(nameLink);

        Label nameLinkText = new Label(ID_NAME_TEXT, Model.of(WebComponentUtil.getEffectiveName(referencedObjectModel.getObject(),
                AbstractRoleType.F_DISPLAY_NAME)));
        nameLinkText.setOutputMarkupId(true);
        nameLink.add(nameLinkText);

    }

}
