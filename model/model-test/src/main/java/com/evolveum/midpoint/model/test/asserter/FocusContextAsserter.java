/**
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class FocusContextAsserter<F extends ObjectType,RA> extends ElementContextAsserter<ModelElementContext<F>,F,RA> {


    public FocusContextAsserter(ModelElementContext<F> focusContext) {
        super(focusContext);
    }

    public FocusContextAsserter(ModelElementContext<F> focusContext, String detail) {
        super(focusContext, detail);
    }

    public FocusContextAsserter(ModelElementContext<F> focusContext, RA returnAsserter, String detail) {
        super(focusContext, returnAsserter, detail);
    }

    public ModelElementContext<F> getFocusContext() {
        return getElementContext();
    }

    @Override
    public PrismObjectAsserter<F,? extends FocusContextAsserter<F,RA>> objectOld() {
        return (PrismObjectAsserter<F, ? extends FocusContextAsserter<F, RA>>) super.objectOld();
    }

    @Override
    public PrismObjectAsserter<F,? extends FocusContextAsserter<F,RA>> objectCurrent() {
        return (PrismObjectAsserter<F, ? extends FocusContextAsserter<F, RA>>) super.objectCurrent();
    }

    @Override
    public PrismObjectAsserter<F,? extends FocusContextAsserter<F,RA>> objectNew() {
        return (PrismObjectAsserter<F, ? extends FocusContextAsserter<F, RA>>) super.objectNew();
    }

    @Override
    public ObjectDeltaAsserter<F, ? extends FocusContextAsserter<F,RA>> primaryDelta() {
        return (ObjectDeltaAsserter<F, ? extends FocusContextAsserter<F,RA>>) super.primaryDelta();
    }

    @Override
    public FocusContextAsserter<F,RA> assertNoPrimaryDelta() {
        super.assertNoPrimaryDelta();
        return this;
    }

    @Override
    public ObjectDeltaAsserter<F, FocusContextAsserter<F,RA>> secondaryDelta() {
        return (ObjectDeltaAsserter<F, FocusContextAsserter<F,RA>>) super.secondaryDelta();
    }

    @Override
    public FocusContextAsserter<F,RA> assertNoSecondaryDelta() {
        super.assertNoSecondaryDelta();
        return this;
    }

    @Override
    protected String desc() {
        // TODO: better desc
        return descWithDetails(getFocusContext());
    }

    @Override
    public FocusContextAsserter<F,RA> display() {
        display(desc());
        return this;
    }

    @Override
    public FocusContextAsserter<F,RA> display(String message) {
        IntegrationTestTools.display(message, getFocusContext());
        return this;
    }
}
