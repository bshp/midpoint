/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.LimitationsEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 *  @author shood
 * */
public class ResourceAssociationEditor extends SimplePanel{

    private static final String ID_LABEL = "label";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_DIRECTION = "direction";
    private static final String ID_ASSOCIATION_ATTRIBUTE = "associationAttribute";
    private static final String ID_VALUE_ATTRIBUTE = "valueAttribute";
    private static final String ID_EXPLICIT_REF_INTEGRITY = "explicitRefIntegrity";

    private static final String ID_REFERENCE = "reference";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXCLUSIVE_STRONG = "exclusiveStrong";
    private static final String ID_TOLERANT = "tolerant";
    private static final String ID_TOLERANT_VP = "tolerantValuePattern";
    private static final String ID_INTOLERANT_VP = "intolerantValuePattern";
    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_MATCHING_RULE = "matchingRule";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_BUTTON_OUTBOUND = "buttonOutbound";
    private static final String ID_BUTTON_LIMITATIONS = "buttonLimitations";
    private static final String ID_MODAL_LIMITATIONS = "limitationsEditor";
    private static final String ID_MODAL_MAPPING = "mappingEditor";

    public ResourceAssociationEditor(String id, IModel<ResourceObjectAssociationType> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){
        Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceObjectAssociationType association = (ResourceObjectAssociationType)getModelObject();

                if(association.getDisplayName() == null && association.getRef() == null){
                    return getString("ResourceAssociationEditor.label.new");
                } else {
                    return getString("ResourceAssociationEditor.label.edit", association.getRef().getLocalPart());
                }
            }
        });
        add(label);

        DropDownChoice kind = new DropDownChoice<>(ID_KIND,
                new PropertyModel<ShadowKindType>(getModel(), "kind"),
                WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                new EnumChoiceRenderer<ShadowKindType>(this));
        add(kind);

        MultiValueTextPanel intent = new MultiValueTextPanel<>(ID_INTENT,
                new PropertyModel<List<String>>(getModel(), "intent"));
        add(intent);

        DropDownChoice direction = new DropDownChoice<>(ID_DIRECTION,
                new PropertyModel<ResourceObjectAssociationDirectionType>(getModel(), "direction"),
                WebMiscUtil.createReadonlyModelFromEnum(ResourceObjectAssociationDirectionType.class),
                new EnumChoiceRenderer<ResourceObjectAssociationDirectionType>(this));
        add(direction);

        //TODO - figure out what associationAttribute is exactly and make this autoCompleteField with proper resource values + validator
        TextField associationAttribute = new TextField<>(ID_ASSOCIATION_ATTRIBUTE,
                new PropertyModel<String>(getModel(), "associationAttribute.localPart"));
        add(associationAttribute);

        //TODO - figure out what valueAttribute is exactly and make this autoCompleteField with proper resource values + validator
        TextField valueAttribute = new TextField<>(ID_VALUE_ATTRIBUTE,
                new PropertyModel<String>(getModel(), "valueAttribute.localPart"));
        add(valueAttribute);

        CheckBox explicitRefIntegrity = new CheckBox(ID_EXPLICIT_REF_INTEGRITY,
                new PropertyModel<Boolean>(getModel(), "explicitReferentialIntegrity"));
        add(explicitRefIntegrity);

        //TODO - figure out what ref is exactly and make this autoCompleteField with proper resource values + validator
        TextField ref = new TextField<>(ID_REFERENCE, new PropertyModel<String>(getModel(), "ref.localPart"));
        add(ref);

        TextField displayName = new TextField<>(ID_DISPLAY_NAME, new PropertyModel<String>(getModel(), "displayName"));
        add(displayName);

        TextArea description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(getModel(), "description"));
        add(description);

        AjaxLink limitations = new AjaxLink(ID_BUTTON_LIMITATIONS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                limitationsEditPerformed(target);
            }
        };
        add(limitations);

        CheckBox exclusiveStrong = new CheckBox(ID_EXCLUSIVE_STRONG, new PropertyModel<Boolean>(getModel(), "exclusiveStrong"));
        add(exclusiveStrong);

        CheckBox tolerant = new CheckBox(ID_TOLERANT, new PropertyModel<Boolean>(getModel(), "tolerant"));
        add(tolerant);

        MultiValueTextPanel tolerantVP = new MultiValueTextPanel<>(ID_TOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "tolerantValuePattern"));
        add(tolerantVP);

        MultiValueTextPanel intolerantVP = new MultiValueTextPanel<>(ID_INTOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "intolerantValuePattern"));
        add(intolerantVP);

        DropDownChoice fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
                new PropertyModel<AttributeFetchStrategyType>(getModel(), "fetchStrategy"),
                WebMiscUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<AttributeFetchStrategyType>(this));
        add(fetchStrategy);

        //TODO - figure out what matchingRule is exactly and make this autoCompleteField with proper resource values + validator
        TextField matchingRule = new TextField<>(ID_MATCHING_RULE, new PropertyModel<String>(getModel(), "matchingRule.localPart"));
        add(matchingRule);

        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL,
                new PropertyModel<String>(getModel(), "outbound.name"));
        outboundLabel.setEnabled(false);
        add(outboundLabel);

        AjaxLink outbound = new AjaxLink(ID_BUTTON_OUTBOUND) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                outboundEditPerformed(target);
            }
        };
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "inbound"), false, true){

            @Override
            protected IModel<String> createTextModel(final IModel<MappingType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        MappingType mapping = model.getObject();

                        if(mapping != null){
                            return mapping.getName();
                        } else {
                            return null;
                        }
                    }
                };
            }

            @Override
            protected MappingType createNewEmptyItem(){
                return new MappingType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, MappingType object){
                mappingEditPerformed(target, object);
            }
        };
        add(inbound);

        initModals();
    }

    private void initModals(){
        ModalWindow limitationsEditor = new LimitationsEditorDialog(ID_MODAL_LIMITATIONS,
                new PropertyModel<List<PropertyLimitationsType>>(getModel(), "limitations"));
        add(limitationsEditor);

        ModalWindow mappingEditor = new MappingEditorDialog(ID_MODAL_MAPPING, null);
        add(mappingEditor);
    }

    private void limitationsEditPerformed(AjaxRequestTarget target){
        LimitationsEditorDialog window = (LimitationsEditorDialog)get(ID_MODAL_LIMITATIONS);
        window.show(target);
    }

    private void outboundEditPerformed(AjaxRequestTarget target){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, new PropertyModel<MappingType>(getModel(), "outbound"));
        window.show(target);
    }

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, mapping);
        window.show(target);
    }
}
