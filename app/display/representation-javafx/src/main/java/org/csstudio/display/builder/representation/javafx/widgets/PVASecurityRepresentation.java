/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.PVASecurityWidget;
import org.csstudio.display.builder.model.widgets.PVASecurityWidget.PVASecurityMode;
import org.epics.vtype.VType;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

@SuppressWarnings("nls")
public class PVASecurityRepresentation extends JFXBaseRepresentation<HBox, PVASecurityWidget>
{
    private static final String LOCKED_ICON = "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z";
    private static final String UNLOCKED_ICON = "M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z";
    private static final String SERVER_ICON = "M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm13-6H4c-.55 0-1-.45-1-1V6c0-.55.45-1 1-1h16c.55 0 1 .45 1 1v6c0 .55-.45 1-1 1zM7 11c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z";
    private static final String PERSON_ICON = "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
    private static final String SHIELD_ICON = "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z";

    private static final Color TLS_ON = Color.web("#2e7d32");
    private static final Color TLS_OFF = Color.web("#9e9e9e");
    private static final Color SERVER = Color.web("#1565c0");
    private static final Color CLIENT = Color.web("#6a1b9a");
    private static final Color AUTH = Color.web("#e65100");

    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final WidgetPropertyListener<Integer> size_listener = this::sizeChanged;
    private final UntypedWidgetPropertyListener content_listener = this::contentChanged;

    private SVGPath icon;
    private Label text;

    @Override
    protected HBox createJFXNode() throws Exception
    {
        icon = new SVGPath();
        icon.setScaleX(0.67);
        icon.setScaleY(0.67);

        text = new Label();

        final HBox box = new HBox(6, icon, text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setManaged(false);
        return box;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addPropertyListener(size_listener);
        model_widget.propHeight().addPropertyListener(size_listener);
        model_widget.propDisplayMode().addUntypedPropertyListener(content_listener);
        model_widget.runtimePropTLSActive().addUntypedPropertyListener(content_listener);
        model_widget.runtimePropServerIdentity().addUntypedPropertyListener(content_listener);
        model_widget.runtimePropClientIdentity().addUntypedPropertyListener(content_listener);
        model_widget.runtimePropAuthMethod().addUntypedPropertyListener(content_listener);
        model_widget.runtimePropValue().addUntypedPropertyListener(content_listener);
        contentChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(size_listener);
        model_widget.propHeight().removePropertyListener(size_listener);
        model_widget.propDisplayMode().removePropertyListener(content_listener);
        model_widget.runtimePropTLSActive().removePropertyListener(content_listener);
        model_widget.runtimePropServerIdentity().removePropertyListener(content_listener);
        model_widget.runtimePropClientIdentity().removePropertyListener(content_listener);
        model_widget.runtimePropAuthMethod().removePropertyListener(content_listener);
        model_widget.runtimePropValue().removePropertyListener(content_listener);
        super.unregisterListeners();
    }

    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
            jfx_node.resize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());
        if (! dirty_content.checkAndClear())
            return;

        final PVASecurityMode mode = model_widget.propDisplayMode().getValue();
        switch (mode)
        {
        case SERVER_IDENTITY:
            icon.setContent(SERVER_ICON);
            icon.setFill(SERVER);
            text.setText(orDash(model_widget.runtimePropServerIdentity().getValue()));
            break;
        case CLIENT_IDENTITY:
            icon.setContent(PERSON_ICON);
            icon.setFill(CLIENT);
            text.setText(orDash(model_widget.runtimePropClientIdentity().getValue()));
            break;
        case AUTH_METHOD:
            icon.setContent(SHIELD_ICON);
            icon.setFill(AUTH);
            text.setText(orDash(model_widget.runtimePropAuthMethod().getValue()));
            break;
        case TLS_STATUS:
        default:
            final VType value = model_widget.runtimePropValue().getValue();
            if (value == null)
            {
                icon.setContent(UNLOCKED_ICON);
                icon.setFill(TLS_OFF);
                text.setText("Disconnected");
            }
            else if (model_widget.runtimePropTLSActive().getValue())
            {
                icon.setContent(LOCKED_ICON);
                icon.setFill(TLS_ON);
                text.setText("TLS Secured");
            }
            else
            {
                icon.setContent(UNLOCKED_ICON);
                icon.setFill(TLS_OFF);
                text.setText("Not Secured");
            }
            break;
        }
    }

    private String orDash(final String text)
    {
        if (text == null || text.isBlank())
            return "—";
        return text;
    }
}
