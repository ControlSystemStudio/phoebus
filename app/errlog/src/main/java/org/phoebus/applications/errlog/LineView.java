package org.phoebus.applications.errlog;

import javafx.application.Platform;
/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** JavaFX view of lines
 *
 *  <p>Scrollable, length-limited list of 'stdout' or 'stderr' messages.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class LineView
{
    private static final Background background = new Background(new BackgroundFill(Color.LIGHTGRAY.brighter(), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Font console_font = Font.font(Preferences.font_name, Preferences.font_size);

    private final TextFlow lines = new TextFlow();
    private ScrollPane scroll;

    public LineView()
    {
        lines.setLineSpacing(5.0);
        lines.setBackground(background);

        scroll = new ScrollPane(lines);
        // Scroll pane grows as large as possible.
        // If output is larger than ScrollPane, viewport scrolls just fine.
        // But when output is smaller than viewport, should fill it:
        // https://reportmill.wordpress.com/2014/06/03/make-scrollpane-content-fill-viewport-bounds/
        scroll.viewportBoundsProperty().addListener((p, o, bounds) ->
        {
            scroll.setFitToWidth( lines.prefWidth(-1)  < bounds.getWidth() );
            scroll.setFitToHeight(lines.prefHeight(-1) < bounds.getHeight());
        });

        // Scroll to bottom when content changes
        lines.heightProperty().addListener(prop ->
        {
            // if (auto_scroll)
                scroll.setVvalue(1.0);
        });

        addContextMenu();
    }

    /** @return Top-level node */
    public Control getControl()
    {
        return scroll;
    }

    private void addContextMenu()
    {
        final MenuItem clear = new MenuItem("Clear");
        clear.setOnAction(event -> clear());
        final ContextMenu menu = new ContextMenu(clear);
        scroll.setContextMenu(menu);
    }

    /** Add message
     *
     *  <p>Safe to call from non-UI thread
     *
     *  @param line Line to add
     *  @param error Highlight as error message?
     */
    public void addLine(final String line, final boolean error)
    {
        final Text formatted_line = new Text(line + "\n");
        formatted_line.setFont(console_font);
        if (error)
            formatted_line.setFill(Color.DARKRED);

        Platform.runLater(() ->
        {
            if (lines.getChildren().size() > Preferences.max_lines)
                lines.getChildren().remove(0);
            lines.getChildren().add(formatted_line);
        });
    }

    /** Remove all content */
    private void clear()
    {
        lines.getChildren().clear();
    }
}