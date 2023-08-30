/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import static org.csstudio.display.converter.medm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget.Resize;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.csstudio.display.converter.medm.Converter;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Composite;

// Turns into either GroupWidget or EmbeddedDisplayWidget
@SuppressWarnings("nls")
public class Composite2Model extends AbstractADL2Model<Widget> {

    public Composite2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void makeModel(ADLWidget adlWidget, Widget parentModel)
    {
        Composite compositeWidget = new Composite(adlWidget);

        if (compositeWidget.hasCompositeFile())
            widgetModel = new EmbeddedDisplayWidget();
        else
        {
            widgetModel = new GroupWidget();
            ((GroupWidget)widgetModel).propTransparent().setValue(true);
            ((GroupWidget)widgetModel).propStyle().setValue(Style.NONE);
        }
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Composite2Model";
        Composite compositeWidget = new Composite(adlWidget);

        setADLObjectProps(compositeWidget, widgetModel);
        setADLObjectProps(compositeWidget, widgetModel);
        setADLDynamicAttributeProps(compositeWidget, widgetModel);

        if (widgetModel instanceof EmbeddedDisplayWidget)
        {
            final EmbeddedDisplayWidget embedded = (EmbeddedDisplayWidget) widgetModel;

            // Expect "path_to_file;macros"
            String[] compositeFile = compositeWidget.get_compositeFile().replaceAll("\"", "").split(";");
            if (compositeFile.length > 0)
            {
                embedded.propFile().setValue(compositeFile[0].replace(".adl", ".bob"));

                if (compositeFile.length > 1 && compositeFile[1].length() > 0)
                    embedded.propMacros().setValue(makeMacros(compositeFile[1]));
            }
            else
                throw new Exception("Missing file name for composite");

            // Don't resize, no border to avoid unexpected growth/shrinkage
            embedded.propResize().setValue(Resize.None);
        }
        else
        {
            final GroupWidget group = (GroupWidget) widgetModel;

            if (compositeWidget.getChildWidgets() != null)
            {
                Converter.convertChildren(compositeWidget.getChildWidgets(), widgetModel, colorMap);

                // Move child positions to be relative to group
                final int compositeX = widgetModel.propX().getValue();
                final int compositeY = widgetModel.propY().getValue();

                for (Widget model : group.runtimeChildren().getValue())
                {
                    model.propX().setValue(model.propX().getValue() - compositeX);
                    model.propY().setValue(model.propY().getValue() - compositeY);
                }
            }
            else
                logger.log(Level.WARNING, "Empty composite");
        }
    }
}
