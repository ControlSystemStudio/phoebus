package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgetParts.CommandItem;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class ShellCommand extends ADLAbstractWidget implements IWidgetWithColorsInBase {
    private int _clr;
    private int _bclr;
    private boolean _isBackColorDefined;
    private boolean _isForeColorDefined;
    private String label = new String();
    private ArrayList<CommandItem> cmdItems = new ArrayList<>();

    public ShellCommand(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("shell command");
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
                if(FileLine.argEquals(row[0], "clr")){ //$NON-NLS-1$
                    setClr(FileLine.getIntValue(row[1]));
                    set_isForeColorDefined(true);
                }else if(FileLine.argEquals(row[0], "bclr")){ //$NON-NLS-1$
                    setBclr(FileLine.getIntValue(row[1]));
                    set_isBackColorDefined(true);
                }else if(FileLine.argEquals(row[0], "label")){ //$NON-NLS-1$
                    setLabel(FileLine.getTrimmedValue(row[1]));
                }
            }
            for (ADLWidget item : adlWidget.getObjects()){
                if (item.getType().startsWith("command[")){
                    cmdItems.add(new CommandItem(item));
                }
            }
        }
        catch (WrongADLFormatException ex) {

        }
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

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        return ret.toArray();
    }

    /**
     * @param _clr the _clr to set
     */
    @Override
    public void setClr(int _clr) {
        this._clr = _clr;
    }

    /**
     * @return the _clr
     */
    @Override
    public int getForegroundColor() {
        return _clr;
    }

    /**
     * @param _bclr the _bclr to set
     */
    @Override
    public void setBclr(int _bclr) {
        this._bclr = _bclr;
    }

    /**
     * @return the _bclr
     */
    @Override
    public int getBackgroundColor() {
        return _bclr;
    }

    /**
     * @param _isBackColorDefined the _isBackColorDefined to set
     */
    public void set_isBackColorDefined(boolean _isBackColorDefined) {
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
    public void set_isForeColorDefined(boolean _isForeColorDefined) {
        this._isForeColorDefined = _isForeColorDefined;
    }

    /**
     * @return the _isForeColorDefined
     */
    @Override
    public boolean isForeColorDefined() {
        return _isForeColorDefined;
    }
    public CommandItem[] getCommandItems(){
        if ( cmdItems.size() > 0 ){
            CommandItem[] retItems = new CommandItem[cmdItems.size()];
            for (int ii=0; ii<cmdItems.size(); ii++){
                retItems[ii] = cmdItems.get(ii);
            }
            return retItems;
        }
        return null;
    }
}
