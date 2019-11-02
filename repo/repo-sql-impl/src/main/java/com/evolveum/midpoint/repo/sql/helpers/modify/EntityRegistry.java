/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbPath;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.Metamodel;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Viliam Repan (lazyman)
 */
@Service
public class EntityRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(EntityRegistry.class);

    @Autowired private SessionFactory sessionFactory;
    @Autowired private PrismContext prismContext;

    private Metamodel metamodel;

    private Map<Class, ManagedType> jaxbMappings = new HashMap<>();

    private Map<ManagedType, Map<String, Attribute>> attributeNameOverrides = new HashMap<>();

    private Map<ManagedType, Map<UniformItemPath, Attribute>> attributeNamePathOverrides = new HashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.debug("Starting initialization");

        metamodel = sessionFactory.getMetamodel();

        for (EntityType entity : metamodel.getEntities()) {
            Class javaType = entity.getJavaType();
            Ignore ignore = (Ignore) javaType.getAnnotation(Ignore.class);
            if (ignore != null) {
                continue;
            }

            Class jaxb;
            if (RObject.class.isAssignableFrom(javaType)) {
                jaxb = RObjectType.getType(javaType).getJaxbClass();
            } else {
                JaxbType jaxbType = (JaxbType) javaType.getAnnotation(JaxbType.class);
                if (jaxbType == null) {
                    throw new IllegalStateException("Unknown jaxb type for " + javaType.getName());
                }
                jaxb = jaxbType.type();
            }

            jaxbMappings.put(jaxb, entity);

            // create override map
            Map<String, Attribute> overrides = new HashMap<>();
            Map<UniformItemPath, Attribute> pathOverrides = new HashMap<>();

            for (Attribute attribute : (Set<Attribute>) entity.getAttributes()) {
                Class jType = attribute.getJavaType();
                JaxbPath[] paths = (JaxbPath[]) jType.getAnnotationsByType(JaxbPath.class);
                if (paths == null || paths.length == 0) {
                    paths = ((Method) attribute.getJavaMember()).getAnnotationsByType(JaxbPath.class);
                }

                if (paths == null || paths.length == 0) {
                    JaxbName name = ((Method) attribute.getJavaMember()).getAnnotation(JaxbName.class);
                    if (name != null) {
                        overrides.put(name.localPart(), attribute);
                    }
                    continue;
                }

                for (JaxbPath path : paths) {
                    JaxbName[] names = path.itemPath();
                    if (names.length == 1) {
                        overrides.put(names[0].localPart(), attribute);
                    } else {
                        UniformItemPath customPath = prismContext.emptyPath();
                        for (JaxbName name : path.itemPath()) {
                            customPath = customPath.append(new QName(name.namespace(), name.localPart()));
                        }
                        pathOverrides.put(customPath, attribute);
                    }
                }
            }

            if (!overrides.isEmpty()) {
                attributeNameOverrides.put(entity, overrides);
            }
            if (!pathOverrides.isEmpty()) {
                attributeNamePathOverrides.put(entity, pathOverrides);
            }
        }

        LOGGER.debug("Initialization finished");
    }

    public ManagedType getJaxbMapping(Class jaxbType) {
        return jaxbMappings.get(jaxbType);
    }

    public ManagedType getMapping(Class entityType) {
        return metamodel.managedType(entityType);
    }

    public Attribute findAttribute(ManagedType type, String name) {
        try {
            return type.getAttribute(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Attribute findAttributeOverride(ManagedType type, String nameOverride) {
        Map<String, Attribute> overrides = attributeNameOverrides.get(type);
        if (overrides == null) {
            return null;
        }

        return overrides.get(nameOverride);
    }

    public boolean hasAttributePathOverride(ManagedType type, ItemPath pathOverride) {
        Map<UniformItemPath, Attribute> overrides = attributeNamePathOverrides.get(type);
        if (overrides == null) {
            return false;
        }

        ItemPath namedOnly = pathOverride.namedSegmentsOnly();

        for (UniformItemPath path : overrides.keySet()) {
            if (path.isSuperPathOrEquivalent(namedOnly)) {
                return true;
            }
        }

        return false;
    }

    public Attribute findAttributePathOverride(ManagedType type, ItemPath pathOverride) {
        Map<UniformItemPath, Attribute> overrides = attributeNamePathOverrides.get(type);
        if (overrides == null) {
            return null;
        }

        return overrides.get(prismContext.toUniformPath(pathOverride));
    }
}
