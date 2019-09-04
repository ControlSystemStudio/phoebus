package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class Image extends ADLAbstractWidget {
    private String imageType = new String("gif");
    private String imageName =  new String();
    private String imageCalc = new String();

    public Image(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("image");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("object")){
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
                String[] row = bodyPart.trim().split("=");
                if (row.length < 2){
                    throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
                }
                if (FileLine.argEquals(row[0], "type")){
                    setImageType(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "image name")){
                    setImageName(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "calc")){
                    setImageCalc(FileLine.getTrimmedValue(row[1]));
                }
            }
        }
        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param imageType the imageType to set
     */
    private void setImageType(String imageType) {
        this.imageType = imageType;
    }

    /**
     * @return the imageType
     */
    public String getImageType() {
        return imageType;
    }

    /**
     * @param imageName the imageName to set
     */
    private void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /**
     * @return the imageName
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * @param imageCalc the imageCalc to set
     */
    private void setImageCalc(String imageCalc) {
        this.imageCalc = imageCalc;
    }

    /**
     * @return the imageCalc
     */
    public String getImageCalc() {
        return imageCalc;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null) ret.add( _adlObject);
        if (_adlDynamicAttribute != null) ret.add( _adlDynamicAttribute);
        if (!(imageName.equals(""))) ret.add(new ADLResource(ADLResource.IMAGE_NAME, imageName));
        if (!(imageType.equals(""))) ret.add(new ADLResource(ADLResource.IMAGE_TYPE, imageType));
        if (!(imageCalc.equals(""))) ret.add(new ADLResource(ADLResource.IMAGE_CALC, imageCalc));

        return ret.toArray();
    }

}
