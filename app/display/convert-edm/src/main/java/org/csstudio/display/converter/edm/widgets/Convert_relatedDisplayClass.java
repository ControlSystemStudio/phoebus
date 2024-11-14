/*******************************************************************************
 * Copyright (c) 2019-2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmBoolean;
import org.csstudio.opibuilder.converter.model.EdmString;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_relatedDisplayClass;
import org.csstudio.display.actions.OpenDisplayAction;
import org.phoebus.framework.macros.Macros;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.csstudio.display.converter.edm.Converter.logger;

/**
 * Convert an EDM widget into Display Builder counterpart
 *
 * @author Kay Kasemir
 * @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_relatedDisplayClass extends ConverterBase<ActionButtonWidget> {
    public Convert_relatedDisplayClass(final EdmConverter converter, final Widget parent, final Edm_relatedDisplayClass t) {
        super(converter, parent, t);

        if (t.isInvisible()) {
            widget.propBackgroundColor().setValue(NamedWidgetColors.TRANSPARENT);
            widget.propTransparent().setValue(true);
        } else
            convertColor(t.getBgColor(), widget.propBackgroundColor());
        convertColor(t.getFgColor(), widget.propForegroundColor());
        convertFont(t.getFont(), widget.propFont());
        
        if (widget.propTransparent().getValue())
        {   // EDM used the fore & back colors for the button, not the menu,
            // which was always black-on-gray.
            // For the display builder, the foreground color is used
            // in the menu, which can then become unreadable
            // --> Force bright foreground color to black
            final WidgetColor color = widget.propForegroundColor().getValue();
            if (color.getRed() + color.getGreen() + color.getBlue() > 100)
            {
                logger.log(Level.INFO, "Change transparent EDM Related Display button color " + color + " to black");
                widget.propForegroundColor().setValue(new WidgetColor(0, 0, 0));
            }
        }

        List<ActionInfo> actions = new ArrayList<>();
        for (int i = 0; i < t.getNumDsps(); ++i) {
            final String is = Integer.toString(i);
            final EdmString menuLabel = t.getMenuLabel().getEdmAttributesMap().get(is);
            final String description = menuLabel != null ? menuLabel.get() : "";
            final String path = convertDisplayPath(t.getDisplayFileName().getEdmAttributesMap().get(is).get());
            if (path == null)
                continue;
            converter.addLinkedDisplay(path);

            Macros macros = new Macros();
            final EdmString symbols = t.getSymbols().getEdmAttributesMap().get(is);
            if (symbols != null)
                try {
                    macros = Macros.fromSimpleSpec(symbols.get());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error in macros for related display " + path + " '" + symbols.get() + "'", ex);
                }

            final EdmBoolean closeDisplay = t.getCloseDisplay().getEdmAttributesMap().get(is);
            final OpenDisplayAction.Target target = closeDisplay != null && closeDisplay.is() ? OpenDisplayAction.Target.REPLACE : OpenDisplayAction.Target.TAB.TAB;
            OpenDisplayAction openDisplayAction =
                    new OpenDisplayAction(description,
                            path,
                            macros,
                            target);
            actions.add(openDisplayAction);
        }
        widget.propActions().setValue(new ActionInfos(actions));

        if (t.getButtonLabel() != null && !t.isInvisible())
            widget.propText().setValue(t.getButtonLabel());
        else
            widget.propText().setValue("");
    }

    @Override
    protected ActionButtonWidget createWidget(final EdmWidget edm) {
        return new ActionButtonWidget();
    }
}
