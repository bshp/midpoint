/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.functions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;


public class CustomFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(CustomFunctions.class);

    private ExpressionFactory expressionFactory;
    private FunctionLibraryType library;
    private ExpressionProfile expressionProfile;
    private PrismContext prismContext;
    /**
     * Operation result existing at the initialization time. It is used only if we cannot obtain current operation
     * result in any other way.
      */
    private OperationResult initializationTimeResult;
    /**
     * Operation result existing at the initialization time. It is used only if we cannot obtain current operation
     * result in any other way.
     */
    private Task initializationTimeTask;

    public CustomFunctions(FunctionLibraryType library, ExpressionFactory expressionFactory, ExpressionProfile expressionProfile,
            OperationResult result, Task task) {
        this.library = library;
        this.expressionFactory = expressionFactory;
        this.expressionProfile = expressionProfile;
        this.prismContext = expressionFactory.getPrismContext();
        this.initializationTimeResult = result;
        this.initializationTimeTask = task;
    }

    /**
     * This method is invoked by the scripts. It is supposed to be only public method exposed
     * by this class.
     */
    public <V extends PrismValue, D extends ItemDefinition> Object execute(String functionName, Map<String, Object> params) throws ExpressionEvaluationException {
        Validate.notNull(functionName, "Function name must be specified");

        ScriptExpressionEvaluationContext ctx = ScriptExpressionEvaluationContext.getThreadLocal();
        Task task;
        OperationResult result;
        if (ctx != null) {
            if (ctx.getTask() != null) {
                task = ctx.getTask();
            } else {
                LOGGER.warn("No task in ScriptExpressionEvaluationContext for the current thread found. Using "
                        + "initialization-time task: {}", initializationTimeTask);
                task = initializationTimeTask;
            }
            if (ctx.getResult() != null) {
                result = ctx.getResult();
            } else {
                LOGGER.warn("No operation result in ScriptExpressionEvaluationContext for the current thread found. Using "
                        + "initialization-time op. result");
                result = initializationTimeResult;
            }
        } else {
            LOGGER.warn("No ScriptExpressionEvaluationContext for current thread found. Using initialization-time task "
                    + "and operation result: {}", initializationTimeTask);
            task = initializationTimeTask;
            result = initializationTimeResult;
        }

        List<ExpressionType> functions = library.getFunction().stream().filter(expression -> functionName.equals(expression.getName())).collect(Collectors.toList());

        LOGGER.trace("functions {}", functions);
        ExpressionType expressionType = functions.iterator().next();

        LOGGER.trace("function to execute {}", expressionType);

        try {
            ExpressionVariables variables = new ExpressionVariables();
            if (MapUtils.isNotEmpty(params)) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    variables.put(entry.getKey(), convertInput(entry, expressionType));
                }
            }

            QName returnType = expressionType.getReturnType();
            if (returnType == null) {
                returnType = DOMUtil.XSD_STRING;
            }

            //noinspection unchecked
            D outputDefinition = (D) prismContext.getSchemaRegistry().findItemDefinitionByType(returnType);
            if (outputDefinition == null) {
                //noinspection unchecked
                outputDefinition = (D) prismContext.definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, returnType);
            }
            if (expressionType.getReturnMultiplicity() != null && expressionType.getReturnMultiplicity() == ExpressionReturnMultiplicityType.MULTI) {
                outputDefinition.toMutable().setMaxOccurs(-1);
            } else {
                outputDefinition.toMutable().setMaxOccurs(1);
            }
            String shortDesc = "custom function execute";
            Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition, expressionProfile,
                    shortDesc, task, result);

            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task);
            PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(context, result);

            LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

            if (outputTriple == null) {
                return null;
            }

            Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();

            if (nonNegativeValues.isEmpty()) {
                return null;
            }

            if (outputDefinition.isMultiValue()) {
                return PrismValueCollectionsUtil.getRealValuesOfCollection(nonNegativeValues);
            }


            if (nonNegativeValues.size() > 1) {
                throw new ExpressionEvaluationException("Expression returned more than one value ("
                        + nonNegativeValues.size() + ") in " + shortDesc);
            }

            return nonNegativeValues.iterator().next().getRealValue();


        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            throw new ExpressionEvaluationException(e.getMessage(), e);
        }

    }

    private TypedValue convertInput(Map.Entry<String, Object> entry, ExpressionType expression) throws SchemaException {

        ExpressionParameterType expressionParam = expression.getParameter().stream().filter(param -> param.getName().equals(entry.getKey())).findAny().orElseThrow(SchemaException :: new);

        QName paramType = expressionParam.getType();
        Class<?> expressionParameterClass = prismContext.getSchemaRegistry().determineClassForType(paramType);

        Object value = entry.getValue();
        if (expressionParameterClass != null && !DOMUtil.XSD_ANYTYPE.equals(paramType) && XmlTypeConverter.canConvert(expressionParameterClass)) {
            value = ExpressionUtil.convertValue(expressionParameterClass, null, entry.getValue(), prismContext.getDefaultProtector(), prismContext);
        }

        Class<?> valueClass;
        if (value == null) {
            valueClass = expressionParameterClass;
        } else {
            valueClass = value.getClass();
        }

        //ItemDefinition def = ExpressionUtil.determineDefinitionFromValueClass(prismContext, entry.getKey(), valueClass, paramType);

        // It is sometimes not possible to derive an item definition from the value class alone: more items can share the same
        // class (e.g. both objectStatePolicyConstraintType and assignmentStatePolicyConstraintType are of
        // StatePolicyConstraintType class). So let's provide valueClass here only.
        return new TypedValue<>(value, valueClass);
    }

}
