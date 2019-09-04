package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLChildren;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class Composite extends ADLAbstractWidget {
    private String _compositeFile = new String();
    private boolean _hasCompositeFile = false;
    private ADLChildren _adlChildren = null;
//    private ArrayList<ADLWidget> _children;

    public Composite(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("composite");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }
                }
                else if (childWidget.getType().equals("children")){
                    _adlChildren = new ADLChildren(childWidget);
                }
                else if (childWidget.getType().equals("dynamic attribute")){
                    _adlDynamicAttribute = new ADLDynamicAttribute(childWidget);
                    if (_adlDynamicAttribute != null){
                        _hasDynamicAttribute = true;
                    }
                }
            }
            for (FileLine fileLine : adlWidget.getBody()) {
                String obj = fileLine.getLine();
                String[] row = obj.trim().split("=", 2); //$NON-NLS-1$
                if(row.length<2){
                    throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin+obj+Messages.Label_WrongADLFormatException_Parameter_End);
                }
                if(FileLine.argEquals(row[0], "composite file") || FileLine.argEquals(row[0], "composite name")){ //$NON-NLS-1$
                    set_compositeFile(FileLine.getTrimmedValue(row[1]));
                    _hasCompositeFile = true;
                    if (_compositeFile.replaceAll("\"", "").equals("")){
                        _hasCompositeFile = false;
                    }
                }
            }
        }
        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param _compositeFile the _compositeFile to set
     */
    private void set_compositeFile(String _compositeFile) {
        this._compositeFile = _compositeFile;
    }

    /**
     * @return the _compositeFile
     */
    public String get_compositeFile() {
        return _compositeFile;
    }

    /**
     *
     * @return
     */
    public boolean hasCompositeFile(){
        return _hasCompositeFile;
    }

    /**
     *
     * @return
     */
    public List<ADLWidget> getChildWidgets() {
        if (_adlChildren != null){
            return _adlChildren.getAdlChildrens();
        }
        else return null;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null ) ret.add( _adlObject);
        if (_adlDynamicAttribute != null) ret.add( _adlDynamicAttribute);
        if (_adlChildren != null ) ret.add(_adlChildren);
        if (!_compositeFile.equals("")) ret.add(new ADLResource(ADLResource.COMPOSITE_FILE, _compositeFile));
        return ret.toArray();
    }
}
