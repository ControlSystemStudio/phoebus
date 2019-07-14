/*************************************************************************\
 * Copyright (c) 2010  UChicago Argonne, LLC
 * This file is distributed subject to a Software License Agreement found
 * in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.PictureWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Image;

public class Image2Model extends AbstractADL2Model<PictureWidget> {

    public Image2Model(ADLWidget adlWidget, WidgetColor[] colorMap,
            Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Image2Model";
        Image imageWidget = new Image(adlWidget);
        setADLObjectProps(imageWidget, widgetModel);
        setADLDynamicAttributeProps(imageWidget, widgetModel);
        widgetModel.propFile().setValue(imageWidget.getImageName());
        widgetModel.propStretch().setValue(true);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new PictureWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}
