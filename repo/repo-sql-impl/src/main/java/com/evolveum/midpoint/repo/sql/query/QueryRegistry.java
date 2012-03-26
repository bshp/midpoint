/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.repo.sql.ClassMapper;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Index;

import javax.xml.namespace.QName;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author lazyman
 */
public class QueryRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(QueryRegistry.class);
    private static QueryRegistry registry;
    private Map<QName, EntityDefinition> definitions = new HashMap<QName, EntityDefinition>();

    public static QueryRegistry getInstance() throws QueryException {
        if (registry == null) {
            registry = new QueryRegistry();
            registry.init();
        }

        return registry;
    }

    public <T extends ObjectType> EntityDefinition findDefinition(Class<T> type) {
        ObjectTypes def = ObjectTypes.getObjectType(type);
        return findDefinition(def.getQName());
    }

    public EntityDefinition findDefinition(QName qname) {
        Validate.notNull(qname, "QName must not be null.");
        return definitions.get(qname);
    }

    /**
     * Method checks mapped classes through reflection and looks for {@link QueryAttribute} and
     * {@link QueryEntity} annotations. From them it builds query tree - paths that can be queried
     * via {@link QueryInterpreter}
     */
    private void init() throws QueryException {
        LOGGER.debug("Initializing query definition registry.");
//        EntityDefinition account = new EntityDefinition();
//        account.setName(SchemaConstants.C_ACCOUNT);
//        account.setType(AccountShadowType.COMPLEX_TYPE);
//        definitions.put(SchemaConstants.C_ACCOUNT, account);
//
//        AttributeDefinition accountType = new AttributeDefinition();
//        accountType.setName(AccountShadowType.F_ACCOUNT_TYPE);
//        accountType.setType(DOMUtil.XSD_STRING);
//        account.putDefinition(AccountShadowType.F_ACCOUNT_TYPE, accountType);
//
//        EntityDefinition extension = new EntityDefinition();
//        extension.setAny(true);
//        extension.setName(ObjectType.F_EXTENSION);
//        extension.setType(ExtensionType.COMPLEX_TYPE);
//        account.putDefinition(ObjectType.F_EXTENSION, extension);
//
//        EntityDefinition attributes = new EntityDefinition();
//        attributes.setAny(true);
//        attributes.setName(ResourceObjectShadowType.F_ATTRIBUTES);
//        attributes.setType(ResourceObjectShadowType.COMPLEX_TYPE);
//        account.putDefinition(ResourceObjectShadowType.F_ATTRIBUTES, attributes);
//
//        AttributeDefinition resourceRef = new AttributeDefinition();
//        resourceRef.setReference(true);
//        resourceRef.setName(ResourceObjectShadowType.F_RESOURCE_REF);
//        resourceRef.setType(ObjectReferenceType.COMPLEX_TYPE);
//        account.putDefinition(resourceRef.getName(), resourceRef);

        Collection<RContainerType> types = ClassMapper.getKnownTypes();
        for (RContainerType type : types) {
            Class clazz = type.getClazz();

            EntityDefinition entityDef = new EntityDefinition();
            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
            entityDef.setName(objectType.getQName());
            entityDef.setType(objectType.getTypeQName());
            definitions.put(entityDef.getName(), entityDef);

            List<Field> fields = getFields(clazz);

            LOGGER.trace("Processing {}, fields count: {}.", new Object[]{clazz.getName(), fields.size()});
            for (Field field : fields) {
                createDefinition(entityDef, field);
            }
        }

        StringBuilder builder = new StringBuilder();
        DebugUtil.debugDumpMapMultiLine(builder, definitions, 3);
        LOGGER.trace("Registry dump\n{}", new Object[]{builder.toString()});

        LOGGER.debug("Query definition registry initialization finished.");
    }

    private void createDefinition(EntityDefinition parent, Field field) {
        if (hasAnnotation(field, QueryEntity.class)) {
            LOGGER.trace("Entity {}", new Object[]{field.getName()});
            QueryEntity queryEntity = field.getAnnotation(QueryEntity.class);
            String name = StringUtils.isNotEmpty(queryEntity.name()) ? queryEntity.name() : field.getName();
            String namespace = StringUtils.isNotEmpty(queryEntity.namespace()) ?
                    queryEntity.namespace() : SchemaConstants.NS_COMMON;

            EntityDefinition entityDef = new EntityDefinition();
            entityDef.setName(new QName(namespace, name));
            entityDef.setAny(queryEntity.any());
            entityDef.setEmbedded(queryEntity.embedded());
            parent.putDefinition(entityDef.getName(), entityDef);
        } else if (hasAnnotation(field, QueryAttribute.class)) {
            LOGGER.trace("Attribute {}", new Object[]{field.getName()});
            QueryAttribute queryAttribute = field.getAnnotation(QueryAttribute.class);
            String name = StringUtils.isNotEmpty(queryAttribute.name()) ? queryAttribute.name() : field.getName();
            String namespace = StringUtils.isNotEmpty(queryAttribute.namespace()) ?
                    queryAttribute.namespace() : SchemaConstants.NS_COMMON;

            AttributeDefinition attrDef = new AttributeDefinition();
            attrDef.setName(new QName(namespace, name));
            attrDef.setIndexed(hasAnnotation(field, Index.class));
            if (RObjectReference.class.isAssignableFrom(field.getType())) {
                attrDef.setReference(true);
            }
            parent.putDefinition(attrDef.getName(), attrDef);
        }
    }

    private boolean hasAnnotation(Field field, Class<? extends Annotation> type) {
        return field.getAnnotation(type) != null;
    }

    /**
     * Method returns declared field from class and it's super classes, it skips static fields.
     */
    public static List<Field> getFields(Class clazz) {
        List<Field> fields = new ArrayList<Field>();
        if (clazz == null) {
            return fields;
        }

        fields.addAll(getFields(clazz.getSuperclass()));

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            fields.add(field);
        }

        return fields;
    }
}
