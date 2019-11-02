/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Principal that extends simple MidPointPrincipal with user interface concepts (user profile).
 *
 * @since 4.0
 * @author Radovan Semancik
 */
public class MidPointUserProfilePrincipal extends MidPointPrincipal {
    private static final long serialVersionUID = 1L;

    private CompiledUserProfile compiledUserProfile;

    private transient int activeSessions = 0;

    public MidPointUserProfilePrincipal(@NotNull UserType user) {
        super(user);
    }

    @NotNull
    public CompiledUserProfile getCompiledUserProfile() {
        if (compiledUserProfile == null) {
            compiledUserProfile = new CompiledUserProfile();
        }
        return compiledUserProfile;
    }

    public void setCompiledUserProfile(CompiledUserProfile compiledUserProfile) {
        this.compiledUserProfile = compiledUserProfile;
    }

    /**
     * Semi-shallow clone.
     */
    public MidPointUserProfilePrincipal clone() {
        MidPointUserProfilePrincipal clone = new MidPointUserProfilePrincipal(this.getUser());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(MidPointUserProfilePrincipal clone) {
        super.copyValues(clone);
        // No need to clone user profile here. It is essentially read-only.
        clone.compiledUserProfile = this.compiledUserProfile;
    }

    @Override
    protected void debugDumpInternal(StringBuilder sb, int indent) {
        super.debugDumpInternal(sb, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "compiledUserProfile", compiledUserProfile, indent + 1);
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MidPointUserProfilePrincipal)) {
            return false;
        }
        return getUser().equals(((MidPointUserProfilePrincipal) obj).getUser());
    }

    @Override
    public int hashCode() {
        return getUser().hashCode();
    }
}
