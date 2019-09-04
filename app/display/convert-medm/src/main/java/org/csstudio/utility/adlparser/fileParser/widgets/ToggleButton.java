package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;

public class ToggleButton extends ADLAbstractWidget {

    public ToggleButton(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("toggle button");
        // TODO Auto-generated constructor stub
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_hasObject) ret.add( _adlObject);
        return ret.toArray();
    }

}
