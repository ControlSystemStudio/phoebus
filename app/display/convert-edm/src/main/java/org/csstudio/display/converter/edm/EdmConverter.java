/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.converter.edm.widgets.ConverterBase;
import org.csstudio.opibuilder.converter.model.EdmDisplay;
import org.csstudio.opibuilder.converter.model.EdmEntity;
import org.csstudio.opibuilder.converter.model.EdmWidget;

/** Convert {@link EdmDisplay} to {@link DisplayModel}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmConverter
{
    private final DisplayModel model = new DisplayModel();

    public EdmConverter(final String name, final EdmDisplay edm)
    {
        model.propName().setValue(name);
        model.propX().setValue(edm.getX());
        model.propY().setValue(edm.getY());
        model.propWidth().setValue(edm.getW());
        model.propHeight().setValue(edm.getH());

        // TODO Global edm.getFont()?
        ConverterBase.convertColor(edm.getBgColor(), model.propBackgroundColor());

        if (edm.getTitle() != null)
            model.propName().setValue(edm.getTitle());

        model.propGridVisible().setValue(edm.isShowGrid());
        if (edm.getGridSize() > 0)
        {
            model.propGridStepX().setValue(edm.getGridSize());
            model.propGridStepY().setValue(edm.getGridSize());
        }

        convertWidgets(model, edm.getWidgets());
    }

    /** @return {@link DisplayModel} */
    public DisplayModel getDisplayModel()
    {
        return model;
    }



    /** Convert several widgets
     *  @param parent Parent widget (display, group, ...)
     *  @param widgets Widgets to convert under that parent
     */
    private void convertWidgets(final Widget parent, final Collection<EdmEntity> widgets)
    {
        for (EdmEntity widget : widgets)
            convertWidget(parent, widget);
    }

    /** Convert one widget
     *  @param parent Parent
     *  @param edm EDM widget to convert
     */
    private void convertWidget(final Widget parent, final EdmEntity edm)
    {
        // Given an EDM Widget type like "activeXTextClass",
        // locate the matching "Convert_activeXTextClass"
        final String wc_name = ConverterBase.class.getPackageName() + ".Convert_" + edm.getType();

        Class<?> clazz;
        try
        {
            clazz = Class.forName(wc_name);
        }
        catch (ClassNotFoundException ex)
        {
            logger.log(Level.WARNING, "No converter for EDM " + edm.getType());
            return;
        }

        // Simply constructing the converter will perform the conversion
        try
        {
            for (Constructor<?> c : clazz.getConstructors())
            {   // Look for suitable constructor
                final Class<?>[] parms = c.getParameterTypes();
                if (parms.length == 3  &&
                    parms[0] == EdmConverter.class &&
                    parms[1] == Widget.class       &&
                    EdmWidget.class.isAssignableFrom(parms[2]))
                {
                    c.newInstance(this, parent, edm);
                    return;
                }
            }
            throw new Exception(clazz.getSimpleName() + " lacks required constructor");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot convert " + edm.getType(), ex);
        }
    }
}
