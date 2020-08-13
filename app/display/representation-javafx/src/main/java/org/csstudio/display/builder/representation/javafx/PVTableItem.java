/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.csstudio.display.builder.model.properties.ScriptPV;
import org.phoebus.ui.autocomplete.AutocompleteMenu;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;

/**
 * Info class for property-based item table.
 *
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 7 Feb 2018
 */
public class PVTableItem {

    public static PVTableItem forPV ( final ScriptPV info ) {
        return new PVTableItem(info.getName(), info.isTrigger());
    }

    private final StringProperty  name    = new SimpleStringProperty();
    private final BooleanProperty trigger = new SimpleBooleanProperty(true);

    /**
     * @param name The PV name.
     * @param trigger {@code true} if the named PV will trigger evaluation.
     */
    public PVTableItem ( final String name, final boolean trigger ) {
        this.name.set(name);
        this.trigger.set(trigger);
    }

    public StringProperty nameProperty ( ) {
        return name;
    }

    public ScriptPV toScriptPV ( ) {
        return new ScriptPV(name.get(), trigger.get());
    }

    public BooleanProperty triggerProperty ( ) {
        return trigger;
    }

    /**
     * {@link PVTableItem} {@link TableCell} with {@link AutocompleteMenu}
     *
     * @author Amanda Carpenter
     */
    public static class AutoCompletedTableCell extends TableCell<PVTableItem, String> {

        private final Node             focusedOnCommit;
        private TextField              textField;

        public AutoCompletedTableCell (final Node focusedOnCommit ) {

            this.focusedOnCommit = focusedOnCommit;

            setAlignment(Pos.CENTER_LEFT);
            
            this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    textField.setText(getItem());
                    cancelEdit();
                    event.consume();
                }
            });
        }

        @Override
        public void cancelEdit ( ) {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void commitEdit ( String newValue ) {
            if (!isEditing() && !newValue.equals(getItem())) {
                TableView<PVTableItem> table = getTableView();
                if (table != null) {
                    TableColumn<PVTableItem, String> column = getTableColumn();
                    TableColumn.CellEditEvent<PVTableItem, String> event = new CellEditEvent<PVTableItem, String>(table,
                            new TablePosition<PVTableItem, String>(table, getIndex(), column),
                            TableColumn.editCommitEvent(), newValue);
                    Event.fireEvent(column, event);
                }
            }
            else {
                super.commitEdit(newValue);
            }
            Platform.runLater( ( ) -> focusedOnCommit.requestFocus());
        }

        @Override
        public void startEdit ( ) {

            if ( !isEmpty() ) {

                super.startEdit();

                createTextField();
                setText(null);
                setGraphic(textField);

                textField.selectAll();
            }

            Platform.runLater( ( ) -> textField.requestFocus());

        }

        @Override
        public void updateItem ( String item, boolean empty ) {

            super.updateItem(item, empty);

            if ( empty ) {
                setText(null);
                setGraphic(null);
            } else if ( isEditing() ) {

                if ( textField != null ) {
                    textField.setText(getItem() == null ? "" : getItem());
                }

                setText(null);
                setGraphic(textField);

            } else {

                setText(getItem() == null ? "" : getItem());
                setGraphic(null);
            }

        }

        private void createTextField ( ) {

            if ( textField == null ) {

                textField = new TextField(getItem() == null ? "" : getItem());

                textField.setOnAction(event -> commitEdit(textField.getText()));
                textField.focusedProperty().addListener( ( ob, o, n ) -> {
                    if ( !n ) {
                        commitEdit(textField.getText());
                    }
                });
                PVAutocompleteMenu.INSTANCE.attachField(textField);

            } else {
                textField.setText(getItem() == null ? "" : getItem());
            }

            textField.setMinWidth(getWidth() - getGraphicTextGap() * 2);

        }

    }

}
