/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public class ObjectBrowserModalTable<T> extends Table<ObjectBrowserModal<T>>{

    public ObjectBrowserModalTable(ObjectBrowserModal<T> parent, SelenideElement parentElement){
        super(parent, parentElement);
    }

    public T clickByName(String name){
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        getParent()
                .getParentElement()
                .waitUntil(Condition.disappears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return getParent().getParent();
    }
}
