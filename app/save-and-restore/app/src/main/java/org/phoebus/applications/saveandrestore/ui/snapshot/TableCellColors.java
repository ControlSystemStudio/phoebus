/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.scene.paint.Color;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.javafx.Brightness;
import org.phoebus.ui.javafx.JFXUtil;

/**
 * Defines color resources, e.g. for the snapshot table view. In particular, for disconnected PVs we
 * want to use the same color as when indicating disconnected PV with a Display Builder widget border.
 */
public class TableCellColors {

    private static final Color DISCONNECTED_COLOR = Color.rgb(Preferences.undefined_severity_background_color[0],
            Preferences.undefined_severity_background_color[1],
            Preferences.undefined_severity_background_color[2]);
    private static final Color ALARM_NONE_COLOR = Color.rgb(Preferences.alarm_area_panel_ok_severity_background_color[0],
            Preferences.alarm_area_panel_ok_severity_background_color[1],
            Preferences.alarm_area_panel_ok_severity_background_color[2]);
    private static final Color ALARM_MINOR_COLOR = Color.rgb(Preferences.alarm_area_panel_minor_severity_background_color[0],
            Preferences.alarm_area_panel_minor_severity_background_color[1],
            Preferences.alarm_area_panel_minor_severity_background_color[2]);
    private static final Color ALARM_MAJOR_COLOR = Color.rgb(Preferences.alarm_area_panel_major_severity_background_color[0],
            Preferences.alarm_area_panel_major_severity_background_color[1],
            Preferences.alarm_area_panel_major_severity_background_color[2]);
    private static final Color ALARM_UNDEFINED_COLOR = Color.rgb(Preferences.alarm_area_panel_undefined_severity_background_color[0],
            Preferences.alarm_area_panel_major_severity_background_color[1],
            Preferences.alarm_area_panel_major_severity_background_color[2]);
    private static final Color ALARM_INVALID_COLOR = Color.rgb(Preferences.alarm_area_panel_invalid_severity_background_color[0],
            Preferences.alarm_area_panel_invalid_severity_background_color[1],
            Preferences.alarm_area_panel_invalid_severity_background_color[2]);

    private static final Color DISCONNECTED_TEXT_COLOR;
    private static final Color ALARM_NONE_TEXT_COLOR;
    private static final Color ALARM_MINOR_TEXT_COLOR;
    private static final Color ALARM_MAJOR_TEXT_COLOR;
    private static final Color ALARM_UNDEFINED_TEXT_COLOR;
    private static final Color ALARM_INVALID_TEXT_COLOR;
    public static final String REGULAR_CELL_STYLE = "-fx-text-fill: black;  -fx-background-color: transparent";
    public static final String DISCONNECTED_STYLE;
    public static final String ALARM_NONE_STYLE;
    public static final String ALARM_MINOR_STYLE;
    public static final String ALARM_MAJOR_STYLE;
    public static final String ALARM_UNDEFINED_STYLE;
    public static final String ALARM_INVALID_STYLE;

    static {
        if (Brightness.of(DISCONNECTED_COLOR) < Brightness.BRIGHT_THRESHOLD) {
            DISCONNECTED_TEXT_COLOR = Color.WHITE;
        }
        else {
            DISCONNECTED_TEXT_COLOR = Color.BLACK;
        }

        if(Brightness.of(ALARM_NONE_COLOR) < Brightness.BRIGHT_THRESHOLD){
            ALARM_NONE_TEXT_COLOR = Color.WHITE;
        }
        else{
            ALARM_NONE_TEXT_COLOR = Color.BLACK;
        }

        if(Brightness.of(ALARM_MINOR_COLOR) < Brightness.BRIGHT_THRESHOLD){
            ALARM_MINOR_TEXT_COLOR = Color.WHITE;
        }
        else{
            ALARM_MINOR_TEXT_COLOR = Color.BLACK;
        }

        if(Brightness.of(ALARM_MAJOR_COLOR) < Brightness.BRIGHT_THRESHOLD){
            ALARM_MAJOR_TEXT_COLOR = Color.WHITE;
        }
        else{
            ALARM_MAJOR_TEXT_COLOR = Color.BLACK;
        }

        if(Brightness.of(ALARM_UNDEFINED_COLOR) < Brightness.BRIGHT_THRESHOLD){
            ALARM_UNDEFINED_TEXT_COLOR = Color.WHITE;
        }
        else{
            ALARM_UNDEFINED_TEXT_COLOR = Color.BLACK;
        }

        if(Brightness.of(ALARM_INVALID_COLOR) < Brightness.BRIGHT_THRESHOLD){
            ALARM_INVALID_TEXT_COLOR = Color.WHITE;
        }
        else{
            ALARM_INVALID_TEXT_COLOR = Color.BLACK;
        }


        DISCONNECTED_STYLE = "-fx-background-insets: 2 2 2 2; -fx-text-fill: " +
                JFXUtil.webRGB(DISCONNECTED_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(DISCONNECTED_COLOR);

        ALARM_NONE_STYLE = "-fx-background-insets: 2 2 2 2; -fx-text-fill: " +
                JFXUtil.webRGB(ALARM_NONE_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(ALARM_NONE_COLOR);

        ALARM_MINOR_STYLE = "-fx-background-insets: 2 2 2 2; -fx-text-fill: " +
                JFXUtil.webRGB(ALARM_MINOR_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(ALARM_MINOR_COLOR);

        ALARM_MAJOR_STYLE = "-fx-background-insets: 2 2 2 2; -fx-text-fill: " +
                JFXUtil.webRGB(ALARM_MAJOR_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(ALARM_MAJOR_COLOR);

        ALARM_UNDEFINED_STYLE = "-fx-background-insets: 2 2 2 2; -fx-text-fill: " +
                JFXUtil.webRGB(ALARM_UNDEFINED_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(ALARM_UNDEFINED_COLOR);

        ALARM_INVALID_STYLE = "-fx-background-insets: 2 2 2 2; -fx-text-fill: " +
                JFXUtil.webRGB(ALARM_INVALID_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(ALARM_INVALID_COLOR);
    }
}
