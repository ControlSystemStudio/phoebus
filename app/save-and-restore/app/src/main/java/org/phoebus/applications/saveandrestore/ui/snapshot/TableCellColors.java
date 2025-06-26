/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.javafx.Brightness;
import org.phoebus.ui.javafx.JFXUtil;

/**
 * Defines color resources, e.g. for the snapshot table view. In particular, for disconnected PVs we
 * want to use the same color as when indicating disconnected PV with a Display Builder widget border.
 */
public class TableCellColors {

    public static final Color DISCONNECTED_COLOR = Color.rgb(Preferences.undefined_severity_background_color[0],
            Preferences.undefined_severity_background_color[1],
            Preferences.undefined_severity_background_color[2]);

    public static final Paint DISCONNECTED_PAINT;
    private static final Color DISCONNECTED_TEXT_COLOR;
    private static final Color DISCONNECTED_BORDER_COLOR;
    public static final String REGULAR_CELL_STYLE = "-fx-text-fill: black;  -fx-background-color: transparent";
    public static final String DISCONNECTED_STYLE;
    public static final String DISCONNECTED_STYLE_SMALL;

    static {
        DISCONNECTED_PAINT = Paint.valueOf(JFXUtil.webRGB(DISCONNECTED_COLOR));
        if (Brightness.of(DISCONNECTED_COLOR) < Brightness.BRIGHT_THRESHOLD) {
            DISCONNECTED_TEXT_COLOR = Color.WHITE;
            DISCONNECTED_BORDER_COLOR = Color.WHITE;
        } else {
            DISCONNECTED_TEXT_COLOR = Color.BLACK;
            DISCONNECTED_BORDER_COLOR = Color.GRAY;
        }


        DISCONNECTED_STYLE = "-fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " +
                JFXUtil.webRGB(DISCONNECTED_TEXT_COLOR) +
                ";  -fx-background-color: " +
                JFXUtil.webRGB(DISCONNECTED_COLOR);
        DISCONNECTED_STYLE_SMALL = "-fx-border-color: " + JFXUtil.webRGB(DISCONNECTED_BORDER_COLOR) +"; -fx-border-width: 2 2 2 2;" +
                " -fx-background-color: " +
                JFXUtil.webRGB(DISCONNECTED_COLOR);
    }
}
