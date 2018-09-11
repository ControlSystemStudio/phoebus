/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.data.PlotDataItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/** Dialog for adding annotation to a trace
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
public class AddAnnotationDialog<XTYPE extends Comparable<XTYPE>> extends Dialog<Boolean>
{
    private RTPlot<XTYPE> plot;
    // Thread-save snapshot of traces at time dialog was opened
    private final ObservableList<Trace<XTYPE>> traces = FXCollections.observableArrayList();
    private ListView<Trace<XTYPE>> trace_list;
    private TextArea text;

    /** Cell for Trace<> items that shows label and color */
    private class TraceCell extends ListCell<Trace<XTYPE>>
    {
		@Override
		protected void updateItem(Trace<XTYPE> item, boolean empty)
		{
			super.updateItem(item, empty);
			if (item == null)
				return;
			setText(item.getLabel());
			setTextFill(item.getColor());
		}
    };

    public AddAnnotationDialog(final RTPlot<XTYPE> plot)
    {
    	this.plot = plot;
    	for (Trace<XTYPE> trace : plot.getTraces())
    		traces.add(trace);

    	setTitle(Messages.AddAnnotation);
    	setHeaderText(Messages.AddAnnotation_Info);
        setResizable(true);

        final GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        content.setPadding(new Insets(10));

        Label label = new Label(Messages.AddAnnotation_Trace);
        trace_list = new ListView<>(traces);
        trace_list.setTooltip(new Tooltip(Messages.AddAnnotation_Trace_TT));
        trace_list.setPrefHeight(100);
        trace_list.setCellFactory(view -> new TraceCell());
        if (traces.size() > 0)
        	trace_list.getSelectionModel().select(0);
        content.add(label, 0, 0);
        content.add(trace_list, 1, 0);
        GridPane.setValignment(label, VPos.TOP);

        label = new Label(Messages.AddAnnotation_Content);
        text = new TextArea();
        text.setText(Messages.AddAnnotation_DefaultText);
        text.setTooltip(new Tooltip(Messages.AddAnnotation_Text_TT));
        text.setPrefHeight(100);
		content.add(label, 0, 1);
        content.add(text, 1, 1);
        GridPane.setValignment(label, VPos.TOP);

        final Label info = new Label(Messages.AddAnnotation_Content_Help);
        content.add(info, 0, 2, 2, 1);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Prevent closing dialog when input fails validation
        final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event ->
        {
        	if (!checkInput())
        		event.consume();
        });

        setResultConverter(button ->
        {
            // Release plot since dialog is held in memory for a while
            this.plot = null;
            traces.clear();
            return button == ButtonType.OK;
        });
    }

    private boolean checkInput()
    {
    	if (traces.isEmpty())
    	{
    		new Alert(AlertType.INFORMATION, Messages.AddAnnotation_NoTraces).showAndWait();
    		return false;
    	}
    	final MultipleSelectionModel<Trace<XTYPE>> seletion = trace_list.getSelectionModel();
    	final Trace<XTYPE> item = seletion.isEmpty() ? traces.get(0) : seletion.getSelectedItem();
    	final String content = text.getText().trim();
    	if (content.isEmpty())
    	{
    		new Alert(AlertType.WARNING, Messages.AddAnnotation_NoContent).showAndWait();
    		return false;
    	}
    	plot.addAnnotation(item, content);
        return true;
    }
}
