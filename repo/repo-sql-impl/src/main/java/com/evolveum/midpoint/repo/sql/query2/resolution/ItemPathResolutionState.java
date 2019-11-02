/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.resolution;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

/**
 * Describes current state in ItemPath resolution.
 *
 * We know what remains to be resolved.
 * We know the HQL item we are pointing to.
 * We know last transition - how we got here.
 *
 * This object is unmodifiable.
 *
 * @author mederly
 */
public class ItemPathResolutionState implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ItemPathResolutionState.class);

    final private ItemPath remainingItemPath;
    final private HqlDataInstance hqlDataInstance;
    final private JpaLinkDefinition lastTransition;                   // how we got here (optional)

    final private ItemPathResolver itemPathResolver;                // provides auxiliary functionality

    public ItemPathResolutionState(ItemPath pathToResolve, HqlDataInstance hqlDataInstance, ItemPathResolver itemPathResolver) {
        Validate.notNull(pathToResolve, "pathToResolve");
        Validate.notNull(hqlDataInstance, "hqlDataInstance");
        Validate.notNull(itemPathResolver, "itemPathResolver");
        this.remainingItemPath = pathToResolve;
        this.hqlDataInstance = hqlDataInstance;
        this.lastTransition = null;
        this.itemPathResolver = itemPathResolver;
    }

    public ItemPath getRemainingItemPath() {
        return remainingItemPath;
    }

    public HqlDataInstance getHqlDataInstance() {
        return hqlDataInstance;
    }

    public JpaLinkDefinition getLastTransition() {
        return lastTransition;
    }

    public ItemPathResolver getItemPathResolver() {
        return itemPathResolver;
    }

    public boolean isFinal() {
        return ItemPath.isEmpty(remainingItemPath);
    }

    /**
     * Executes transition to next state. Modifies query context by adding joins as necessary.
     *
     * Precondition: !isFinal()
     * Precondition: adequate transition exists
     *
     * @param itemDefinition Target item definition (used/required only for "any" properties)
     * @param singletonOnly Collections are forbidden
     * @return destination state - always not null
     */
    public ItemPathResolutionState nextState(ItemDefinition itemDefinition, boolean singletonOnly, PrismContext prismContext) throws QueryException {

        // special case - ".." when having previous state means returning to that state
        // used e.g. for Exists (some-path, some-conditions AND Equals(../xxx, yyy))
        //
        // This is brutal hack, to be thought again.
        if (remainingItemPath.startsWithParent() && hqlDataInstance.getParentItem() != null) {
            return new ItemPathResolutionState(
                    remainingItemPath.rest(),
                    hqlDataInstance.getParentItem(),
                    itemPathResolver);

        }
        DataSearchResult<?> result = hqlDataInstance.getJpaDefinition().nextLinkDefinition(remainingItemPath, itemDefinition, prismContext);
        LOGGER.trace("nextLinkDefinition on '{}' returned '{}'", remainingItemPath, result != null ? result.getLinkDefinition() : "(null)");
        if (result == null) {       // sorry we failed (however, this should be caught before -> so IllegalStateException)
            throw new IllegalStateException("Couldn't find '" + remainingItemPath + "' in " + hqlDataInstance.getJpaDefinition() +", looks like item can't be used in search.");
        }
        JpaLinkDefinition linkDefinition = result.getLinkDefinition();
        String newHqlPath = hqlDataInstance.getHqlPath();
        if (linkDefinition.hasJpaRepresentation()) {
            if (singletonOnly && linkDefinition.isMultivalued()) {
                throw new QueryException("Collections are not allowable for right-side paths nor for dereferencing");     // TODO better message + context
            }
            if (!linkDefinition.isEmbedded() || linkDefinition.isMultivalued()) {
                LOGGER.trace("Adding join for '{}' to context", linkDefinition);
                newHqlPath = itemPathResolver.addJoin(linkDefinition, hqlDataInstance.getHqlPath());
            } else {
                newHqlPath += "." + linkDefinition.getJpaName();
            }
        }
        HqlDataInstance<?> parentDataInstance;
        if (!remainingItemPath.startsWithParent()) {
            // TODO what about other special cases? (@, ...)
            parentDataInstance = hqlDataInstance;
        } else {
            parentDataInstance = null;
        }
        return new ItemPathResolutionState(
                result.getRemainder(),
                new HqlDataInstance<>(newHqlPath, result.getTargetDefinition(), parentDataInstance),
                itemPathResolver);
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    public String debugDumpNoParent() {
        return debugDump(0, false);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showParent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ItemPathResolutionState:\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Remaining path: ").append(remainingItemPath).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Last transition: ").append(lastTransition).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("HQL data item:\n").append(hqlDataInstance.debugDump(indent + 2, showParent));
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ItemPathResolutionState{" +
                "remainingItemPath=" + remainingItemPath +
                ", hqlDataInstance='" + hqlDataInstance + '\'' +
                '}';
    }

}
