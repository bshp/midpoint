/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SecurityUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityUtils.class);

    public static MidPointUserProfilePrincipal getPrincipalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return getPrincipalUser(authentication);
    }

    public static MidPointUserProfilePrincipal getPrincipalUser(Authentication authentication) {
        if (authentication == null) {
            LOGGER.trace("Authentication not available in security context.");
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof MidPointUserProfilePrincipal) {
            return (MidPointUserProfilePrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            // silently ignore to avoid filling the logs
            return null;
        }
        LOGGER.debug("Principal user in security context holder is {} ({}) but not type of {}",
                principal, principal.getClass(), MidPointUserProfilePrincipal.class.getName());
        return null;
    }

    public static boolean isMenuAuthorized(MainMenuItem item) {
        Class clazz = item.getPageClass();
        return clazz == null || isPageAuthorized(clazz);
    }

    public static boolean isMenuAuthorized(MenuItem item) {
        Class clazz = item.getPageClass();
        return isPageAuthorized(clazz);
    }

    public static boolean isPageAuthorized(Class page) {
        if (page == null) {
            return false;
        }

        PageDescriptor descriptor = (PageDescriptor) page.getAnnotation(PageDescriptor.class);
        if (descriptor == null ){
            return false;
        }


        AuthorizationAction[] actions = descriptor.action();
        List<String> list = new ArrayList<>();
        if (actions != null) {
            for (AuthorizationAction action : actions) {
                list.add(action.actionUri());
            }
        }

        return WebComponentUtil.isAuthorized(list.toArray(new String[list.size()]));
    }

    public static WebMarkupContainer createHiddenInputForCsrf(String id) {
        WebMarkupContainer field = new WebMarkupContainer(id) {

            @Override
            public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                super.onComponentTagBody(markupStream, openTag);

                appendHiddenInputForCsrf(getResponse());
            }
        };
        field.setRenderBodyOnly(true);

        return field;
    }

    public static void appendHiddenInputForCsrf(Response resp) {
        CsrfToken csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return;
        }

        String parameterName = csrfToken.getParameterName();
        String value = csrfToken.getToken();

        resp.write("<input type=\"hidden\" name=\"" + parameterName + "\" value=\"" + value + "\"/>");
    }

    public static CsrfToken getCsrfToken() {
        Request req = RequestCycle.get().getRequest();
        HttpServletRequest httpReq = (HttpServletRequest) req.getContainerRequest();

        return (CsrfToken) httpReq.getAttribute("_csrf");
    }
}
