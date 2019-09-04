package org.csstudio.utility.adlparser.fileParser.widgets;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;

/**
 *
 * @author hammonds
 *
 */
public class Line extends PolyLine {

    public Line(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("line");
    }

}
