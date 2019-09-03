/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 *
 * <code>MultitypeTableCell</code> is a table cell which is used for editing multiple types of values. Based on the
 * return value of the {@link #isTextFieldType()} the cell will display either a text field or a combo box. The combo
 * box will show the values provided to the {@link #setItems(List)} method.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 * @param <S> the type of table view generic
 * @param <T> the type of the item in this cell
 */
public class MultitypeTableCell<S, T> extends TableCell<S, T> {

    private TextField textField;
    private ComboBox<T> comboBox;
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
        "converter");
    private final ObservableList<T> items = FXCollections.observableArrayList();



    /**
     * Return true if this cell should display a text field or false if it should display the combo box. If the cell
     * should display a combo box, make sure that the method {@link #setItems(List)} has been called: either from this
     * method, or sooner, if the items are static.
     *
     * @return true for text field or false for combo box
     */
    public boolean isTextFieldType() {
        return true;
    }

    /**
     * Set the converter used for converting the value to string.
     *
     * @param converter the converter
     */
    public void setConverter(StringConverter<T> converter) {
        this.converter.set(converter == null ? CellUtils.<T> defaultStringConverter() : converter);
    }

    /**
     * Set the items that will be displayed in the combo box.
     *
     * @param newItems the items
     */
    public void setItems(List<T> newItems) {
        items.setAll(newItems);
    }

    /**
     * Returns the items that are currently set for the combo box.
     *
     * @return the items
     */
    public List<T> getItems() {
        return items;
    }

    /*
     * (non-Javadoc)
     *
     * @see javafx.scene.control.TableCell#startEdit()
     */
    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }
        if (isTextFieldType()) {
            super.startEdit();
            if (isEditing()) {
                if (textField == null) {
                    textField = CellUtils.createTextField(this, converter.get());
                }
                CellUtils.startEdit(this, converter.get(), null, null, textField);
            }
        } else {
            if (comboBox == null) {
                comboBox = CellUtils.createComboBox(this, items, converter);
                comboBox.editableProperty().set(false);
            }
            comboBox.getSelectionModel().select(getItem());
            super.startEdit();
            setText(null);
            setGraphic(comboBox);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javafx.scene.control.TableCell#cancelEdit()
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        if (isTextFieldType()) {
            CellUtils.cancelEdit(this, converter.get(), null);
        } else {
            setText(converter.get().toString(getItem()));
            setGraphic(null);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javafx.scene.control.Cell#updateItem(java.lang.Object, boolean)
     */
    @Override
    public void updateItem(T item, boolean empty) {
        boolean textFieldType = isTextFieldType();
        this.getStyleClass().remove("text-field-table-cell");
        this.getStyleClass().remove("combo-box-table-cell");
        if (textFieldType) {
            this.getStyleClass().add("text-field-table-cell");
        } else {
            this.getStyleClass().add("combo-box-table-cell");
        }
        super.updateItem(item, empty);
        if (textFieldType) {
            CellUtils.updateItem(this, converter.get(), null, null, textField);
        } else {
            CellUtils.updateItem(this, converter.get(), null, null, comboBox);
        }
    }
}
