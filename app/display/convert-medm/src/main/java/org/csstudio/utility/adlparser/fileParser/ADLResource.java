package org.csstudio.utility.adlparser.fileParser;

public class ADLResource {
    private String _name;
    private Object _value;

    public static final String X = "X";
    public static final String Y = "Y";
    public static final String WIDTH = "Width";
    public static final String HEIGHT = "Height";
    public static final String FOREGROUND_COLOR = "ForeGround Color";
    public static final String BACKGROUND_COLOR = "BackGround Color";
    public static final String LINE_WIDTH = "LineWidth";
    public static final String STYLE = "Style";
    public static final String FILL = "Fill";
    public static final String CHANNEL = "Channel";
    public static final String POSTFIX = "Postfix";
    public static final String CONNECTION_STATE = "Connection State";
    public static final String CHILDREN = "Children";
    public static final String POINTS = "Points";
    public static final String RD_LABEL = "Label";
    public static final String RD_NAME = "Name";
    public static final String RD_ARGS = "Args";
    public static final String VISIBILITY = "Visibility";
    public static final String COLOR_RULE = "Color Rule";
    public static final String LOPR_SRC = "LOPR Source";
    public static final String LOPR_DEFAULT = "LOPR Default";
    public static final String HOPR_SRC = "HOPR Source";
    public static final String HOPR_DEFAULT = "HOPR Default";
    public static final String PREC_SRC = "PREC Source";
    public static final String PREC_DEFAULT = "PREC Default";
    public static final String PLOT_TITLE = "title";
    public static final String PLOT_XLABEL = "x label";
    public static final String PLOT_YLABEL = "y label";
    public static final String PEN_COLOR = "color";
    public static final String ADL_OBJECT = "object";
    public static final String ADL_MONITOR = "monitor";
    public static final String ADL_CONTROL = "control";
    public static final String ADL_BASIC_ATTRIBUTE = "basic attribute";
    public static final String ADL_CHILDREN = "children";
    public static final String ADL_DYNAMIC_ATTRIBUTES = "dynamic attributes";
    public static final String ADL_LIMITS = "limits";
    public static final String ADL_MENU_ITEM = "menu item";
    public static final String ADL_PEN = "pen";
    public static final String ADL_PLOTCOM = "plotcom";
    public static final String ADL_PLOTDATA = "plot data";
    public static final String ADL_PLOT_TRACE = "trace";
    public static final String ADL_POINTS = "points";
    public static final String ADL_SENSITIVE = "sensitive";
    public static final String ADL_RELATED_DISPLAY_ITEM = "trace";
    public static final String SNAP_TO_GRID = "snapToGrid";
    public static final String GRID_ON = "gridOn";
    public static final String GRID_SPACING = "gridSpacing";
    public static final String ARC_BEGIN_ANGLE = "begin angle";
    public static final String ARC_PATH_ANGLE = "path angle";
    public static final String DIRECTION = "direction";
    public static final String COLOR_MODE = "color mode";
    public static final String LABEL = "label";
    public static final String FILL_MODE = "fill mode";
    public static final String BYTE_START_BIT = "start bit";
    public static final String BYTE_END_BIT = "start bit";
    public static final String PLOT_COUNT = "count";
    public static final String PLOT_ERASE = "erase";
    public static final String PLOT_TRIGGER = "trigger";
    public static final String PLOT_ERASE_MODE = "erase mode";
    public static final String PLOT_STYLE = "style";
    public static final String PLOT_MODE = "mode";
    public static final String COMPOSITE_FILE = "composite file";
    public static final String X_AXIS_DATA = "x_axis";
    public static final String Y1_AXIS_DATA = "y1_axis";
    public static final String Y2_AXIS_DATA = "y2_axis";
    public static final String IMAGE_NAME = "imageName";
    public static final String IMAGE_TYPE = "imageType";
    public static final String IMAGE_CALC = "imageCalc";
    public static final String PRESS_MSG = "press message";
    public static final String RELEASE_MSG = "release message";
    public static final String UNITS = "units";
    public static final String PERIOD = "period";
    public static final String TEXT_ALIGNMENT = "alignment";
    public static final String TEXT_FORMAT = "format";
    public static final String TEXT_TEXTIX = "textix";
    public static final String VALUATOR_PREC = "increment";
    public static final String PLOT_AXIS_STYLE = "axisStyle";
    public static final String PLOT_RANGE_STYLE = "rangeStyle";
    public static final String PLOT_RANGE_MIN = "minRange";
    public static final String PLOT_RANGE_MAX = "maxRange";
    public static final String PLOT_XDATA = "xData";
    public static final String PLOT_YDATA = "xData";
    public static final String PLOT_DATA_COLOR = "data color";
    public static final String CHANNELB = "chanb";
    public static final String CHANNELC = "chanc";
    public static final String CHANNELD = "chand";
    public static final String CALC = "calc";

    public ADLResource (String name, Object value){
        this._name = name;
        this._value = value;
    }

    /**
     * @param _name the _name to set
     */
    public void setName(String _name) {
        this._name = _name;
    }

    /**
     * @return the _name
     */
    public String getName() {
        return _name;
    }

    /**
     * @param _value the _value to set
     */
    public void setValue(Object _value) {
        this._value = _value;
    }

    /**
     * @return the _value
     */
    public Object getValue() {
        return _value;
    }


}
