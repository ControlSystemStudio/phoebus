/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.console;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.util.RingBuffer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Console UI: Text area and input section
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Console
{
    /** Logger for all Terminal code */
    public static final Logger logger = Logger.getLogger(Console.class.getPackageName());

    private static final int output_line_limit;
    private static final int history_size;
    private static final String font_name;
    private static final int font_size;
    private static final String prompt;
    private static final String prompt_info;
    static final String shell;
    static final File directory;

    /** Preferences */
    static
    {
        PreferencesReader prefs = new PreferencesReader(Console.class, "/console_preferences.properties");
        output_line_limit = prefs.getInt("output_line_limit");
        history_size = prefs.getInt("history_size");
        font_name = prefs.get("font_name");
        font_size = prefs.getInt("font_size");
        prompt = prefs.get("prompt");
        prompt_info = prefs.get("prompt_info");
        shell = prefs.get("shell");
        directory = new File(prefs.get("directory"));
    }

    public enum LineType
    {
        NORMAL,
        OUTPUT,
        ERROR
    };

    private static final Background background = new Background(new BackgroundFill(Color.LIGHTGRAY.brighter(), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Font console_font = Font.font(font_name, font_size);

    private final TextFlow output = new TextFlow();
    private final Label prompt_lbl = new Label(prompt);
    private final TextField input = new TextField();
    private VBox layout;
    private ScrollPane scroll;

    private volatile boolean auto_scroll = true;

    private final RingBuffer<String> history = new RingBuffer<>(history_size);
    private int history_position = 0;

    public Console()
    {
        // Create layout:
        // +---------------+
        // |     output    |
        // +---------------+
        // | prompt: input |
        // +---------------+
        output.setLineSpacing(5.0);
        output.setBackground(background);

        input.setPromptText(prompt_info);
        HBox.setHgrow(input, Priority.ALWAYS);
        final HBox promptrow = new HBox(prompt_lbl, input);
        promptrow.setAlignment(Pos.CENTER_LEFT);

        scroll = new ScrollPane(output);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        // Scroll pane grows as large as possible.
        // If output is larger than ScrollPane, viewport scrolls just fine.
        // But when output is smaller than viewport, should fill it:
        // https://reportmill.wordpress.com/2014/06/03/make-scrollpane-content-fill-viewport-bounds/
        scroll.viewportBoundsProperty().addListener((p, o, bounds) ->
        {
            scroll.setFitToWidth( output.prefWidth(-1)  < bounds.getWidth() );
            scroll.setFitToHeight(output.prefHeight(-1) < bounds.getHeight());
        });

        layout = new VBox(5.0, scroll, promptrow);
        layout.setPadding(new Insets(5.0));

        // Behavior:
        // Scroll to bottom when content changes
        output.heightProperty().addListener(prop ->
        {
            if (auto_scroll)
                scroll.setVvalue(1.0);
        });

        // Handle keys
        input.setOnKeyPressed(this::handleKeypressed);
    }

    private void handleKeypressed(final KeyEvent event)
    {
        // Cursor up/down selects items from input history
        switch (event.getCode())
        {
        case UP:
            if (history_position <= 0  ||  history.isEmpty())
                return;
            --history_position;
            input.setText(history.get(history_position));
            input.positionCaret(input.getText().length());
            break;
        case DOWN:
            if (history_position >= history.size() - 1)
                return;
            ++history_position;
            input.setText(history.get(history_position));
            input.positionCaret(input.getText().length());
            break;
        default:
            break;
        }
    }

    /** @param on_input Handler to call when user enters new line of input */
    public void setOnInput(final Consumer<String> on_input)
    {
        input.setOnAction(event ->
        {
            final String line = input.getText();
            input.clear();

            // Add entered line to history and rest history position
            history.add(line);
            history_position = history.size();

            // Show entered text
            addLine(prompt_lbl.getText() + line, LineType.NORMAL);

            // Invoke handler
            on_input.accept(line);
        });
    }

    /** @return Top-level node */
    public Parent getNode()
    {
        return layout;
    }

    /** @param line Line to show in output
     *  @param type How to represent it
     */
    public void addLine(final String line, final LineType type)
    {
        final Text formatted_line = new Text(line + "\n");
        formatted_line.setFont(console_font);
        if (type == LineType.OUTPUT)
            formatted_line.setFill(Color.BLUE);
        else if (type == LineType.ERROR)
            formatted_line.setFill(Color.DARKRED);

        Platform.runLater(() ->
        {
            if (output.getChildren().size() > output_line_limit)
                output.getChildren().remove(0);
            output.getChildren().add(formatted_line);
        });
    }

    /** Disable user input */
    public void disable()
    {
        input.setText("Process Exited");
        input.setDisable(true);
    }
}
