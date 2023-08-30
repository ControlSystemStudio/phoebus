/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx.widgets;


import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.widgets.TextSymbolWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;


/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 3 Aug 2017
 */
public class TextSymbolRepresentation extends RegionBaseRepresentation<Label, TextSymbolWidget> {

    private int                                  arrayIndex             = 0;
    private final DirtyFlag                      dirtyContent           = new DirtyFlag();
    private final DirtyFlag                      dirtyGeometry          = new DirtyFlag();
    private final DirtyFlag                      dirtyStyle             = new DirtyFlag();
    private final DirtyFlag                      dirtyValue             = new DirtyFlag();
    private final UntypedWidgetPropertyListener contentListener = this::contentChanged;
    private final UntypedWidgetPropertyListener geometryListener = this::geometryChanged;
    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final WidgetPropertyListener<List<WidgetProperty<String>>> symbolsListener = this::symbolsChanged;
    private final WidgetPropertyListener<VType> valueListener = this::valueChanged;
    private volatile boolean                     enabled                = true;
    private int                                  symbolIndex            = -1;
    private final WidgetPropertyListener<String> symbolPropertyListener = this::symbolChanged;
    private final AtomicBoolean                  updatingValue          = new AtomicBoolean(false);
    /**
     * Was there ever any transformation applied to the jfx_node?
     *  <p>Used to optimize:
     *  If there never was a rotation, don't even _clear()_ it
     *  to keep the Node's nodeTransformation == null
     */
    private boolean                              was_ever_transformed   = false;

    @Override
    public void updateChanges ( ) {

        super.updateChanges();

        Object value;

        if ( dirtyGeometry.checkAndClear() ) {

            value = model_widget.propVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isVisible()) ) {
                jfx_node.setVisible((boolean) value);
            }

            jfx_node.resize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());

        }

        if ( dirtyContent.checkAndClear() ) {

            value = model_widget.propArrayIndex().getValue();

            if ( !Objects.equals(value, arrayIndex) ) {
                arrayIndex = Math.max(0, (int) value);
            }

            symbolIndex = Math.min(Math.max(symbolIndex, 0), model_widget.propSymbols().size() - 1);

            final String symbol_value = ( symbolIndex >= 0 ) ? model_widget.propSymbols().getElement(symbolIndex).getValue() : "\u263A";
            model_widget.runtimePropSymbolValue().setValue(symbol_value);
            jfx_node.setText(symbol_value);

        }

        if ( dirtyStyle.checkAndClear() ) {

            final int width = model_widget.propWidth().getValue();
            final int height = model_widget.propHeight().getValue();
            final RotationStep rotation = model_widget.propRotationStep().getValue();
            final int w, h;

            switch ( rotation ) {
                case NINETY:
                    w = height; h = width;
                    jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()), new Translate(-height, 0));
                    was_ever_transformed = true;
                    break;
                case ONEEIGHTY:
                    w = width; h = height;
                    jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()), new Translate(-width, -height));
                    was_ever_transformed = true;
                    break;
                case MINUS_NINETY:
                    w = height; h = width;
                    jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()), new Translate(0, -width));
                    was_ever_transformed = true;
                    break;
                case NONE:
                default:
                    w = width; h = height;
                    if ( was_ever_transformed ) {
                        jfx_node.getTransforms().clear();
                    }
                    break;
            }

            jfx_node.resize(w, h);
            value = model_widget.propEnabled().getValue();

            if ( !Objects.equals(value, enabled) ) {

                enabled = (boolean) value;

                Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);

            }

            jfx_node.setAlignment(JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(), model_widget.propVerticalAlignment().getValue()));
            jfx_node.setBackground(model_widget.propTransparent().getValue()
                ? null
                : new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY))
            );
            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            jfx_node.setTextFill(JFXUtil.convert(model_widget.propForegroundColor().getValue()));
            jfx_node.setWrapText(model_widget.propWrapWords().getValue());

        }

        if ( dirtyValue.checkAndClear() && updatingValue.compareAndSet(false, true) ) {

            try {

                value = model_widget.runtimePropValue().getValue();

                if ( value != null ) {
                    if ( value instanceof VBoolean ) {
                        symbolIndex = ((VBoolean) value).getValue() ? 1 : 0;
                    } else if ( value instanceof VString ) {
                        try {
                            symbolIndex = Integer.parseInt(((VString) value).getValue());
                        } catch ( NumberFormatException nfex ) {
                            logger.log(Level.FINE, "Failure parsing the string value: {0} [{1}].", new Object[] { ((VString) value).getValue(), nfex.getMessage() });
                        }
                    } else if ( value instanceof VNumber ) {
                        symbolIndex = ((VNumber) value).getValue().intValue();
                    } else if ( value instanceof VEnum ) {
                        symbolIndex = ((VEnum) value).getIndex();
                    } else if ( value instanceof VNumberArray ) {

                        ListNumber array = ((VNumberArray) value).getData();

                        if ( array.size() > 0 ) {
                            symbolIndex = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    } else if ( value instanceof VEnumArray ) {

                        ListNumber array = ((VEnumArray) value).getIndexes();

                        if ( array.size() > 0 ) {
                            symbolIndex = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    }
                }

            } finally {
                updatingValue.set(false);
            }

            symbolIndex = Math.min(Math.max(symbolIndex, 0), model_widget.propSymbols().size() - 1);

            final String symbol_value = ( symbolIndex >= 0 ) ? model_widget.propSymbols().getElement(symbolIndex).getValue() : "\u263A";
            model_widget.runtimePropSymbolValue().setValue(symbol_value);
            jfx_node.setText(symbol_value);

        }

    }

    @Override
    protected Label createJFXNode ( ) throws Exception {

        Label symbol = new Label();

        symbol.setAlignment(JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(), model_widget.propVerticalAlignment().getValue()));
        symbol.setBackground(model_widget.propTransparent().getValue()
            ? null
            : new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY))
        );
        symbol.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        symbol.setTextFill(JFXUtil.convert(model_widget.propForegroundColor().getValue()));
        symbol.setText("\u263A");
        symbol.setManaged(false);

        enabled = model_widget.propEnabled().getValue();

        Styles.update(symbol, Styles.NOT_ENABLED, !enabled);

        return symbol;

    }

    @Override
    protected void registerListeners ( ) {

        super.registerListeners();

        model_widget.propPVName().addUntypedPropertyListener(contentListener);
        model_widget.propArrayIndex().addUntypedPropertyListener(contentListener);

        model_widget.propSymbols().addPropertyListener(symbolsListener);

        model_widget.propVisible().addUntypedPropertyListener(geometryListener);
        model_widget.propX().addUntypedPropertyListener(geometryListener);
        model_widget.propY().addUntypedPropertyListener(geometryListener);
        model_widget.propWidth().addUntypedPropertyListener(geometryListener);
        model_widget.propHeight().addUntypedPropertyListener(geometryListener);

        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propHorizontalAlignment().addUntypedPropertyListener(styleListener);
        model_widget.propRotationStep().addUntypedPropertyListener(styleListener);
        model_widget.propTransparent().addUntypedPropertyListener(styleListener);
        model_widget.propVerticalAlignment().addUntypedPropertyListener(styleListener);
        model_widget.propWrapWords().addUntypedPropertyListener(styleListener);

        if ( toolkit.isEditMode() ) {
            dirtyValue.checkAndClear();
        } else {
            model_widget.runtimePropValue().addPropertyListener(valueListener);
            valueChanged(null, null, null);
        }
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propPVName().removePropertyListener(contentListener);
        model_widget.propArrayIndex().removePropertyListener(contentListener);
        model_widget.propSymbols().removePropertyListener(symbolsListener);
        model_widget.propVisible().removePropertyListener(geometryListener);
        model_widget.propX().removePropertyListener(geometryListener);
        model_widget.propY().removePropertyListener(geometryListener);
        model_widget.propWidth().removePropertyListener(geometryListener);
        model_widget.propHeight().removePropertyListener(geometryListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propEnabled().removePropertyListener(styleListener);
        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propHorizontalAlignment().removePropertyListener(styleListener);
        model_widget.propRotationStep().removePropertyListener(styleListener);
        model_widget.propTransparent().removePropertyListener(styleListener);
        model_widget.propVerticalAlignment().removePropertyListener(styleListener);
        model_widget.propWrapWords().removePropertyListener(styleListener);

        if (! toolkit.isEditMode())
            model_widget.runtimePropValue().removePropertyListener(valueListener);

        super.unregisterListeners();
    }

    private void contentChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyContent.mark();
        toolkit.scheduleUpdate(this);
    }

    private void geometryChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyGeometry.mark();
        toolkit.scheduleUpdate(this);
    }

    private void styleChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyStyle.mark();
        toolkit.scheduleUpdate(this);
    }

    private void symbolChanged ( final WidgetProperty<String> property, final String oldValue, final String newValue ) {
        dirtyContent.mark();
        toolkit.scheduleUpdate(this);
    }

    private void symbolsChanged ( final WidgetProperty<List<WidgetProperty<String>>> property, final List<WidgetProperty<String>> oldValue, final List<WidgetProperty<String>> newValue ) {

        if ( oldValue != null ) {
            oldValue.stream().forEach(p -> p.removePropertyListener(symbolPropertyListener));
        }

        if ( newValue != null ) {
            newValue.stream().forEach(p -> p.addPropertyListener(symbolPropertyListener));
        }

        dirtyContent.mark();
        toolkit.scheduleUpdate(this);

    }

    private void valueChanged ( final WidgetProperty<? extends VType> property, final VType oldValue, final VType newValue ) {
        dirtyValue.mark();
        toolkit.scheduleUpdate(this);
    }

}
