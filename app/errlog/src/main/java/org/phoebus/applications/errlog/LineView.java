package org.phoebus.applications.errlog;

import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;

/** JavaFX view of lines
 *
 *  <p>Scrollable, length-limited list of 'stdout' or 'stderr' messages.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class LineView
{
    /** One 'Line': Text and flag if it's error message */
    private static class Line
    {
        final String text;
        final boolean error;

        Line(final String text, final boolean error)
        {
            this.text = text;
            this.error = error;
        }
    };

    /** List cell for coloring {@link Line} */
    private static class LineCell extends ListCell<Line>
    {
        @Override
        protected void updateItem(final Line item, final boolean empty)
        {
            super.updateItem(item, empty);
            if (empty || item == null)
                setText("");
            else
            {
                setText(item.text);
                if (item.error)
                    setTextFill(Color.DARKRED);
                else
                    setTextFill(Color.BLACK);
            }
        }
    }

    private final ListView<Line> list = new ListView<>();

    public LineView()
    {
        list.setCellFactory(view -> new LineCell());
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addContextMenu();
    }

    /** @return Top-level node */
    public Control getControl()
    {
        return list;
    }

    private void addContextMenu()
    {
        final MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> copy());

        final MenuItem clear = new MenuItem("Clear");
        clear.setOnAction(event -> clear());

        final ContextMenu menu = new ContextMenu(copy, clear);
        list.setContextMenu(menu);
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
        final Line formatted_line = new Line(line, error);
        final ObservableList<Line> items = list.getItems();

        Platform.runLater(() ->
        {
            if (items.size() > Preferences.max_lines)
                items.remove(0);
            items.add(formatted_line);
            list.scrollTo(items.size()-1);
        });
    }

    /** Copy content to clipboard */
    private void copy()
    {
        // Use selected lines
        String lines = list.getSelectionModel()
                           .getSelectedItems()
                           .stream()
                           .map(line -> line.text)
                           .collect(Collectors.joining("\n"));
        // If nothing selected, use all lines
        if (lines.isEmpty())
            lines = list.getItems()
                        .stream()
                        .map(line -> line.text)
                        .collect(Collectors.joining("\n"));

        final ClipboardContent content = new ClipboardContent();
        content.putString(lines);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /** Remove all content */
    private void clear()
    {
        list.getItems().clear();
    }
}