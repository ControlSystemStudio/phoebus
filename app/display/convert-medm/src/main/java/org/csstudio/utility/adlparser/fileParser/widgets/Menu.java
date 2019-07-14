package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLControl;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class Menu extends ADLAbstractWidget {
    private String color_mode = new String("static");

    public Menu(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("menu");
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
    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_hasObject) ret.add( _adlObject);
        if (!(color_mode.equals(""))) ret.add(new ADLResource(ADLResource.COLOR_MODE, color_mode));
        return ret.toArray();
    }
}
