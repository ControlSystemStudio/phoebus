package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;

/**
 *
 * @author hammonds
 *
 */
public class Rectangle extends ADLAbstractWidget {

    public Rectangle(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("rectangle");
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
