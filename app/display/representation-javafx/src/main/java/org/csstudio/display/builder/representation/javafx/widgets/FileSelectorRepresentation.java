/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.io.File;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.FileSelectorWidget;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Styles;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/** JFX Representation for {@link FileSelectorWidget}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FileSelectorRepresentation extends JFXBaseRepresentation<Button, FileSelectorWidget>
{
    private static final Image open_file_icon = ImageCache.getImage(FileSelectorRepresentation.class, "/icons/open_file.png");

    /** Globally maintain the last directory that was selected.
     *
     *  <p>Next time a dialog opens, it will use this directory
     *  unless the initial value contains a directory path.
     */
    private static File last_directory = null;

    private final DirtyFlag dirty_size = new DirtyFlag();
    private final WidgetPropertyListener<Integer> sizeChangedListener = this::sizeChanged;

    private final DirtyFlag dirty_config = new DirtyFlag();
    private final UntypedWidgetPropertyListener configChangedListener = this::configChanged;

    @Override
    protected Button createJFXNode() throws Exception
    {
        final Button button = new Button();

        if (open_file_icon != null)
            button.setGraphic(new ImageView(open_file_icon));

        if (! toolkit.isEditMode())
            button.setOnAction(event -> selectFile());

        return button;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addPropertyListener(sizeChangedListener);
        model_widget.propHeight().addPropertyListener(sizeChangedListener);
        model_widget.propEnabled().addUntypedPropertyListener(configChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propEnabled().removePropertyListener(configChangedListener);
        super.unregisterListeners();
    }

    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void configChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_config.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
            jfx_node.setPrefSize(model_widget.propWidth().getValue(),
                                 model_widget.propHeight().getValue());
        if (dirty_config.checkAndClear())
        {
            if (toolkit.isEditMode())
                Styles.update(jfx_node, Styles.NOT_ENABLED, ! model_widget.propEnabled().getValue());
            else
                jfx_node.setDisable(! model_widget.propEnabled().getValue());
        }
    }

    private void selectFile()
    {
        final FileSelectorWidget.FileComponent component = model_widget.propComponent().getValue();

        String path = VTypeUtil.getValueString(model_widget.runtimePropValue().getValue(), false);

        File file = path == null ? null : new File(path);

        // Prompt for just dialog, or use general file selector?
        if (component == FileSelectorWidget.FileComponent.DIRECTORY)
        {
            final DirectoryChooser dialog = new DirectoryChooser();
            dialog.setTitle("Select Directory");
            if (null != file.getParentFile() && file.getParentFile().exists())
                dialog.setInitialDirectory(file.getParentFile());
            else if (last_directory != null)
                dialog.setInitialDirectory(last_directory);
            file = dialog.showDialog(jfx_node.getScene().getWindow());
        }
        else
        {
            final FileChooser dialog = new FileChooser();
            dialog.setTitle("Select File");
            if (null != file.getParentFile() && file.getParentFile().exists())
                dialog.setInitialDirectory(file.getParentFile());
            else if (last_directory != null)
                dialog.setInitialDirectory(last_directory);
            // Try to already select the initial file,
            // but at least on Linux that seems to have no effect
            if (file != null)
                dialog.setInitialFileName(file.getName());
            file = dialog.showOpenDialog(jfx_node.getScene().getWindow());
        }

        if (file == null)
            return;

        if (file.isDirectory())
            last_directory = file;
        else
            last_directory = file.getParentFile();

        switch (model_widget.propComponent().getValue())
        {
        case FULL:
            path = file.getAbsolutePath();
            break;
        case DIRECTORY:
            path = last_directory.getAbsolutePath();
            break;
        case FULLNAME:
            path = file.getName();
            break;
        case BASENAME:
            path = file.getName();
            final int sep = path.lastIndexOf('.');
            if (sep >= 0)
                path = path.substring(0, sep);
            break;
        }

        toolkit.fireWrite(model_widget, path);
    }
}
