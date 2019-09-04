/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import org.csstudio.display.builder.editor.app.MorphWidgetsMenu;
import org.csstudio.display.builder.model.widgets.ChoiceButtonWidget;
import org.csstudio.display.builder.model.widgets.RadioWidget;

/** Demo widget 'morph'
 *  @author Kay Kasemir
 */
public class MorphWidgetDemo
{
    public static void main(final String[] args)
    {
        final RadioWidget radio = new RadioWidget();
        System.out.println(radio.propItems());

        final ChoiceButtonWidget choice = MorphWidgetsMenu.createNewWidget(ChoiceButtonWidget.WIDGET_DESCRIPTOR, radio);
        System.out.println(choice.propItems());
    }
}
