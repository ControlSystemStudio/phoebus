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

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.SymbolWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.SVGHelper;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Styles;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;



/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 19 Jun 2017
 */
public class SymbolRepresentation extends RegionBaseRepresentation<StackPane, SymbolWidget> {

    private static final double INDEX_LABEL_SIZE = 32.0;

    private int                                  arrayIndex             = 0;
    private boolean                              defaultSymbolVisible   = false;
    private volatile boolean                     autoSize               = false;
    private Symbol                               symbol;
    private final Symbol                         defaultSymbol;
    private final DefaultSymbolNode              defaultSymbolNode      = new DefaultSymbolNode();
    private final DirtyFlag                      dirtyContent           = new DirtyFlag();
    private final DirtyFlag                      dirtyGeometry          = new DirtyFlag();
    private final DirtyFlag                      dirtyStyle             = new DirtyFlag();
    private final DirtyFlag                      dirtyValue             = new DirtyFlag();
    private final UntypedWidgetPropertyListener contentListener = this::contentChanged;
    private final UntypedWidgetPropertyListener geometryListener = this::geometryChanged;
    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final WidgetPropertyListener<VType> valueListener = this::valueChanged;
    private final WidgetPropertyListener<List<WidgetProperty<String>>> symbolsListener = this::symbolsChanged;
    private final WidgetPropertyListener<Integer> indexListener = this::initialIndexChanged;

    private volatile boolean                     enabled                = true;
    private final ImageView                      imageView              = new ImageView();
    private final Label                          indexLabel             = new Label();
    private final Circle                         indexLabelBackground   = new Circle(INDEX_LABEL_SIZE / 2, Color.BLACK.deriveColor(0.0, 0.0, 0.0, 0.75));
    private final Rectangle                      disconnectedRectangle  = new Rectangle();
    private Dimension2D                          maxSize                = new Dimension2D(0, 0);
    private final WidgetPropertyListener<String> symbolPropertyListener = this::symbolChanged;
    private final AtomicReference<List<Symbol>>  symbols                = new AtomicReference<>(Collections.emptyList());
    private final AtomicBoolean                  updatingValue          = new AtomicBoolean(false);

    // ---- imageIndex property
    private IntegerProperty imageIndex = new SimpleIntegerProperty(-1);

    private int getImageIndex ( ) {
        return imageIndex.get();
    }

    private IntegerProperty imageIndexProperty ( ) {
        return imageIndex;
    }

    /**
     * Compute the maximum width and height of the given {@code widget} based on
     * the its set of symbol images.
     *
     * @param widget The {@link SymbolWidget} whose size must be computed.
     * @return A not {@code null} maximum dimension of the given {@code widget}.
     */
    public static Dimension2D computeMaximumSize ( final SymbolWidget widget ) {

        Double[] max_size = new Double[] { 0.0, 0.0 };

        widget.propSymbols().getValue().stream().forEach(s -> {

            final String imageFile = s.getValue();

            try {

                final String filename = ModelResourceUtil.resolveResource(widget.getTopDisplayModel(), imageFile);
                final Image image = new Image(ModelResourceUtil.openResourceStream(filename));

                if ( max_size[0] < image.getWidth() ) {
                    max_size[0] = image.getWidth();
                }
                if ( max_size[1] < image.getHeight() ) {
                    max_size[1] = image.getHeight();
                }

            } catch ( Exception ex ) {
                //  The following message has proven to be annoying and not useful.
                //logger.log(Level.WARNING, "Cannot obtain image size for {0} [{1}].", new Object[] { imageFile, ex.getMessage() });
            }

        });

        return new Dimension2D(max_size[0], max_size[1]);

    }

    public static String resolveImageFile ( SymbolWidget widget, String imageFileName ) {

        try {

            String expandedFileName = MacroHandler.replace(widget.getMacrosOrProperties(), imageFileName);

            // Resolve new image file relative to the source widget model (not 'top'!).
            // Get the display model from the widget tied to this representation.
            final DisplayModel widgetModel = widget.getDisplayModel();

            // Resolve the image path using the parent model file path.
            return ModelResourceUtil.resolveResource(widgetModel, expandedFileName);

        } catch ( Exception ex ) {

            logger.log(Level.WARNING, String.format("Failure resolving image path: %s", imageFileName), ex);

            return null;

        }

    }

    private static boolean resourceExists ( String fileName ) {

        try {
            ModelResourceUtil.openResourceStream(fileName);
        } catch ( Exception ex ) {
            return false;
        }

        return true;

    }

    public SymbolRepresentation ( ) {

        super();

        //  This initialization must be performed here, to allow defaultSymbolNode
        //  to be initialized first.
        defaultSymbol = new Symbol();

    }

    @Override
    public void dispose ( ) {
        super.dispose();

        symbol = null;

        symbols.get().clear();
        symbols.set(null);
    }

    private void setArrayIndex ( ) {
        Object value = model_widget.propArrayIndex().getValue();

        if ( !Objects.equals(value, arrayIndex) ) {
            arrayIndex = Math.max(0, (int) value);
        }
    }

    @Override
    public void updateChanges ( ) {

        super.updateChanges();

        Object value;

        //  Must be the first "if" statement to be executed, because it select the array index for the value.
        if ( dirtyContent.checkAndClear() ) {

            setArrayIndex();

            dirtyValue.mark();

        }

        //  Must be the second "if" statement to be executed, because it select the node to be displayed.
        if ( dirtyValue.checkAndClear() && updatingValue.compareAndSet(false, true) ) {

            int idx = Integer.MIN_VALUE;    // Marker indicating non-valid value.

            try {

                value = model_widget.runtimePropValue().getValue();

                if ( value != null ) {
                    disconnectedRectangle.setVisible(false);

                    if ( PVWidget.RUNTIME_VALUE_NO_PV == value ) {
                        idx = model_widget.propInitialIndex().getValue();
                    } else if ( value instanceof VBoolean ) {
                        idx = ( (VBoolean) value ).getValue() ? 1 : 0;
                    } else if ( value instanceof VString ) {
                        try {
                            idx = Integer.parseInt(( (VString) value ).getValue());
                        } catch ( NumberFormatException nfex ) {
                            logger.log(Level.FINE, "Failure parsing the string value: {0} [{1}].", new Object[] { ( (VString) value ).getValue(), nfex.getMessage() });
                        }
                    } else if ( value instanceof VNumber ) {
                        idx = ( (VNumber) value ).getValue().intValue();
                    } else if ( value instanceof VEnum ) {
                        idx = ( (VEnum) value ).getIndex();
                    } else if ( value instanceof VNumberArray ) {

                        ListNumber array = ( (VNumberArray) value ).getData();

                        if ( array.size() > 0 ) {
                            idx = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    } else if ( value instanceof VEnumArray ) {

                        ListNumber array = ( (VEnumArray) value ).getIndexes();

                        if ( array.size() > 0 ) {
                            idx = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    }
                } else if (! toolkit.isEditMode()) {
                    disconnectedRectangle.setVisible(true);
                } else {
                    idx = model_widget.propInitialIndex().getValue();
                }

            } finally {
                updatingValue.set(false);
            }

            List<Symbol> symbolsList = symbols.get();
            int oldIndex = getImageIndex();

            if ( idx == Integer.MIN_VALUE ) {
                // Keep current index
                idx = Math.min(Math.max(oldIndex, 0), symbolsList.size() - 1);
            }

            if ( idx < 0 || symbolsList.isEmpty() ) {
                symbol = getDefaultSymbol();
            } else {
                symbol = symbolsList.get(Math.min(idx, symbolsList.size() - 1));
            }

            if ( oldIndex != idx ) {
                dirtyGeometry.mark();
            }

            imageIndex.set(idx);
            jfx_node.getChildren().set(0, getSymbolNode(true));

        }

        if ( dirtyGeometry.checkAndClear() ) {

            value = model_widget.propVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isVisible()) ) {
                jfx_node.setVisible((boolean) value);
            }

            value = model_widget.propAutoSize().getValue();

            if ( !Objects.equals(value, autoSize) ) {
                autoSize = (boolean) value;
            }

            if ( autoSize ) {
                model_widget.propWidth().setValue((int) Math.round(maxSize.getWidth()));
                model_widget.propHeight().setValue((int) Math.round(maxSize.getHeight()));
            }

            double w = model_widget.propWidth().getValue();
            double h = model_widget.propHeight().getValue();

            if ( w < INDEX_LABEL_SIZE || h < INDEX_LABEL_SIZE ) {
                jfx_node.getChildren().remove(indexLabel);
                jfx_node.getChildren().remove(indexLabelBackground);
            } else {
                if ( !jfx_node.getChildren().contains(indexLabelBackground) ) {
                    jfx_node.getChildren().add(indexLabelBackground);
                }
                if ( !jfx_node.getChildren().contains(indexLabel) ) {
                    jfx_node.getChildren().add(indexLabel);
                }
            }

            if ( symbol != null ) {
                setSymbolSize(w, h, model_widget.propPreserveRatio().getValue());
            }

            disconnectedRectangle.setWidth(w);
            disconnectedRectangle.setHeight(h);

            jfx_node.setLayoutX(model_widget.propX().getValue());
            jfx_node.setLayoutY(model_widget.propY().getValue());
            jfx_node.setPrefSize(w, h);

            value = model_widget.propRotation().getValue();

            if ( !Objects.equals(value, jfx_node.getRotate()) ) {
                jfx_node.setRotate((double) value);
            }

        }

        if ( dirtyStyle.checkAndClear() ) {

            value = model_widget.propEnabled().getValue();

            if ( !Objects.equals(value, enabled) ) {

                enabled = (boolean) value;

                Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);

            }

            value = model_widget.propShowIndex().getValue() || defaultSymbolVisible;

            if ( !Objects.equals(value, indexLabel.isVisible()) ) {
                indexLabel.setVisible((boolean) value);
                indexLabelBackground.setVisible((boolean) value);
            }

            if ( model_widget.propTransparent().getValue() ) {
                jfx_node.setBackground(null);
            } else {
                jfx_node.setBackground(new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
            }

        }

    }

    @Override
    protected StackPane createJFXNode ( ) throws Exception {

        autoSize = model_widget.propAutoSize().getValue();
        symbol = getDefaultSymbol();

        StackPane symbolPane = new StackPane();

        indexLabelBackground.setStroke(Color.LIGHTGRAY.deriveColor(0.0, 1.0, 1.0, 0.75));
        indexLabelBackground.setVisible(model_widget.propShowIndex().getValue());

        indexLabel.setAlignment(Pos.CENTER);
        indexLabel.setFont(Font.font(indexLabel.getFont().getFamily(), FontWeight.BOLD, 16));
        indexLabel.setTextFill(Color.WHITE);
        indexLabel.setVisible(model_widget.propShowIndex().getValue());
        indexLabel.textProperty().bind(Bindings.convert(imageIndexProperty()));

        WidgetColor rect_color = WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID);
        WidgetColor arect_color = new WidgetColor(rect_color.getRed(), rect_color.getGreen(), rect_color.getBlue(), 128);
        disconnectedRectangle.setFill(JFXUtil.convert(arect_color));
        if (toolkit.isEditMode())
            disconnectedRectangle.setVisible(false);

        symbolPane.getChildren().addAll(getSymbolNode(false), indexLabelBackground, indexLabel, disconnectedRectangle);

        if ( model_widget.propTransparent().getValue() ) {
            symbolPane.setBackground(null);
        } else {
            symbolPane.setBackground(new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
        }

        enabled = model_widget.propEnabled().getValue();

        // Set array index here so that we can clear dirtyContent --> dirtyContent sets dirtyValue and we don't want that
        setArrayIndex();
        dirtyContent.checkAndClear();

        Styles.update(symbolPane, Styles.NOT_ENABLED, !enabled);

        // Clear dirtyValue, we have nothing to show yet
        dirtyValue.checkAndClear();

        symbolChanged(null, null, null);

        return symbolPane;

    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        model_widget.propArrayIndex().addUntypedPropertyListener(contentListener);
        model_widget.propPVName().addUntypedPropertyListener(contentListener);

        model_widget.propSymbols().addPropertyListener(symbolsListener);
        model_widget.propSymbols().getValue().stream().forEach(p -> p.addPropertyListener(symbolPropertyListener));

        model_widget.propInitialIndex().addPropertyListener(indexListener);

        model_widget.propAutoSize().addUntypedPropertyListener(geometryListener);
        model_widget.propVisible().addUntypedPropertyListener(geometryListener);
        model_widget.propX().addUntypedPropertyListener(geometryListener);
        model_widget.propY().addUntypedPropertyListener(geometryListener);
        model_widget.propWidth().addUntypedPropertyListener(geometryListener);
        model_widget.propHeight().addUntypedPropertyListener(geometryListener);
        model_widget.propPreserveRatio().addUntypedPropertyListener(geometryListener);
        model_widget.propRotation().addUntypedPropertyListener(geometryListener);

        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleListener);
        model_widget.propShowIndex().addUntypedPropertyListener(styleListener);
        model_widget.propTransparent().addUntypedPropertyListener(styleListener);

        if (!toolkit.isEditMode())
            model_widget.runtimePropValue().addPropertyListener(valueListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propArrayIndex().removePropertyListener(contentListener);
        model_widget.propPVName().removePropertyListener(contentListener);

        model_widget.propSymbols().removePropertyListener(symbolsListener);
        model_widget.propSymbols().getValue().stream().forEach(p -> p.removePropertyListener(symbolPropertyListener));

        model_widget.propInitialIndex().removePropertyListener(indexListener);

        model_widget.propAutoSize().removePropertyListener(geometryListener);
        model_widget.propVisible().removePropertyListener(geometryListener);
        model_widget.propX().removePropertyListener(geometryListener);
        model_widget.propY().removePropertyListener(geometryListener);
        model_widget.propWidth().removePropertyListener(geometryListener);
        model_widget.propHeight().removePropertyListener(geometryListener);
        model_widget.propPreserveRatio().removePropertyListener(geometryListener);
        model_widget.propRotation().removePropertyListener(geometryListener);

        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propEnabled().removePropertyListener(styleListener);
        model_widget.propShowIndex().removePropertyListener(styleListener);
        model_widget.propTransparent().removePropertyListener(styleListener);

        if (!toolkit.isEditMode())
            model_widget.runtimePropValue().removePropertyListener(valueListener);

        super.unregisterListeners();
    }

    private void contentChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyContent.mark();
        toolkit.scheduleUpdate(this);
    }

    /**
     * Fix the file names imported from BOY.
     */
    private void fixImportedSymbolNames ( ) {

        try {

            ArrayWidgetProperty<WidgetProperty<String>> propSymbols = model_widget.propSymbols();
            List<String> fileNames = new ArrayList<>(2);

            switch ( model_widget.getImportedFrom() ) {
                case "org.csstudio.opibuilder.widgets.ImageBoolIndicator":
                    return;
                case "org.csstudio.opibuilder.widgets.symbol.bool.BoolMonitorWidget": {

                        String imageFileName = propSymbols.getElement(0).getValue();
                        int dotIndex = imageFileName.lastIndexOf('.');
                        String onImageFileName = imageFileName.substring(0, dotIndex) + " On" + imageFileName.substring(dotIndex);
                        String offImageFileName = imageFileName.substring(0, dotIndex) + " Off" + imageFileName.substring(dotIndex);

                        if ( resourceExists(resolveImageFile(model_widget, onImageFileName)) ) {
                            fileNames.add(onImageFileName);
                        } else {
                            fileNames.add(imageFileName);
                        }

                        if ( resourceExists(resolveImageFile(model_widget, offImageFileName)) ) {
                            fileNames.add(offImageFileName);
                        } else {
                            fileNames.add(imageFileName);
                        }

                    }
                    break;
                case "org.csstudio.opibuilder.widgets.symbol.multistate.MultistateMonitorWidget": {

                        String imageFileName = propSymbols.getElement(0).getValue();
                        int dotIndex = imageFileName.lastIndexOf('.');
                        int spaceIndex = imageFileName.lastIndexOf(' ');

                        try {
                            model_widget.propInitialIndex().setValue(Integer.parseInt(imageFileName.substring(1 + spaceIndex, dotIndex)));
                        } catch ( NumberFormatException nfex ) {
                            logger.log(Level.WARNING, "Imported image file doesn't contain state value [{0}].", imageFileName);
                        }

                        int index = 0;

                        while ( true ) {

                            String nthImageFileName = MessageFormat.format("{0} {1,number,#########0}{2}", imageFileName.substring(0, spaceIndex), index, imageFileName.substring(dotIndex));

                            if ( resourceExists(resolveImageFile(model_widget, nthImageFileName)) ) {
                                fileNames.add(nthImageFileName);
                            } else {
                                break;
                            }

                            index++;

                        }

                    }
                    break;
                default:
                    logger.log(Level.WARNING, "Invalid imported type [{0}].", model_widget.getImportedFrom());
                    return;
            }

            for ( int i = 0; i < fileNames.size(); i++ ) {
                model_widget.addOrReplaceSymbol(i, fileNames.get(i));
            }

        } finally {
            model_widget.clearImportedFrom();
        }

    }

    private void geometryChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyGeometry.mark();
        toolkit.scheduleUpdate(this);
    }

    private Symbol getDefaultSymbol ( ) {
        return defaultSymbol;
    }

    private DefaultSymbolNode getDefaultSymbolNode ( ) {
        return defaultSymbolNode;
    }

    Node getSymbolNode ( boolean setDefault ) {

        Image image = symbol.getImage();

        if ( image == null ) {
            if (!setDefault)
                return imageView;

            if (!defaultSymbolVisible) {
                defaultSymbolVisible = true;
                dirtyStyle.mark();
            }

            return getDefaultSymbolNode();
        } else {

            if (defaultSymbolVisible) {
                defaultSymbolVisible = false;
                dirtyStyle.mark();
            }

            imageView.setImage(image);

            return imageView;

        }

    }

    private void initialIndexChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyValue.mark();
        toolkit.scheduleUpdate(this);
    }

    void setSymbolSize ( double width, double height, boolean preserveRatio ) {
        if ( symbol != null ) {
            if ( symbol.getImage() == null ) {
                getDefaultSymbolNode().setSize(width, height);
            }else{
                symbol.resize(width, height, preserveRatio);
            }
        }
    }

    private void styleChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyStyle.mark();
        toolkit.scheduleUpdate(this);
    }

    private void symbolChanged ( final WidgetProperty<String> property, final String oldValue, final String newValue ) {
        ModelThreadPool.getExecutor().execute(this::updateSymbols);
    }

    private void symbolsChanged ( final WidgetProperty<List<WidgetProperty<String>>> property, final List<WidgetProperty<String>> oldValue, final List<WidgetProperty<String>> newValue ) {
        ModelThreadPool.getExecutor().execute( ( ) -> {

            if ( oldValue != null ) {
                oldValue.stream().forEach(p -> p.removePropertyListener(symbolPropertyListener));
            }

            if ( newValue != null ) {
                newValue.stream().forEach(p -> p.addPropertyListener(symbolPropertyListener));
            }

            updateSymbols();

        });
    }

    private synchronized void updateSymbols ( ) {

        List<WidgetProperty<String>> fileNames = model_widget.propSymbols().getValue();
        List<Symbol> symbolsList = new ArrayList<>(fileNames.size());
        Map<String, Symbol> symbolsMap = new HashMap<>(fileNames.size());
        Map<String, Symbol> currentSymbolsMap = symbols.get().stream().distinct().collect(Collectors.toMap(Symbol::getFileName, sc -> sc));

        try {

            if ( model_widget.getImportedFrom() != null ) {
                fixImportedSymbolNames();
            }

            fileNames.stream().forEach(f -> {

                String fileName = f.getValue();
                Symbol s = symbolsMap.get(fileName);

                if ( s == null ) {     // Symbol not yet loaded...

                    s = currentSymbolsMap.get(fileName);

                    if ( s == null ) { // Neither previously loaded.
                        s = new Symbol(fileName, model_widget.propWidth().getValue(), model_widget.propHeight().getValue());
                    }

                    symbolsMap.put(fileName, s);

                }

                symbolsList.add(s);

            });

        } finally {

            maxSize = new Dimension2D(
                symbolsList.stream().mapToDouble(Symbol::getOriginalWidth).max().orElse(0.0),
                symbolsList.stream().mapToDouble(Symbol::getOriginalHeight).max().orElse(0.0)
            );

            symbols.set(symbolsList);

            dirtyGeometry.mark();
            dirtyValue.mark();
            toolkit.scheduleUpdate(this);

        }

    }

    private void valueChanged ( final WidgetProperty<? extends VType> property, final VType oldValue, final VType newValue ) {
        dirtyValue.mark();
        toolkit.scheduleUpdate(this);
    }

    private class DefaultSymbolNode extends Group {

        private final Rectangle r;
        private final Line l1;
        private final Line l2;

        DefaultSymbolNode() {

            setManaged(true);

            int w = 100;
            int h = 100;

            r = new Rectangle(0, 0, w, h);

            r.setFill(null);
            r.setArcHeight(0);
            r.setArcWidth(0);
            r.setStroke(Color.BLACK);
            r.setStrokeType(StrokeType.INSIDE);

            l1 = new Line(0.5, 0.5, w - 0.5, h - 0.5);

            l1.setStroke(Color.BLACK);
            l1.setStrokeLineCap(StrokeLineCap.BUTT);

            l2 = new Line(0.5, h - 0.5, w - 0.5, 0.5);

            l2.setStroke(Color.BLACK);
            l2.setStrokeLineCap(StrokeLineCap.BUTT);

            getChildren().add(r);
            getChildren().add(l1);
            getChildren().add(l2);

        }

        public void setSize ( double w, double h ) {

            r.setWidth(w);
            r.setHeight(h);

            l1.setEndX(w - 0.5);
            l1.setEndY(h - 0.5);

            l2.setEndX(w - 0.5);
            l2.setStartY(h - 0.5);

        }

    }

    private class Symbol {

        private final String fileName;
        private Image image = null;
        private double originalHeight = 100;
        private double originalWidth = 100;

        Symbol ( ) {
            fileName = null;
        }

        Symbol ( String fileName, double width, double height ) {

            this.fileName = fileName;

            String imageFileName = resolveImageFile(model_widget, fileName);

            if ( imageFileName != null ) {
                if (toolkit.isEditMode()) {
                    ImageCache.remove(imageFileName);
                }

                if(imageFileName.toLowerCase().endsWith("svg")){
                    image = loadSVG(width, height);
                }
                else{
                    image = ImageCache.cache(imageFileName, () ->
                    {
                        // Open the image from the stream created from the
                        // resource file.
                        try{
                            return new Image(ModelResourceUtil.openResourceStream(imageFileName));
                        } catch ( Exception ex ) {
                            logger.log(Level.WARNING, "Failure loading image: ({0}) {1} [{2}].", new Object[] { fileName, imageFileName, ex.getMessage() });
                        }
                        return null;
                    });
                }

                if ( image != null ) {
                    originalWidth = image.getWidth();
                    originalHeight = image.getHeight();
                }
            }
        }

        String getFileName ( ) {
            return fileName;
        }

        double getOriginalHeight ( ) {
            return originalHeight;
        }

        double getOriginalWidth ( ) {
            return originalWidth;
        }

        Image getImage ( ) {
            return image;
        }

        /**
         * Resizes the image. If the underlying resource is a SVG, it is reloaded.
         * @param width
         * @param height
         * @param preserveRatio T
         */
        void resize(double width, double height, boolean preserveRatio){
            if(fileName.toLowerCase().endsWith("svg")) {
                image = loadSVG(width, height);
                imageView.setImage(image);
            }
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(preserveRatio);
        }

        /**
         * Loads a SVG resource. The image cache is used, but the key to the SVG resource depends
         * on the width and height of the image. Reason is that when resizing a image the underlying
         * SVG must be transcoded again with the new size.
         * @param width
         * @param height
         * @return An {@link Image} or <code>null</code>.
         */
        Image loadSVG(double width, double height){
            String imageFileName = resolveImageFile(model_widget, fileName);
            return SVGHelper.loadSVG(imageFileName, width, height);
        }
    }
}
