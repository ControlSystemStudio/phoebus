/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.time.Instant;

import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Menu item to duplicate a file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertiesAction extends MenuItem
{
    private static class FilePropertiesDialog extends Dialog<Void>
    {
        private final File file;
        private final CheckBox writable = new CheckBox(Messages.PropDlgWritable);
        private final CheckBox executable = new CheckBox(Messages.PropDlgExecutable);

        public FilePropertiesDialog(final File file)
        {
            this.file = file;
            setTitle(Messages.PropDlgTitle);
            setResizable(true);

            getDialogPane().setContent(createContent());
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
            ok.addEventFilter(ActionEvent.ACTION, event ->
            {
                if (! updateFile())
                    event.consume();
            });

            setResultConverter(button -> null);
        }

        private Node createContent()
        {
            final GridPane layout = new GridPane();
            layout.setHgap(5);
            layout.setVgap(5);

            int row = 0;

            final TextField filename = new TextField(file.getAbsolutePath());
            filename.setEditable(false);
            GridPane.setHgrow(filename, Priority.ALWAYS);
            layout.add(new Label(Messages.PropDlgPath), 0, row);
            layout.add(filename, 1, row);

            final TextField date = new TextField(TimestampFormats.MILLI_FORMAT.format(Instant.ofEpochMilli(file.lastModified())));
            date.setEditable(false);
            date.setMinWidth(200);
            GridPane.setHgrow(date, Priority.ALWAYS);
            layout.add(new Label(Messages.PropDlgDate), 0, ++row);
            layout.add(date, 1, row);

            if (!file.isDirectory())
            {
                final TextField size = new TextField(file.length() + " " + Messages.PropDlgBytes);
                size.setEditable(false);
                GridPane.setHgrow(size, Priority.ALWAYS);
                layout.add(new Label(Messages.PropDlgSize), 0, ++row);
                layout.add(size, 1, row);
            }

            layout.add(new Label(Messages.PropDlgPermissions), 0, ++row);
            layout.add(writable, 1, row);
            layout.add(executable, 1, ++row);

            writable.setSelected(file.canWrite());
            executable.setSelected(file.canExecute());

            return layout;
        }

        public boolean updateFile()
        {
            file.setExecutable(executable.isSelected());
            file.setWritable(writable.isSelected());
            return true;
        }
    };

    /** @param node Node used to position confirmation dialog
     *  @param item Item to duplicate
     */
    public PropertiesAction(final Node node, final TreeItem<FileInfo> item)
    {
        super(Messages.Properties, ImageCache.getImageView(ImageCache.class, "/icons/info.png"));

        setOnAction(event ->
        {
            final FilePropertiesDialog dialog = new FilePropertiesDialog(item.getValue().file);
            DialogHelper.positionDialog(dialog, node, 0, 0);
            dialog.showAndWait();
        });
    }
}
