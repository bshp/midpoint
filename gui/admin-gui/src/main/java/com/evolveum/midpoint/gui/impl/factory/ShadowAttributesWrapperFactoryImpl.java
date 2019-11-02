/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;


@Component
public class ShadowAttributesWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {


    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ResourceAttributeContainerDefinition && ShadowAttributesType.class.isAssignableFrom(((ResourceAttributeContainerDefinition) def).getCompileTimeClass());
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent,
            PrismContainer<C> childContainer, ItemStatus status) {
        getRegistry().registerWrapperPanel(ShadowAttributesType.COMPLEX_TYPE, PrismContainerPanel.class);
        return new PrismContainerWrapperImpl<C>((PrismContainerValueWrapper<C>) parent, childContainer, status);
    }

//    @Override
//    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<C> parent,
//            PrismContainerValue<C> value) {
//        ObjectClassComplexTypeDefinition occtDef = (ObjectClassComplexTypeDefinition) parent.getComplexTypeDefinition();
//
//        return occtDef.getDefinitions();
//    }
}
