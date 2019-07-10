package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLMonitor;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class ByteMonitor extends ADLAbstractWidget {
    private String color_mode = new String("static");
    private String direction = new String("right");
    private int startBit = 15;
    private int endBit = 0;

    public ByteMonitor(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("bar");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }

                }
                else if (childWidget.getType().equals("monitor")){
                    _adlMonitor = new ADLMonitor(childWidget);
                    if (_adlMonitor != null){
                        _hasMonitor = true;
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
                else if (FileLine.argEquals(row[0], "direction")){
                    setDirection(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "sbit")){
                    setStartBit(FileLine.getIntValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "ebit")){
                    setEndBit(FileLine.getIntValue(row[1]));
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
     * @param startBit the startBit to set
     */
    private void setStartBit(int startBit) {
        this.startBit = startBit;
    }
    /**
     * @return the startBit
     */
    public int getStartBit() {
        return startBit;
    }
    /**
     * @param endBit the endBit to set
     */
    private void setEndBit(int endBit) {
        this.endBit = endBit;
    }
    /**
     * @return the endBit
     */
    public int getEndBit() {
        return endBit;
    }
    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_hasObject) ret.add( _adlObject);
        if (_hasMonitor) ret.add( _adlMonitor);
        ret.add(new ADLResource(ADLResource.DIRECTION, direction));
        ret.add(new ADLResource(ADLResource.COLOR_MODE, color_mode));
        ret.add(new ADLResource(ADLResource.BYTE_START_BIT, new Integer(startBit)));
        ret.add(new ADLResource(ADLResource.BYTE_END_BIT, new Integer(endBit)));
        return ret.toArray();
    }

}
