package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgetParts.RelatedDisplayItem;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class RelatedDisplay extends ADLAbstractWidget implements IWidgetWithColorsInBase {
    private String label = new String();
    private String visual = new String("menu");
    private int bclr;
    private int clr;
    private ArrayList<RelatedDisplayItem> rdItems = new ArrayList<>();
    private boolean _isBackColorDefined;
    private boolean _isForeColorDefined;

    public RelatedDisplay(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("related display");
        set_isForeColorDefined(false);
        set_isBackColorDefined(false);

        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("object")){
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
                if (FileLine.argEquals(row[0], "label")){
                    setLabel(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "clr")){
                    setClr(FileLine.getIntValue(row[1]));
                    set_isForeColorDefined(true);
                }
                else if (FileLine.argEquals(row[0], "bclr")){
                    setBclr(FileLine.getIntValue(row[1]));
                    set_isBackColorDefined(true);
                }
                else if (FileLine.argEquals(row[0], "visual")){
                    setVisual(FileLine.getTrimmedValue(row[1]));
                }
            }
            for (ADLWidget item : adlWidget.getObjects()){
                if (item.getType().startsWith("display[")){
                    rdItems.add(new RelatedDisplayItem(item));
                }
            }
        }
        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
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
     * @param clr the foreground color
     */
    @Override
    public void setClr(int clr) {
        this.clr = clr;
    }

    /**
     * @return the foreground color
     */
    @Override
    public int getForegroundColor() {
        return clr;
    }

    /**
     * @param bclr the background color
     */
    @Override
    public void setBclr (int bclr) {
        this.bclr = bclr;
    }

    /**
     * @return the background color
     */
    @Override
    public int getBackgroundColor() {
        return bclr;
    }
    /**
     * @return array of RelatedDisplayItem
     */
    public RelatedDisplayItem[] getRelatedDisplayItems(){
        if ( rdItems.size() > 0 ){
            RelatedDisplayItem[] retItems = new RelatedDisplayItem[rdItems.size()];
            for (int ii=0; ii<rdItems.size(); ii++){
                retItems[ii] = rdItems.get(ii);
            }
            return retItems;
        }
        return null;
    }


    /**
     * @param _isBackColorDefined the _isBackColorDefined to set
     */
    private void set_isBackColorDefined(boolean _isBackColorDefined) {
        this._isBackColorDefined = _isBackColorDefined;
    }


    /**
     * @return the _isBackColorDefined
     */
    @Override
    public boolean isBackColorDefined() {
        return _isBackColorDefined;
    }


    /**
     * @param _isForeColorDefined the _isForeColorDefined to set
     */
    private void set_isForeColorDefined(boolean _isForeColorDefined) {
        this._isForeColorDefined = _isForeColorDefined;
    }


    /**
     * @return the _isForeColorDefined
     */
    @Override
    public boolean isForeColorDefined() {
        return _isForeColorDefined;
    }


    /**
     * @param visual the visual to set
     */
    public void setVisual(String visual) {
        this.visual = visual;
    }


    /**
     * @return the visual
     */
    public String getVisual() {
        return visual;
    }


    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null) ret.add( _adlObject);
        if (!(label.equals(""))) ret.add(new ADLResource(ADLResource.LABEL, label));
        if (!(visual.equals(""))) ret.add(new ADLResource(ADLResource.COLOR_MODE, visual));
        if (!(_isBackColorDefined)) ret.add(new ADLResource(ADLResource.BACKGROUND_COLOR, bclr));
        if (!(_isForeColorDefined)) ret.add(new ADLResource(ADLResource.FOREGROUND_COLOR, clr));
        for (RelatedDisplayItem item : rdItems){
            if (item!=null){
                ret.add(item);
            }
        }

        return ret.toArray();
    }
}

