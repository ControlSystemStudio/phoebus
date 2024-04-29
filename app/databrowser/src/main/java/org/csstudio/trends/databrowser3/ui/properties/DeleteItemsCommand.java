/*******************************************************************************
 * Copyright (c) 2010-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AnnotationInfo;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to delete items
 *  @author Kay Kasemir
 */
public class DeleteItemsCommand extends UndoableAction {
    final private Model model;
    final private List<ModelItem> items;
    final private Map<ModelItem, List<AnnotationInfo>> mapModelItemAnnotations;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be registered
     *  @param model Model were PV is to be added
     *  @param items Model items to delete
     */
    public DeleteItemsCommand(final UndoableActionManager operations_manager,
            final Model model, final List<ModelItem> items) {
        super(Messages.DeleteItem);
        this.model = model;
        // This list could be a reference to the model's list.
        // Since we will loop over this list, assert that there are no co-modification problems by creating a copy.
        this.items = new ArrayList<>(items);
        this.mapModelItemAnnotations = new HashMap<>();
        operations_manager.execute(this);
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        for (ModelItem item : items) {
            final List<AnnotationInfo> annotations = deleteAnnotations(item);

            model.removeItem(item);

            // Any changes?
            if (!annotations.equals(model.getAnnotations()))
                model.setAnnotations(annotations);
        }
    }

    private List<AnnotationInfo> deleteAnnotations(ModelItem item) {
        // check for annotations to remove because their item is deleted
        final int item_index = model.getItems().indexOf(item);
        final List<AnnotationInfo> deletedAnnotations = new ArrayList<>();
        final List<AnnotationInfo> annotations = new ArrayList<>();
        for (AnnotationInfo annotation : model.getAnnotations()) {
            if (annotation.getItemIndex() == item_index)
                deletedAnnotations.add(annotation);
            else
                annotations.add(annotation);
        }

        // possibly recalculate item index for remaining annotations
        if (!deletedAnnotations.isEmpty()
                && !annotations.isEmpty()
                && item_index < (model.getItems().size() - 1)) {
            for (int i=0; i<annotations.size(); i++) {
                AnnotationInfo ai = annotations.get(i);
                if (ai.getItemIndex() >= item_index) {
                    // annotation item index - 1, not item_index - 1
                    annotations.remove(i);
                    annotations.add(i, new AnnotationInfo(ai.isInternal(), ai.getItemIndex()-1, ai.getTime(), ai.getValue(), ai.getOffset(), ai.getText()));
                }
            }
        }

        // keep track of deleted annotations
        mapModelItemAnnotations.put(item, deletedAnnotations);

        // annotations after item has been deleted
        return annotations;
    }

    /** {@inheritDoc} */
    @Override
    public void undo() {
        for (ModelItem item : items) {
            try {
                model.addItem(item);

                final List<AnnotationInfo> annotations = restoreAnnotations(item);

                // Any changes?
                if (!annotations.equals(model.getAnnotations()))
                    model.setAnnotations(annotations);
            } catch (Exception ex) {
                ExceptionDetailsErrorDialog.openError(
                        Messages.Error,
                        MessageFormat.format(Messages.AddItemErrorFmt, item.getName()),
                        ex);
            }
        }
    }

    private List<AnnotationInfo> restoreAnnotations(ModelItem item) {
        final List<AnnotationInfo> annotations = new ArrayList<>(model.getAnnotations());
        final int item_index = model.getItems().indexOf(item);

        // check for annotations to restore because their item was restored
        for (AnnotationInfo ai : mapModelItemAnnotations.get(item))
            annotations.add(new AnnotationInfo(ai.isInternal(), item_index, ai.getTime(), ai.getValue(), ai.getOffset(), ai.getText()));

        // annotations after item has been restored
        return annotations;
    }

}
