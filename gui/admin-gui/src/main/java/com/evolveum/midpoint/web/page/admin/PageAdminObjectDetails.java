/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DetailsPageSaveMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public abstract class PageAdminObjectDetails<O extends ObjectType> extends PageAdmin
        implements ProgressReportingAwarePage {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageAdminObjectDetails.class.getName() + ".";

    public static final String PARAM_RETURN_PAGE = "returnPage";

    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_LOAD_PARENT_ORGS = DOT_CLASS + "loadParentOrgs";
    private static final String OPERATION_LOAD_GUI_CONFIGURATION = DOT_CLASS + "loadGuiConfiguration";
    protected static final String OPERATION_SAVE = DOT_CLASS + "save";
    protected static final String OPERATION_PREVIEW_CHANGES = DOT_CLASS + "previewChanges";
    protected static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";

    protected static final String ID_SUMMARY_PANEL = "summaryPanel";
    protected static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_PROGRESS_PANEL = "progressPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminObjectDetails.class);

    private LoadableModel<PrismObjectWrapper<O>> objectModel;
    private LoadableModel<List<FocusSubwrapperDto<OrgType>>> parentOrgModel;

    private ProgressPanel progressPanel;

    // used to determine whether to leave this page or stay on it (after
    // operation finishing)
    private ObjectDelta<O> delta;

    private AbstractObjectMainPanel<O> mainPanel;
    private boolean saveOnConfigure;        // ugly hack - whether to invoke 'Save' when returning to this page

    private boolean editingFocus = false;             //before we got isOidParameterExists status depending only on oid parameter existence
                                                    //we should set editingFocus=true not only when oid parameter exists but also
                                                    //when object is given as a constructor parameter

    public boolean isEditingFocus() {
        return editingFocus;
    }

    @Override
    protected void createBreadcrumb() {
        createInstanceBreadcrumb();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (saveOnConfigure) {
            saveOnConfigure = false;
            add(new AbstractAjaxTimerBehavior(Duration.milliseconds(100)) {
                @Override
                protected void onTimer(AjaxRequestTarget target) {
                    stop(target);
                    savePerformed(target);
                }
            });
        }
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        String simpleName = getCompileTimeClass().getSimpleName();
        String lokalizedSimpleName = new StringResourceModel("ObjectType." + simpleName).setDefaultValue(simpleName).getString();
        if (isAdd()) {
            return createStringResource("PageAdminObjectDetails.title.new", lokalizedSimpleName);
        }

        String name = null;
        if (getObjectWrapper() != null && getObjectWrapper().getObject() != null) {
            name = WebComponentUtil.getName(getObjectWrapper().getObject());
        }

        return createStringResource("PageAdminObjectDetails.title.edit.readonly.${readOnly}", getObjectModel(), lokalizedSimpleName, name);
    }

    private boolean isAdd() {
        return !isOidParameterExists() && !editingFocus;
    }

    public LoadableModel<PrismObjectWrapper<O>> getObjectModel() {
        return objectModel;
    }

    protected AbstractObjectMainPanel<O> getMainPanel() {
        return mainPanel;
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
        return objectModel.getObject();
    }

    public List<FocusSubwrapperDto<OrgType>> getParentOrgs() {
        return parentOrgModel.getObject();
    }

    public ObjectDelta<O> getDelta() {
        return delta;
    }

    public void setDelta(ObjectDelta<O> delta) {
        this.delta = delta;
    }

    public ProgressPanel getProgressPanel() {
        return progressPanel;
    }

    protected void reviveModels() throws SchemaException {
        WebComponentUtil.revive(objectModel, getPrismContext());
        WebComponentUtil.revive(parentOrgModel, getPrismContext());
    }

    public abstract Class<O> getCompileTimeClass();


    public void initialize(final PrismObject<O> objectToEdit) {
        boolean isNewObject = objectToEdit == null && StringUtils.isEmpty(getObjectOidParameter());

        initialize(objectToEdit, isNewObject, false);
    }

    public void initialize(final PrismObject<O> objectToEdit, boolean isNewObject) {
        initialize(objectToEdit, isNewObject, false);
    }

    public void initialize(final PrismObject<O> objectToEdit, boolean isNewObject, boolean isReadonly) {
        initializeModel(objectToEdit, isNewObject, isReadonly);
        initLayout();
    }

    protected void initializeModel(final PrismObject<O> objectToEdit, boolean isNewObject, boolean isReadonly) {
        editingFocus = !isNewObject;

        objectModel = new LoadableModel<PrismObjectWrapper<O>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PrismObjectWrapper<O> load() {
                PrismObjectWrapper<O> wrapper = loadObjectWrapper(objectToEdit, isReadonly);
//                wrapper.sort();
                return wrapper;
            }
        };

        parentOrgModel = new LoadableModel<List<FocusSubwrapperDto<OrgType>>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<FocusSubwrapperDto<OrgType>> load() {
                return loadOrgWrappers();
            }
        };
    }

    protected List<FocusSubwrapperDto<OrgType>> loadOrgWrappers() {
        // WRONG!! TODO: fix
        return null;
    }

    protected abstract O createNewObject();

    protected void initLayout() {
        initLayoutSummaryPanel();

        mainPanel = createMainPanel(ID_MAIN_PANEL);
        mainPanel.setOutputMarkupId(true);
        add(mainPanel);

        progressPanel = new ProgressPanel(ID_PROGRESS_PANEL);
        add(progressPanel);
    }

    protected abstract ObjectSummaryPanel<O> createSummaryPanel();

    protected void initLayoutSummaryPanel() {

        ObjectSummaryPanel<O> summaryPanel = createSummaryPanel();
        summaryPanel.setOutputMarkupId(true);

        setSummaryPanelVisibility(summaryPanel);
        add(summaryPanel);
    }

    protected void setSummaryPanelVisibility(ObjectSummaryPanel<O> summaryPanel){
        summaryPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isOidParameterExists() || editingFocus;
            }
        });
    }

    protected abstract AbstractObjectMainPanel<O> createMainPanel(String id);

    protected String getObjectOidParameter() {
        PageParameters parameters = getPageParameters();
        LOGGER.trace("Page parameters: {}", parameters);
        StringValue oidValue = parameters.get(OnePageParameterEncoder.PARAMETER);
        LOGGER.trace("OID parameter: {}", oidValue);
        if (oidValue == null) {
            return null;
        }
        String oid = oidValue.toString();
        if (StringUtils.isBlank(oid)) {
            return null;
        }
        return oid;
    }

    public boolean isOidParameterExists() {
        return getObjectOidParameter() != null;
    }

    protected PrismObjectWrapper<O> loadObjectWrapper(PrismObject<O> objectToEdit, boolean isReadonly) {
        Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
        OperationResult result = task.getResult();
        PrismObject<O> object = null;
        Collection<SelectorOptions<GetOperationOptions>> loadOptions = null;
        try {
            if (!isOidParameterExists()) {
                if (objectToEdit == null) {
                    LOGGER.trace("Loading object: New object (creating)");
                    O focusType = createNewObject();

                    // Apply subtype using page parameters
                    List<StringValue> subtypes = getPageParameters().getValues(ObjectType.F_SUBTYPE.getLocalPart());
                    subtypes.stream().filter(p -> !p.isEmpty()).forEach(c -> focusType.subtype(c.toString()));

                    getMidpointApplication().getPrismContext().adopt(focusType);
                    object = (PrismObject<O>) focusType.asPrismObject();
                } else {
                    LOGGER.trace("Loading object: New object (supplied): {}", objectToEdit);
                    object = objectToEdit;
                }
            } else {
                loadOptions = getOperationOptionsBuilder()
                        .item(UserType.F_JPEG_PHOTO).retrieve()
                        .build();
                String focusOid = getObjectOidParameter();
                object = WebModelServiceUtils.loadObject(getCompileTimeClass(), focusOid, loadOptions, this, task, result);
                LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, object);
            }

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
        }

        showResult(result, false);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded object:\n{}", object.debugDump());
        }

        if (object == null) {
            if (isOidParameterExists()) {
                getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
            } else {
                getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
            }
            throw new RestartResponseException(getRestartResponsePage());
        }

        ItemStatus itemStatus = isOidParameterExists() || editingFocus ? ItemStatus.NOT_CHANGED : ItemStatus.ADDED;
        PrismObjectWrapper<O> wrapper;

        PrismObjectWrapperFactory<O> factory = getRegistry().getObjectWrapperFactory(object.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(ItemStatus.ADDED == itemStatus);
        //we don't want to set to false.. refactor this method to take either enum (READONLY, AUTO, ...) or
        // Boolean instead of boolean isReadonly
        if (isReadonly) {
            context.setReadOnly(isReadonly);
        }


        try {
            wrapper = factory.createObjectWrapper(object, itemStatus, context);
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
            try {
                wrapper = factory.createObjectWrapper(object, itemStatus, context);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }

        showResult(result, false);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded focus wrapper:\n{}", wrapper.debugDump());
        }

        return wrapper;
    }

//    private void loadParentOrgs(PrismObjectWrapper<O> wrapper, Task task, OperationResult result) {
//        OperationResult subResult = result.createMinorSubresult(OPERATION_LOAD_PARENT_ORGS);
//        PrismObject<O> focus = wrapper.getObject();
//        // Load parent organizations (full objects). There are used in the
//        // summary panel and also in the main form.
//        // Do it here explicitly instead of using resolve option to have ability
//        // to better handle (ignore) errors.
//        for (ObjectReferenceType parentOrgRef : focus.asObjectable().getParentOrgRef()) {
//
//            PrismObject<OrgType> parentOrg = null;
//            try {
//
//                parentOrg = getModelService().getObject(OrgType.class, parentOrgRef.getOid(), null, task,
//                        subResult);
//                LOGGER.trace("Loaded parent org with result {}",
//                        new Object[] { subResult.getLastSubresult() });
//            } catch (AuthorizationException e) {
//                // This can happen if the user has permission to read parentOrgRef but it does not have
//                // the permission to read target org
//                // It is OK to just ignore it.
//                subResult.muteLastSubresultError();
//                LOGGER.debug("User {} does not have permission to read parent org unit {} (ignoring error)", task.getOwner().getName(), parentOrgRef.getOid());
//            } catch (Exception ex) {
//                subResult.recordWarning(createStringResource("PageAdminObjectDetails.message.loadParentOrgs.warning", parentOrgRef.getOid()).getString(), ex);
//                LOGGER.warn("Cannot load parent org {}: {}", parentOrgRef.getOid(), ex.getMessage(), ex);
//            }
//
//            if (parentOrg != null) {
//                wrapper.getParentOrgs().add(parentOrg);
//            }
//        }
//        subResult.computeStatus();
//    }

    protected abstract Class<? extends Page> getRestartResponsePage();

    public Object findParam(String param, String oid, OperationResult result) {

        Object object = null;

        for (OperationResult subResult : result.getSubresults()) {
            if (subResult != null && subResult.getParams() != null) {
                if (subResult.getParams().get(param) != null
                        && subResult.getParams().get(OperationResult.PARAM_OID) != null
                        && subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
                    return subResult.getParams().get(param);
                }
                object = findParam(param, oid, subResult);

            }
        }
        return object;
    }

    // TODO put this into correct place
    protected boolean previewRequested;

    /**
     * This will be called from the main form when save button is pressed.
     */
    public void savePerformed(AjaxRequestTarget target) {
        progressPanel.onBeforeSave();
        OperationResult result = new OperationResult(OPERATION_SAVE);
        previewRequested = false;
        saveOrPreviewPerformed(target, result, false);
    }

    public void previewPerformed(AjaxRequestTarget target) {
        progressPanel.onBeforeSave();
        OperationResult result = new OperationResult(OPERATION_PREVIEW_CHANGES);
        previewRequested = true;
        saveOrPreviewPerformed(target, result, true);
    }

    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
        saveOrPreviewPerformed(target, result, previewOnly, null);
    }

    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly, Task task) {
        boolean delegationChangesExist = processDeputyAssignments(previewOnly);

        PrismObjectWrapper<O> objectWrapper = getObjectWrapper();
        LOGGER.debug("Saving object {}", objectWrapper);

        // todo: improve, delta variable is quickfix for MID-1006
        // redirecting to user list page everytime user is created in repository
        // during user add in gui,
        // and we're not taking care about account/assignment create errors
        // (error message is still displayed)
        delta = null;

        if(task == null) {
        task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        }

        ModelExecuteOptions options = getOptions(previewOnly);

        LOGGER.debug("Using execute options {}.", options);

        try {
            reviveModels();

            delta = objectWrapper.getObjectDelta();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("pageAdminObjectDetails.message.cantCreateObject"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Create Object failed", ex);
            showResult(result);
            target.add(getFeedbackPanel());
            return;
        }

        switch (objectWrapper.getStatus()) {
            case ADDED:
                try {
                    PrismObject<O> objectToAdd = delta.getObjectToAdd();
                    WebComponentUtil.encryptCredentials(objectToAdd, true, getMidpointApplication());
                    prepareObjectForAdd(objectToAdd);
                    getPrismContext().adopt(objectToAdd, getCompileTimeClass());
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
                    }

                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());

                        final Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
                        final Collection<SimpleValidationError> validationErrors = performCustomValidation(objectToAdd, deltas);
                        if (checkValidationErrors(target, validationErrors)) {
                            return;
                        }
                        progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                    } else {
                        result.recordSuccess();
                    }
                } catch (Exception ex) {
                    result.recordFatalError(getString("pageFocus.message.cantCreateFocus"), ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Create user failed", ex);
                    showResult(result);
                }
                break;

            case NOT_CHANGED:
                try {
                    WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
                    prepareObjectDeltaForModify(delta); //preparing of deltas for projections (ADD, DELETE, UNLINK)

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
                    }

                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());
                        deltas.add(delta);
                    }

                    List<ObjectDelta<? extends ObjectType>> additionalDeltas = getAdditionalModifyDeltas(result);
                    if (additionalDeltas != null) {
                        for (ObjectDelta additionalDelta : additionalDeltas) {
                            if (!additionalDelta.isEmpty()) {
                                additionalDelta.revive(getPrismContext());
                                deltas.add(additionalDelta);
                            }
                        }
                    }

                    if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
                        ObjectDelta emptyDelta = getPrismContext().deltaFactory().object().createEmptyModifyDelta(getCompileTimeClass(),
                                objectWrapper.getObject().getOid());
                        deltas.add(emptyDelta);

                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
                        if (checkValidationErrors(target, validationErrors)) {
                            return;
                        }
                        progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                    } else if (!deltas.isEmpty()) {
                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
                        if (checkValidationErrors(target, validationErrors)) {
                            return;
                        }
                        progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                    } else if (previewOnly && delta.isEmpty() && delegationChangesExist){
                        progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                    } else {
                        progressPanel.clearProgressPanel();            // from previous attempts (useful only if we would call finishProcessing at the end, but that's not the case now)
                        if (!previewOnly) {
                            if (!delegationChangesExist) {
                                result.recordWarning(getString("PageAdminObjectDetails.noChangesSave"));
                                showResult(result);
                            }
                            redirectBack();
                        } else {
                            if (!delegationChangesExist) {
                                warn(getString("PageAdminObjectDetails.noChangesPreview"));
                                target.add(getFeedbackPanel());
                            }
                        }
                    }

                } catch (Exception ex) {
                    if (!executeForceDelete(objectWrapper, task, options, result)) {
                        result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
                        LoggingUtils.logUnexpectedException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
                    } else {
                        result.recomputeStatus();
                    }
                    showResult(result);
                }
                break;
            // support for add/delete containers (e.g. delete credentials)
            default:
                error(getString("pageAdminFocus.message.unsupportedState", objectWrapper.getStatus()));
        }

//        result.recomputeStatus();
//
//        if (!result.isInProgress()) {
//            LOGGER.trace("Result NOT in progress, calling finishProcessing");
//            finishProcessing(target, result, false);
//        }

        LOGGER.trace("returning from saveOrPreviewPerformed");
    }

    protected boolean processDeputyAssignments(boolean previewOnly){
        return false;
    }

    protected boolean checkValidationErrors(AjaxRequestTarget target, Collection<SimpleValidationError> validationErrors) {
        if (validationErrors != null && !validationErrors.isEmpty()) {
            for (SimpleValidationError error : validationErrors) {
                LOGGER.error("Validation error, attribute: '" + error.printAttribute()
                        + "', message: '" + error.getMessage() + "'.");
                error("Validation error, attribute: '" + error.printAttribute()
                        + "', message: '" + error.getMessage() + "'.");
            }

            target.add(getFeedbackPanel());
            return true;
        }
        return false;
    }

    @Override
    public void startProcessing(AjaxRequestTarget target, OperationResult result) {
        LOGGER.trace("startProcessing called, making main panel invisible");
        mainPanel.setVisible(false);
        target.add(mainPanel);
    }

    @NotNull
    protected ModelExecuteOptions getOptions(boolean previewOnly) {
        ModelExecuteOptions options = mainPanel.getExecuteChangeOptionsDto().createOptions();
        if (previewOnly) {
            options.getOrCreatePartialProcessing().setApprovals(PartialProcessingTypeType.PROCESS);
        }
        return options;
    }

    protected void prepareObjectForAdd(PrismObject<O> object) throws SchemaException {

    }

    protected void prepareObjectDeltaForModify(ObjectDelta<O> objectDelta) throws SchemaException {

    }

    protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
        return null;
    }

    protected boolean executeForceDelete(PrismObjectWrapper<O> userWrapper, Task task, ModelExecuteOptions options,
            OperationResult parentResult) {
        return isForce();
    }

    protected boolean isForce() {
        return getMainPanel().getExecuteChangeOptionsDto().isForce();
    }

    protected boolean isKeepDisplayingResults() {
        return getMainPanel().getExecuteChangeOptionsDto().isKeepDisplayingResults();
    }


    protected Collection<SimpleValidationError> performCustomValidation(PrismObject<O> object,
            Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
        Collection<SimpleValidationError> errors = null;

        if (object == null) {
            if (getObjectWrapper() != null && getObjectWrapper().getObjectOld() != null) {
                object = getObjectWrapper().getObjectOld().clone();        // otherwise original object could get corrupted e.g. by applying the delta below

                for (ObjectDelta delta : deltas) {
                    // because among deltas there can be also ShadowType deltas
                    if (UserType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                        delta.applyTo(object);
                    }
                }
            }
        } else {
            object = object.clone();
        }

        performAdditionalValidation(object, deltas, errors);

        for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
            if (errors == null) {
                errors = validator.validateObject(object, deltas);
            } else {
                errors.addAll(validator.validateObject(object, deltas));
            }
        }

        return errors;
    }

    protected void performAdditionalValidation(PrismObject<O> object,
            Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) throws SchemaException {

    }

    public List<ObjectFormType> getObjectFormTypes() {
        Task task = createSimpleTask(OPERATION_LOAD_GUI_CONFIGURATION);
        OperationResult result = task.getResult();
        CompiledUserProfile adminGuiConfiguration;
        try {
            adminGuiConfiguration = getModelInteractionService().getCompiledUserProfile(task, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            throw new SystemException("Cannot load GUI configuration: "+e.getMessage(), e);
        }
        ObjectFormsType objectFormsType = adminGuiConfiguration.getObjectForms();
        if (objectFormsType == null) {
            return null;
        }
        List<ObjectFormType> objectForms = objectFormsType.getObjectForm();
        if (objectForms == null || objectForms.isEmpty()) {
            return objectForms;
        }
        List<ObjectFormType> validObjectForms = new ArrayList<>();
        for (ObjectFormType objectForm: objectForms) {
            if (isSupportedObjectType(objectForm.getType())) {
                validObjectForms.add(objectForm);
            }
        }
        return validObjectForms;
    }

    protected boolean isSupportedObjectType(QName type) {
        ObjectTypes objectType = ObjectTypes.getObjectType(getCompileTimeClass());
        return QNameUtil.match(objectType.getTypeQName(),type);
    }

    public void setSaveOnConfigure(boolean saveOnConfigure) {
        this.saveOnConfigure = saveOnConfigure;
    }

    public boolean isSaveOnConfigure() {
        return saveOnConfigure;
    }

    public boolean isForcedPreview() {
        GuiObjectDetailsPageType objectDetails = getCompiledUserProfile().findObjectDetailsConfiguration(getCompileTimeClass());
        return objectDetails != null && DetailsPageSaveMethodType.FORCED_PREVIEW.equals(objectDetails.getSaveMethod());
    }

}
