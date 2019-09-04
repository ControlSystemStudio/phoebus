/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ArcWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Arc;

public class Arc2Model extends AbstractADL2Model<ArcWidget> {

    public Arc2Model(ADLWidget adlWidget, WidgetColor[] colorMap,  Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        Arc arcWidget = new Arc(adlWidget);
        setADLObjectProps(arcWidget, widgetModel);
        setADLBasicAttributeProps(arcWidget, widgetModel, true);
        setADLDynamicAttributeProps(arcWidget, widgetModel);
        widgetModel.propArcStart().setValue(arcWidget.get_begin()/64.0);
        widgetModel.propArcSize().setValue(arcWidget.get_path()/64.0);

        //check fill parameters
        widgetModel.propTransparent().setValue(true);
        if ( arcWidget.hasADLBasicAttribute() )
        {
            setShapesColorFillLine(arcWidget);
            if (arcWidget.getAdlBasicAttribute().getFill().equals("solid") )
                widgetModel.propTransparent().setValue(false);
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget, Widget parentModel) {
        widgetModel = new ArcWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);

    }
}
