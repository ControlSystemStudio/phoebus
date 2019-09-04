/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ByteMonitor;

public class Byte2Model extends AbstractADL2Model<ByteMonitorWidget> {

    public Byte2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception
    {
        ByteMonitor byteWidget = new ByteMonitor(adlWidget);
        setADLObjectProps(byteWidget, widgetModel);
        setADLMonitorProps(byteWidget, widgetModel);

        widgetModel.propSquare().setValue(true);

        final int start = Math.min(byteWidget.getStartBit(), byteWidget.getEndBit());
        final int count = Math.abs(byteWidget.getEndBit() - byteWidget.getStartBit()) + 1;
        widgetModel.propStartBit().setValue(start);
        widgetModel.propNumBits().setValue(count);

        // ByteMonitorWidget defaults to start bit at right resp. bottom.
        // MEDM is reverted: Start bit left resp. top.
        // So MEDM start=3, end=0 means: [3|2|1|0],
        // which is the 'normal' mode for the display builder with start = 0, 4 bits, not reversed.
        // If MEDM has start=0, end=3, this means: [0|1|2|3],
        // which for the display builder with start = 0, 4 bits means "reversed".
        widgetModel.propBitReverse().setValue(byteWidget.getEndBit() > byteWidget.getStartBit());

        // Permitted values:  "right", "down"
        widgetModel.propHorizontal().setValue(! byteWidget.getDirection().equals("down"));
    }

    @Override
    public void makeModel(ADLWidget adlWidget, Widget parentModel)
    {
        widgetModel = new ByteMonitorWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}
