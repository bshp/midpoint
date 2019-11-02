/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 */
public interface Event extends DebugDumpable, ShortDumpable {

    LightweightIdentifier getId();

    boolean isStatusType(EventStatusType eventStatusType);
    boolean isOperationType(EventOperationType eventOperationType);
    boolean isCategoryType(EventCategoryType eventCategoryType);

    boolean isAccountRelated();

    boolean isUserRelated();

    boolean isWorkItemRelated();

    boolean isWorkflowProcessRelated();

    boolean isWorkflowRelated();

    boolean isPolicyRuleRelated();

    boolean isAdd();

    boolean isModify();

    boolean isDelete();

    boolean isSuccess();

    boolean isAlsoSuccess();

    boolean isFailure();

    boolean isOnlyFailure();

    boolean isInProgress();

    // requester

    SimpleObjectRef getRequester();

    String getRequesterOid();

    void setRequester(SimpleObjectRef requester);

    // requestee

    SimpleObjectRef getRequestee();

    String getRequesteeOid();

    void setRequestee(SimpleObjectRef requestee);

    void createExpressionVariables(VariablesMap variables, OperationResult result);

    /**
     * Checks if the event is related to an item with a given path.
     * The meaning of the result depends on a kind of event (focal, resource object, workflow)
     * and on operation (add, modify, delete).
     *
     * Namely, this method is currently defined for ADD and MODIFY (not for DELETE) operations,
     * for focal and resource objects events (not for workflow ones).
     *
     * For MODIFY it checks whether an item with a given path is touched.
     * For ADD it checks whether there is a value for an item with a given path in the object created.
     *
     * For unsupported events the method returns false.
     *
     * Paths are compared without taking ID segments into account.
     *
     * EXPERIMENTAL; does not always work (mainly for values being deleted)
     *
     * @param itemPath
     * @return
     */
    boolean isRelatedToItem(ItemPath itemPath);

    String getChannel();

    /**
     * If needed, we can prescribe the handler that should process this event. It is recommended only for ad-hoc situations.
     * A better is to define handlers in system configuration.
     */
    EventHandlerType getAdHocHandler();

    /**
     * Returns plaintext focus password value, if known.
     * Beware: might not always work correctly:
     * 1. If the change execution was only partially successful, the value returned might or might not be stored in the repo
     * 2. If the password was changed to null, the 'null' value is returned. So the caller cannot distinguish it from "no change"
     *    situation. A new method for this would be needed.
     */
    default String getFocusPassword() {
        return null;
    }
}
