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
public class MessageButton extends ADLAbstractWidget {
    private String label = "";
    private String press_msg = "";
    private String release_msg = "";
    private String color_mode = "static";

    public MessageButton(ADLWidget adlWidget) {
        super(adlWidget);
        name = "message button";
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {

                if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }

                }
                else if (childWidget.getType().equals("control")){
                    _adlControl = new ADLControl(childWidget);
                    if (_adlControl != null){
                        _hasControl = true;
                    }
                }
            }
            for (FileLine fileLine : adlWidget.getBody()){
                String bodyPart = fileLine.getLine();
                String[] row = bodyPart.trim().split("=");
                if (row.length < 2){
                    throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
                }
                if (FileLine.argEquals(row[0], "label")){
                    setLabel(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "press_msg")){
                    setPress_msg(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "release_msg")){
                    setRelease_msg(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "clrmod")){
                    setColor_mode(FileLine.getTrimmedValue(row[1]));
                }
            }
        }
        catch (WrongADLFormatException ex) {

        }
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param press_msg the press_msg to set
     */
    public void setPress_msg(String press_msg) {
        this.press_msg = press_msg;
    }

    /**
     * @return the press_msg
     */
    public String getPress_msg() {
        return press_msg;
    }

    /**
     * @param release_msg the release_msg to set
     */
    public void setRelease_msg(String release_msg) {
        this.release_msg = release_msg;
    }

    /**
     * @return the release_msg
     */
    public String getRelease_msg() {
        return release_msg;
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
        if (!(label.equals(""))) ret.add(new ADLResource(ADLResource.LABEL, label));
        if (!(press_msg.equals(""))) ret.add(new ADLResource(ADLResource.PRESS_MSG, press_msg));
        if (!(release_msg.equals(""))) ret.add(new ADLResource(ADLResource.RELEASE_MSG, release_msg));
        return ret.toArray();
    }

}
