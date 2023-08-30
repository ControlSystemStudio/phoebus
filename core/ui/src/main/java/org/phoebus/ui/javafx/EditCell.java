/*
 * Copied from https://gist.github.com/james-d/be5bbd6255a4640a5357. The "project" does not mention license terms.
 *
 * Modifications by European Spallation Source.
 *
 */
package org.phoebus.ui.javafx;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class EditCell<S, T> extends TableCell<S, T> {
    private final TextField textField = new TextField();

    // Converter for converting the text in the text field to the user type, and vice-versa:
    private final StringConverter<T> converter;

    /**
     * Creates and initializes an edit cell object.
     *
     * @param converter
     *            the converter to convert from and to strings
     */
    public EditCell(StringConverter<T> converter) {
        this.converter = converter;

        itemProperty().addListener((o, b, n) -> {
            setText(n != null ? this.converter.toString(n) : null);
        });

        setGraphic(this.textField);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        setAlignment(Pos.CENTER_LEFT);

        this.textField.setOnAction(evt -> {
            commitEdit(this.converter.fromString(this.textField.getText()));
        });

        this.textField.focusedProperty().addListener((o, b, n) -> {
            if (!n) {
                commitEdit(this.converter.fromString(this.textField.getText()));
            }
        });
        
        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                textField.setText(converter.toString(getItem()));
                cancelEdit();
                event.consume();
            }
        });
    }

    /**
     * Convenience converter that does nothing (converts Strings to themselves and vice-versa...).
     */
    public static final StringConverter<String> IDENTITY_CONVERTER = new StringConverter<String>() {

        @Override
        public String toString(String object) {
            return object;
        }

        @Override
        public String fromString(String string) {
            return string;
        }
    };

    /**
     * Convenience method for creating an EditCell for a String value.
     *
     * @return the edit cell
     */
    public static <S> EditCell<S, String> createStringEditCell() {
        return new EditCell<S, String>(IDENTITY_CONVERTER);
    }

    // set the text of the text field and display the graphic
    @Override
    public void startEdit() {
        super.startEdit();
        this.textField.setText(this.converter.toString(getItem()));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        this.textField.requestFocus();
    }

    // revert to text display
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    // commits the edit. Update property if possible and revert to text display
    @Override
    public void commitEdit(T item) {
        // This block is necessary to support commit on losing focus, because the baked-in mechanism
        // sets our editing state to false before we can intercept the loss of focus.
        // The default commitEdit(...) method simply bails if we are not editing...

        if (!isEditing() && !item.equals(getItem())) {
            TableView<S> table = getTableView();
            if (table != null) {
                TableColumn<S, T> column = getTableColumn();
                CellEditEvent<S, T> event = new CellEditEvent<S, T>(table,
                        new TablePosition<S, T>(table, getIndex(), column),
                        TableColumn.editCommitEvent(), item);
                Event.fireEvent(column, event);
            }
        }
        else {
            super.commitEdit(item);
        }

        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }
}