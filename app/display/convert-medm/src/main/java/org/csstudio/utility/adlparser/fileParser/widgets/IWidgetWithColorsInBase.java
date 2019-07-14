package org.csstudio.utility.adlparser.fileParser.widgets;

public interface IWidgetWithColorsInBase {
    public boolean isForeColorDefined();
    public boolean isBackColorDefined();
    public void setBclr(int clr);
    public int getForegroundColor();
    public void setClr(int clr);
    public int getBackgroundColor();
}
