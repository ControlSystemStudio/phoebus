/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/** Creates JavaFX item for unknown model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UnknownRepresentation extends JFXBaseRepresentation<Label, Widget>
{
    private static final NamedWidgetColor INVALID = WidgetColorService.getColor("INVALID");
    private static final Color COLOR = Color.rgb(INVALID.getRed(), INVALID.getGreen(), INVALID.getBlue(), INVALID.getAlpha() / 255.0);
    private static final Border BORDER = new Border(new BorderStroke(COLOR, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT, Insets.EMPTY));
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final UntypedWidgetPropertyListener sizeChangedListener = this::sizeChanged;

    @Override
    public Label createJFXNode() throws Exception
    {
        final Label label = new Label("<" + model_widget.getType() + ">");
        label.setBorder(BORDER);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(COLOR);
        label.setWrapText(true);
        return label;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        super.unregisterListeners();
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
            jfx_node.setPrefSize(model_widget.propWidth().getValue(),
                                 model_widget.propHeight().getValue());
    }
}
