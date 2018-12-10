/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import org.csstudio.javafx.rtplot.Annotation;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.undo.RemoveAnnotationAction;
import org.csstudio.javafx.rtplot.internal.undo.UpdateAnnotationTextAction;
import org.phoebus.ui.dialog.MultiLineInputDialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Dialog for editing or removing annotations
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EditAnnotationDialog<XTYPE extends Comparable<XTYPE>> extends Dialog<Boolean>
{
	private class AnnotationItem
	{
		boolean selected = true;
		final Annotation<XTYPE> annotation;
		AnnotationItem(final Annotation<XTYPE> annotation)
		{
			this.annotation = annotation;
		}
	}

	private RTPlot<XTYPE> plot;
    private final ObservableList<AnnotationItem> annotations = FXCollections.observableArrayList();
    private ListView<AnnotationItem> annotation_list;

    private class AnnotationCell extends ListCell<AnnotationItem>
    {
		private static final int MAX_LENGTH = 30;

		@Override
		protected void updateItem(AnnotationItem item, boolean empty)
		{
			super.updateItem(item, empty);
			if (item == null)
				return;
			String text = item.annotation.getTrace().getLabel() + ": " + item.annotation.getText().replaceAll("\n", "\\\\n");
			if (text.length() > MAX_LENGTH)
				text = text.substring(0, MAX_LENGTH) + "...";
			final CheckBox selector = new CheckBox(text);
			selector.setTextFill(item.annotation.getTrace().getColor());
			selector.setSelected(true);
			final Button edit = new Button(Messages.AnnotationEditBtn);
			final BorderPane line = new BorderPane();
			line.setLeft(selector);
			line.setRight(edit);
			setGraphic(line); // 'Graphic' == any Node that represents the cell

			selector.setOnAction(event -> item.selected = selector.isSelected());
			edit.setOnAction(event -> editAnnotation(item.annotation));
		}
    };

    public EditAnnotationDialog(final RTPlot<XTYPE> plot)
    {
    	setTitle(Messages.EditAnnotation);
    	setResizable(true);

    	this.plot = plot;

        for (Annotation<XTYPE> annotation : plot.getAnnotations())
            if (! annotation.isInternal())
                annotations.add(new AnnotationItem(annotation));

        annotation_list = new ListView<>(annotations);
        annotation_list.setCellFactory(view -> new AnnotationCell());
        annotation_list.setPrefWidth(500);
        annotation_list.setPrefHeight(200);

        final Label info = new Label(Messages.EditAnnotation_Info);

        getDialogPane().setContent(new VBox(10.0, annotation_list, info));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button ->
        {
            try
            {
            	if (button == ButtonType.OK)
            	{
            		updateAnnotations();
            		return true;
            	}
            	return false;
            }
            finally
            {
                // Release plot since dialog is held in memory for a while
                this.plot = null;
                annotations.clear();
            }
        });
    }

    private void editAnnotation(final Annotation<XTYPE> annotation)
    {
        final MultiLineInputDialog dialog = new MultiLineInputDialog(annotation.getText());
        dialog.setTitle(Messages.AnnotationEditTitle);
        dialog.setHeaderText(Messages.AnnotationEditHdr);
        dialog.showAndWait().ifPresent(new_text ->
        {
        	plot.getUndoableActionManager().execute(
        			new UpdateAnnotationTextAction<>(plot, annotation, new_text));
        	// Update annotation_list by causing fake update of item in observed list
        	for (int i=0; i<annotations.size(); ++i)
        	    if (annotations.get(i).annotation == annotation)
        	    {
        	        annotations.set(i, annotations.get(i));
        	        break;
        	    }
        });
    }

    private void updateAnnotations()
    {
    	for (AnnotationItem item : annotations)
    		if (! item.selected)
	            plot.getUndoableActionManager().execute(
	            		new RemoveAnnotationAction<>(plot, item.annotation));
    }
}
