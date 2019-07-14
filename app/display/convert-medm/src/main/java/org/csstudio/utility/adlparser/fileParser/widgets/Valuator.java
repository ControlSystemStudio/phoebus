package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLControl;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLLimits;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class Valuator extends ADLAbstractWidget {
    private String color_mode = new String("static");
    private String direction = new String("right");
    private String label = new String("none");
    private float increment = 1.0f;

    public Valuator(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("valuator");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("control")){
                    _adlControl = new ADLControl(childWidget);
                    if (_adlControl != null){
                        _hasControl = true;
                    }
                }
                else if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }

                }
                else if (childWidget.getType().equals("limits")){
                    _adlLimits = new ADLLimits(childWidget);
                    if (_adlLimits != null){
                        _hasLimits = true;
                    }
                }
            }
            for (FileLine fileLine : adlWidget.getBody()){
                String bodyPart = fileLine.getLine();
                String[] row = bodyPart.trim().split("=");
                if (row.length < 2){
                    throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
                }
                if (FileLine.argEquals(row[0], "clrmod")){
                    setColor_mode(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "label")){
                    setLabel(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "direction")){
                    setDirection(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "dPrecision")){
                    setIncrement(FileLine.getFloatValue(row[1]));
                }
            }
        }
        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param color_mode the color_mode to set
     */
    private void setColor_mode(String color_mode) {
        this.color_mode = color_mode;
    }

    /**
     * @return the color_mode
     */
    public String getColor_mode() {
        return color_mode;
    }
    /**
     * @param direction the direction to set
     */
    private void setDirection(String direction) {
        this.direction = direction;
    }
    /**
     * @return the direction
     */
    public String getDirection() {
        return direction;
    }
    /**
     * @param label the label to set
     */
    private void setLabel(String label) {
        this.label = label;
    }
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param increment the increment to set
     */
    private void setIncrement(float increment) {
        this.increment = increment;
    }

    /**
     * @return the increment
     */
    public float getIncrement() {
        return increment;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null) ret.add( _adlObject);
        if (_adlControl != null) ret.add( _adlControl);
        if (_adlLimits != null) ret.add( _adlLimits);
        ret.add(new ADLResource(ADLResource.DIRECTION, direction));
        ret.add(new ADLResource(ADLResource.COLOR_MODE, color_mode));
        ret.add(new ADLResource(ADLResource.LABEL, label));
        ret.add(new ADLResource(ADLResource.VALUATOR_PREC, increment));
        return ret.toArray();
    }
}
