package org.phoebus.applications.alarm.ui.tree;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ValidatingTextFieldTableCell<S, T> extends TextFieldTableCell<S, T> {

    private TextField textField;

    public ValidatingTextFieldTableCell(StringConverter<T> converter) {
        super(converter);
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (isEditing() && getGraphic() instanceof TextField tf) {
            textField = tf;
            // add listener
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            	if (!isNowFocused && textField != null) {
                    T newValue = getConverter().fromString(textField.getText());
                    if (newValue.equals(getItem())) {
                        // nothing changed so cancel
                        cancelEdit();
                    } else {
                        // changed so validate
                        commitEdit(newValue);
                    }
                }
            });
        }
    }
    
    // utility method to simplify usage in column
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return forTableColumn(new javafx.util.converter.DefaultStringConverter());
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(StringConverter<T> converter) {
        return column -> new ValidatingTextFieldTableCell<>(converter);
    }
}
