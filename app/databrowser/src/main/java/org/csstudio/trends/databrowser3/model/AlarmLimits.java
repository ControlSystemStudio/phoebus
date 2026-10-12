/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.csstudio.trends.databrowser3.model;

import javafx.scene.paint.Color;

import java.util.logging.Level;

import static org.csstudio.trends.databrowser3.Activator.logger;

public class AlarmLimits {

    private AlarmLimitItem loloAlarmLimitItem;
    private AlarmLimitItem lowAlarmLimitItem;
    private AlarmLimitItem highAlarmLimitItem;
    private AlarmLimitItem hihiAlarmLimitItem;

    private Color majorColor;
    private Color minorColor;

    private final PVItem parentItem;
    private final Model model;

    public AlarmLimits(PVItem parentItem, Model model) {
        this.parentItem = parentItem;
        this.model = model;
    }

    public Color getMajorColor() {
        return majorColor;
    }

    public Color getMinorColor() {
        return minorColor;
    }

    public void setMajorColor(Color color) {
        this.majorColor = color;
        hihiAlarmLimitItem.setColor(majorColor);
        loloAlarmLimitItem.setColor(majorColor);
    }

    public void setMinorColor(Color color) {
        this.minorColor = color;
        lowAlarmLimitItem.setColor(minorColor);
        highAlarmLimitItem.setColor(minorColor);
    }

    public void showAlarmLimits(boolean show){
        if(show){
            showAlarmLimitTraces();
        }
        else{
            hideAlarmLimitTraces();
        }
    }

    private void showAlarmLimitTraces(){
        for(AlarmLimitItem.LimitType limitType : AlarmLimitItem.LimitType.values()) {
            switch (limitType) {
                case LOLO:
                    loloAlarmLimitItem = new AlarmLimitItem(parentItem, limitType);
                    loloAlarmLimitItem.setAxis(parentItem.getAxis());
                    loloAlarmLimitItem.setDisplayName("");
                    loloAlarmLimitItem.setColor(majorColor == null ? parentItem.getPaintColor() : majorColor);
                    loloAlarmLimitItem.setLineStyle(parentItem.getLineStyle());
                    loloAlarmLimitItem.setLineWidth(3);
                    try {
                        model.addItem(loloAlarmLimitItem);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to create alarm LOLO limit item for " + parentItem.getName(), e);
                    }
                    break;
                case LOW:
                    lowAlarmLimitItem = new AlarmLimitItem(parentItem, limitType);
                    lowAlarmLimitItem.setAxis(parentItem.getAxis());
                    lowAlarmLimitItem.setDisplayName("");
                    lowAlarmLimitItem.setColor(minorColor == null ? parentItem.getPaintColor() : minorColor);
                    lowAlarmLimitItem.setLineStyle(parentItem.getLineStyle());
                    lowAlarmLimitItem.setLineWidth(3);
                    try {
                        model.addItem(lowAlarmLimitItem);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to create alarm LOW limit item for " + parentItem.getName(), e);
                    }
                    break;
                case HIGH:
                    highAlarmLimitItem = new AlarmLimitItem(parentItem, limitType);
                    highAlarmLimitItem.setAxis(parentItem.getAxis());
                    highAlarmLimitItem.setDisplayName("");
                    highAlarmLimitItem.setColor(minorColor == null ? parentItem.getPaintColor() : minorColor);
                    highAlarmLimitItem.setLineStyle(parentItem.getLineStyle());
                    highAlarmLimitItem.setLineWidth(3);
                    try {
                        model.addItem(highAlarmLimitItem);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to create alarm HIGH limit item for " + parentItem.getName(), e);
                    }
                    break;
                case HIHI:
                    hihiAlarmLimitItem = new AlarmLimitItem(parentItem, limitType);
                    hihiAlarmLimitItem.setAxis(parentItem.getAxis());
                    hihiAlarmLimitItem.setDisplayName("");
                    hihiAlarmLimitItem.setColor(majorColor == null ? parentItem.getPaintColor() : majorColor);
                    hihiAlarmLimitItem.setLineStyle(parentItem.getLineStyle());
                    hihiAlarmLimitItem.setLineWidth(3);
                    try {
                        model.addItem(hihiAlarmLimitItem);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to create alarm HIHI limit item for " + parentItem.getName(), e);
                    }
                    break;
            }
        }
        model.fireShowAlarmLimitsChanged();
    }

    private void hideAlarmLimitTraces(){
        if(loloAlarmLimitItem != null && loloAlarmLimitItem.isVisible()){
            model.removeItem(loloAlarmLimitItem);
            loloAlarmLimitItem = null;
        }
        if(lowAlarmLimitItem != null && lowAlarmLimitItem.isVisible()){
            model.removeItem(lowAlarmLimitItem);
            lowAlarmLimitItem = null;
        }
        if(highAlarmLimitItem != null && highAlarmLimitItem.isVisible()){
            model.removeItem(highAlarmLimitItem);
            highAlarmLimitItem = null;
        }
        if(hihiAlarmLimitItem != null && hihiAlarmLimitItem.isVisible()){
            model.removeItem(hihiAlarmLimitItem);
            hihiAlarmLimitItem = null;
        }
        model.fireShowAlarmLimitsChanged();
    }


    public void setLimitTracesVisible(boolean visible) {
        if(loloAlarmLimitItem != null) {
            loloAlarmLimitItem.visible = visible;
            loloAlarmLimitItem.model.ifPresent(m -> m.fireItemVisibilityChanged(loloAlarmLimitItem));
        }
        if(lowAlarmLimitItem != null) {
            lowAlarmLimitItem.visible = visible;
            lowAlarmLimitItem.model.ifPresent(m -> m.fireItemVisibilityChanged(lowAlarmLimitItem));
        }
        if(highAlarmLimitItem != null) {
            highAlarmLimitItem.visible = visible;
            highAlarmLimitItem.model.ifPresent(m -> m.fireItemVisibilityChanged(highAlarmLimitItem));
        }
        if(hihiAlarmLimitItem != null) {
            hihiAlarmLimitItem.visible = visible;
            hihiAlarmLimitItem.model.ifPresent(m -> m.fireItemVisibilityChanged(hihiAlarmLimitItem));
        }
    }
}
