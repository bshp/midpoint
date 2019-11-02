/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author mederly
 */
public class TaskTypeUtil {

    /**
     * Returns a stream of the task and all of its subtasks.
     */
    @NotNull
    public static Stream<TaskType> getAllTasksStream(TaskType root) {
        return Stream.concat(Stream.of(root),
                getResolvedSubtasks(root).stream().flatMap(TaskTypeUtil::getAllTasksStream));
    }

    public static List<TaskType> getResolvedSubtasks(TaskType parent) {
        List<TaskType> rv = new ArrayList<>();
        for (ObjectReferenceType childRef : parent.getSubtaskRef()) {
            //noinspection unchecked
            PrismObject<TaskType> child = childRef.getObject();
            if (child != null) {
                rv.add(child.asObjectable());
            } else {
                throw new IllegalStateException("Unresolved subtaskRef in " + parent + ": " + childRef);
            }
        }
        return rv;
    }

    public static void addSubtask(TaskType parent, TaskType child, PrismContext prismContext) {
        parent.getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(child, prismContext));
    }
}
