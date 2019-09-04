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
public class Arc extends ADLAbstractWidget {
    private int _begin = 0;
    private int _path = 5760;         //default 90 degrees

    public Arc(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("arc");
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
            for (FileLine fileLine : adlWidget.getBody()) {
                String parameter = fileLine.getLine();
                if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                    continue;
                }
                String[] row = parameter.split("="); //$NON-NLS-1$
                if(row.length!=2){
                    throw new WrongADLFormatException(Messages.ADLMonitor_WrongADLFormatException_Begin+parameter+Messages.ADLMonitor_WrongADLFormatException_End);
                }
                if(FileLine.argEquals(row[0], "begin")){ //$NON-NLS-1$
                    set_begin(FileLine.getIntValue(row[1]));
                }else if(FileLine.argEquals(row[0], "path")){ //$NON-NLS-1$
                    set_path(FileLine.getIntValue(row[1]));
                }else {
                    throw new WrongADLFormatException(Messages.ADLMonitor_WrongADLFormatException_Parameter_Begin+row[0]+Messages.ADLMonitor_WrongADLFormatException_Parameter_End+parameter);
                }

            }
        }
        catch (WrongADLFormatException ex) {

        }
    }

    /**
     * @param _begin the _begin to set
     */
    private void set_begin(int _begin) {
        this._begin = _begin;
    }

    /**
     * @return the _begin
     */
    public int get_begin() {
        return _begin;
    }

    /**
     * @param _path the _path to set
     */
    private void set_path(int _path) {
        this._path = _path;
    }

    /**
     * @return the _path
     */
    public int get_path() {
        return _path;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_hasObject) ret.add( _adlObject);
        if (_hasBasicAttribute) ret.add( _adlBasicAttribute);
        if (_hasDynamicAttribute) ret.add( _adlDynamicAttribute);
        ret.add(new ADLResource(ADLResource.ARC_BEGIN_ANGLE, new Integer(_begin)));
        ret.add(new ADLResource(ADLResource.ARC_PATH_ANGLE, new Integer(_path)));
        return ret.toArray();
    }

}
