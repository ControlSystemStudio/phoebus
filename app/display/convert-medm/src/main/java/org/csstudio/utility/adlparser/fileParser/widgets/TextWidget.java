package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class TextWidget extends ADLAbstractWidget implements ITextWidget{
    private String textix = new String();
    private String alignment = new String();
    private String color_mode = new String();
    private String format = new String();

    public TextWidget(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("text");
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
            for (FileLine fileLine : adlWidget.getBody()){
                String bodyPart = fileLine.getLine();
                String[] row = bodyPart.trim().split("=", 2);
                if (row.length < 2){
                    throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
                }
                if (FileLine.argEquals(row[0], "textix")){
                    setTextix(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "align")){
                    setAlignment(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "color_mode")){
                    setColor_mode(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "format")){
                    setFormat(FileLine.getTrimmedValue(row[1]));
                }
            }
        }
        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }
    }

    private void setTextix(String inString){
        textix = inString;
    }
    /**
     * @return the textix
     */
    public String getTextix() {
        return textix;
    }

    /**
     * @param alignment the alignment to set
     */
    @Override
    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    /**
     * @return the alignment
     */
    @Override
    public String getAlignment() {
        return alignment;
    }

    /**
     * @param clrmod the clrmod to set
     */
    public void setColor_mode(String clrmod) {
        this.color_mode = clrmod;
    }

    /**
     * @return the clrmod
     */
    public String getColor_mode() {
        return color_mode;
    }

    /**
     * @param format the format to set
     */
    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the format
     */
    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null) ret.add( _adlObject);
        if (_adlBasicAttribute != null) ret.add( _adlBasicAttribute);
        if (_adlDynamicAttribute != null) ret.add( _adlDynamicAttribute);
        if (!(color_mode.equals(""))) ret.add(new ADLResource(ADLResource.COLOR_MODE, color_mode));
        if (!(alignment.equals(""))) ret.add(new ADLResource(ADLResource.TEXT_ALIGNMENT, alignment));
        if (!(format.equals(""))) ret.add(new ADLResource(ADLResource.TEXT_FORMAT, format));
        if (!(textix.equals(""))) ret.add(new ADLResource(ADLResource.TEXT_TEXTIX, textix));
        return ret.toArray();
    }

}
