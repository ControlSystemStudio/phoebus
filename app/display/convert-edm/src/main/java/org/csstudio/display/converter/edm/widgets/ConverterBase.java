/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;

@SuppressWarnings("nls")
public abstract class ConverterBase<W extends Widget>
{
    protected final W widget;

    public ConverterBase(final EdmConverter converter, final Widget parent, final EdmWidget t)
    {
        widget = createWidget();

        // TODO Correct offset of parent widget?
        widget.propX().setValue(t.getX());
        widget.propY().setValue(t.getY());
        widget.propWidth().setValue(t.getW());
        widget.propHeight().setValue(t.getH());

        // TODO See OpiWidget for visPv


        final ChildrenProperty parent_children = ChildrenProperty.getChildren(parent);
        if (parent_children == null)
            throw new IllegalStateException("Cannot add as child to " + parent);
        parent_children.addChild(widget);
    }

    protected abstract W createWidget();
}
