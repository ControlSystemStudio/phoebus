package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPoints;

/**
 *
 * @author hammonds
 *
 */
public class PolyLine extends ADLAbstractWidget {

    public PolyLine(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("polyline");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("basic attribute")){
                    _adlBasicAttribute = new ADLBasicAttribute(childWidget);
                    if (_adlBasicAttribute != null){
                        _hasBasicAttribute = true;
                    }
                }
                else if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }

                }
                else if (childWidget.getType().equals("dynamic attribute")){
                    _adlDynamicAttribute = new ADLDynamicAttribute(childWidget);
                    if (_adlDynamicAttribute != null){
                        _hasDynamicAttribute = true;
                    }
                }
                else if (childWidget.getType().equals("points")){
                    _adlPoints = new ADLPoints(childWidget);
                    if (_adlPoints != null){
                        _hasPoints = true;
                    }
                }
            }
        }
        catch (WrongADLFormatException ex) {

        }
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null) ret.add( _adlObject);
        if (_adlBasicAttribute != null) ret.add( _adlBasicAttribute);
        if (_adlDynamicAttribute != null) ret.add( _adlDynamicAttribute);
        if (_adlPoints != null) ret.add( _adlPoints);
        return ret.toArray();
    }

}
