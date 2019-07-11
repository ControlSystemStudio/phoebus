/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLDisplay;

/**
 *
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public class Display2Model extends AbstractADL2Model<DisplayModel> {

    public Display2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void makeModel(ADLWidget adlWidget, Widget parentModel){
        widgetModel = new DisplayModel();
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ADLDisplay adlDisp = new ADLDisplay(adlWidget);

        setADLObjectProps(adlDisp, widgetModel);
        setColor(adlDisp.getBackgroundColor(), CommonWidgetProperties.propBackgroundColor);
        widgetModel.setPropertyValue(DisplayModel.propGridVisible, adlDisp.is_gridOn());
        widgetModel.setPropertyValue(DisplayModel.propGridStepX.getName(), adlDisp.get_gridSpacing());
    }
}
