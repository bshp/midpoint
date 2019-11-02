/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.impl;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipalManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@Component
public class MidPointPrincipalManagerMock implements MidPointPrincipalManager, UserDetailsService {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointPrincipalManagerMock.class);

    @Autowired(required = true)
    private transient RepositoryService repositoryService;

    @Autowired(required = true)
    private ActivationComputer activationComputer;

    @Autowired(required = true)
    private Clock clock;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Override
    public MidPointPrincipal getPrincipal(String username) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        PrismObject<UserType> user = null;
        try {
            user = findByUsername(username, result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
            throw new SystemException(ex.getMessage(), ex);
        }

        return getPrincipal(user, null, result);
    }

    @Override
    public MidPointPrincipal getPrincipalByOid(String oid) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        UserType user = getUserByOid(oid, result);
        return getPrincipal(user.asPrismObject());
    }

    @Override
    public MidPointPrincipal getPrincipal(PrismObject<UserType> user) throws SchemaException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        return getPrincipal(user, null, result);
    }



    @Override
    public MidPointPrincipal getPrincipal(PrismObject<UserType> user,
            AuthorizationTransformer authorizationLimiter, OperationResult result) throws SchemaException {
        if (user == null) {
            return null;
        }

        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);

        MidPointPrincipal principal = new MidPointPrincipal(user.asObjectable());
        initializePrincipalFromAssignments(principal, systemConfiguration);
        return principal;
    }

    private PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) {
        PrismObject<SystemConfigurationType> systemConfiguration = null;
        try {
            // TODO: use SystemObjectCache instead?
            systemConfiguration = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                    null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.warn("No system configuration: {}", e.getMessage(), e);
        }
        return systemConfiguration;
    }

    @Override
    public void updateUser(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas) {
        OperationResult result = new OperationResult(OPERATION_UPDATE_USER);
        try {
            save(principal, result);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{principal.getFullName(), principal.getOid(), ex.getMessage()});
        }
    }

    private PrismObject<UserType> findByUsername(String username, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        ObjectQuery query = ObjectQueryUtil.createNormNameQuery(usernamePoly, prismContext);
        LOGGER.trace("Looking for user, query:\n" + query.debugDump());

        List<PrismObject<UserType>> list = repositoryService.searchObjects(UserType.class, query, null,
                result);
        LOGGER.trace("Users found: {}.", (list != null ? list.size() : 0));
        if (list == null || list.size() != 1) {
            return null;
        }

        return list.get(0);
    }

    private void initializePrincipalFromAssignments(MidPointPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration) {

        OperationResult result = new OperationResult(MidPointPrincipalManagerMock.class.getName() + ".addAuthorizations");

        principal.setApplicableSecurityPolicy(locateSecurityPolicy(principal, systemConfiguration, result));

//        if (systemConfiguration != null) {
//            principal.setAdminGuiConfiguration(systemConfiguration.asObjectable().getAdminGuiConfiguration());
//        }

        AuthorizationType authorizationType = new AuthorizationType();
        authorizationType.getAction().add("FAKE");
        principal.getAuthorities().add(new Authorization(authorizationType));

        ActivationType activation = principal.getUser().getActivation();
        if (activation != null) {
            activationComputer.computeEffective(principal.getUser().getLifecycleState(), activation, null);
        }
    }

    private SecurityPolicyType locateSecurityPolicy(MidPointPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration, OperationResult result) {
        if (systemConfiguration == null) {
            return null;
        }
        ObjectReferenceType globalSecurityPolicyRef = systemConfiguration.asObjectable().getGlobalSecurityPolicyRef();
        if (globalSecurityPolicyRef == null) {
            return null;
        }
        try {
            PrismObject<SecurityPolicyType> policy = repositoryService.getObject(SecurityPolicyType.class, globalSecurityPolicyRef.getOid(), null, result);
            return policy.asObjectable();
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private MidPointPrincipal save(MidPointPrincipal person, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        UserType oldUserType = getUserByOid(person.getOid(), result);
        PrismObject<UserType> oldUser = oldUserType.asPrismObject();

        PrismObject<UserType> newUser = person.getUser().asPrismObject();

        ObjectDelta<UserType> delta = oldUser.diff(newUser);
        repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(),
                new OperationResult(OPERATION_UPDATE_USER));

        return person;
    }

    private UserType getUserByOid(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(UserType.class, oid,
                null, result).asObjectable();
        if (object != null && (object instanceof UserType)) {
            return (UserType) object;
        }

        return null;
    }

    @Override
    public <F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object) {
        if (object == null || object.getOid() == null) {
            return null;
        }
        PrismObject<F> owner = null;
        if (object.canRepresent(ShadowType.class)) {
            owner = repositoryService.searchShadowOwner(object.getOid(), null, new OperationResult(MidPointPrincipalManagerMock.class+".resolveOwner"));
        }
        if (owner == null) {
            return null;
        }
        return owner;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//         TODO Auto-generated method stub
        try {
            return getPrincipal(username);
        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        } catch (SchemaException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }


}
