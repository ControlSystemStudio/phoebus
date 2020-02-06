/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.util;

import static javafx.scene.paint.Color.GRAY;
import static javafx.scene.paint.Color.web;
import static org.csstudio.display.builder.editor.Plugin.logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.EditorUtil;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.palette.Palette;
import org.csstudio.display.builder.editor.tracker.SelectedWidgetUITracker;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.PictureWidget;
import org.csstudio.display.builder.model.widgets.SymbolWidget;
import org.csstudio.display.builder.model.widgets.WebBrowserWidget;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.SVGHelper;
import org.csstudio.display.builder.representation.javafx.widgets.SymbolRepresentation;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/** Helper for widget drag/drop
 *
 *  <h3>Handling New File/URL Extensions</h3>
 *  To add the support for a new set of file/URL extensions do the following:
 *  <ul>
 *   <li>Create a new static list of extensions (see {@link #IMAGE_FILE_EXTENSIONS});</li>
 *   <li>Update {@link #SUPPORTED_EXTENSIONS} to include the new list;</li>
 *   <li>Update {@link #installWidgetsFromFiles(Dragboard, SelectedWidgetUITracker, List, List)}
 *       to handle files having the new extensions;</li>
 *   <li>Update {@link #installWidgetsFromURL(DragEvent, List, List)}
 *       to handle URLs having the new extensions.</li>
 *  </ul>
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class WidgetTransfer {

    //  The extensions listed here MUST BE ALL UPPERCASE.
    private static List<String> IMAGE_FILE_EXTENSIONS = Arrays.asList("BMP", "GIF", "JPEG", "JPG", "PNG", "SVG");
    private static List<String> EMBEDDED_FILE_EXTENSIONS = Arrays.asList("BOB", "OPI");
    private static List<String> SUPPORTED_EXTENSIONS = Stream.concat(IMAGE_FILE_EXTENSIONS.stream(), EMBEDDED_FILE_EXTENSIONS.stream()).collect(Collectors.toList());

    // Lazily initialized list of widgets that have a PV
    private static List<WidgetDescriptor> pvWidgetDescriptors = null;

    // Could create custom data format, or use "application/xml".
    // Transferring as DataFormat("text/plain"), however, allows exchange
    // with basic text editor, which can be very convenient.

    /**
     * Add support for 'dragging' a widget out of a node
     *
     * @param source Source {@link Node}
     * @param editor
     * @param palette Description of widget type to drag
     * @param descriptor
     * @param image Image to represent the widget, or <code>null</code>
     */
    public static void addDragSupport (
        final Node source,
        final DisplayEditor editor,
        final Palette palette,
        final WidgetDescriptor descriptor,
        final Image image
    ) {

        source.setOnDragDetected( ( MouseEvent event ) -> {

            logger.log(Level.FINE, "Starting drag for {0}", descriptor);

            editor.getWidgetSelectionHandler().clear();

            final Widget widget = descriptor.createWidget();

            // In class editor mode, create widget with some class name.
            // In display editor mode, apply the class settings.
            final DisplayModel model = editor.getModel();

            if ( model != null && model.isClassModel() ) {
                widget.propName().setValue("MY_CLASS");
            } else {
                WidgetClassesService.getWidgetClasses().apply(widget);
            }

            final String xml;

            try {
                xml = ModelWriter.getXML(Arrays.asList(widget));
            } catch ( Exception ex ) {
                logger.log(Level.WARNING, "Cannot drag-serialize", ex);
                return;
            }

            final Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();

            content.putString(xml);
            db.setContent(content);

            // Create drag outline for widget, adjusted by zoom factor
            final double zoom = editor.getZoom();
            final int width = (int) (widget.propWidth().getValue() * zoom);
            final int height = (int) (widget.propHeight().getValue() * zoom);
            db.setDragView(createDragImage(widget, image, width, height));
            event.consume();
        });

        source.setOnDragDone(event -> {
            // Widget was dropped
            // -> Stop scrolling, clear the selected palette entry
            editor.getAutoScrollHandler().canceTimeline();
            palette.clearSelectedWidgetType();
        });

    }

    /**
     * Add support for dropping widgets
     *
     * @param node Node that will receive the widgets
     * @param group_handler Group handler
     * @param selection_tracker The selection tracker.
     * @param handleDroppedModel Callback for handling the dropped widgets
     */
    public static void addDropSupport (
        final Node node,
        final ParentHandler group_handler,
        final SelectedWidgetUITracker selection_tracker,
        final Consumer<List<Widget>> handleDroppedModel
    ) {

        node.setOnDragOver(event  ->
        {
            final Dragboard db = event.getDragboard();

            if (db.hasString() || db.hasUrl() || db.hasRtf() || db.hasHtml() || db.hasImage() || db.hasFiles())
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);

            group_handler.locateParent(event.getX(), event.getY(), 10, 10);
            event.consume();
        });

        node.setOnDragDropped(event ->
        {
            final Dragboard db = event.getDragboard();
            final Point2D location = selection_tracker.gridConstrain(event.getX(), event.getY());
            final List<Widget> widgets = new ArrayList<>();
            final List<Runnable> updates = new ArrayList<>();

            // Used to check for HTML and RTF only to extract plain text from it.
            // On RedHat 7, dropping from email in web browser,
            // that resulted in getting no text at all.
            // Same for RTF from Mac TextEdit.
            // On the other hand, plain String works, so using that.
            if (db.hasFiles() && canAcceptFiles(db.getFiles()))
                installWidgetsFromFiles(db, selection_tracker, widgets, updates);
            else if (db.hasImage() && db.getImage() != null)
                installPictureWidgetFromImage(db, selection_tracker, widgets);
            else if (db.hasUrl() && db.getUrl() != null)
                installWidgetsFromURL(event, widgets, updates);
            else if (db.hasString() && db.getString() != null)
                installWidgetsFromString(event, selection_tracker, widgets);

            if (widgets.isEmpty())
                event.setDropCompleted(false);
            else
            {
                logger.log(Level.FINE, "Dropped {0} widgets.", widgets.size());

                GeometryTools.moveWidgets((int) location.getX(), (int) location.getY(), widgets);
                handleDroppedModel.accept(widgets);

                // Now that model holds the widgets, perform updates that for
                // example check an image size
                for ( Runnable update : updates ) {
                    EditorUtil.getExecutor().execute(update);
                }

                event.setDropCompleted(true);
            }

            event.consume();
            group_handler.hide();

        });
    }

    /** @param file File
     *  @return File extension or ""
     */
    private static String getExtension(final File file)
    {
        return getExtension(file.getName());
    }

    /** @param name File name
     *  @return File extension or ""
     */
    private static String getExtension(final String name)
    {
        final int sep = name.lastIndexOf('.');
        if (sep >= 0)
            return name.substring(sep+1);
        return "";
    }


    /**
     * Return {@code true} if there is a {@link File} in {@code files}
     * whose extension is one of the {@link #SUPPORTED_EXTENSIONS}.
     * <P>
     * <B>Note:<B> only one file will be accepted: the first one
     * matching the above condition.
     *
     * @param files The {@link List} of {@link File}s to be checked.
     *            Can be {@code null} or empty.
     * @return {@code true} if a file existing whose extension is
     *         contained in {@link #SUPPORTED_EXTENSIONS}.
     */
    private static boolean canAcceptFiles ( final List<File> files ) {

        for (File file : files)
            if (SUPPORTED_EXTENSIONS.contains(getExtension(file).toUpperCase()))
                return true;

        return false;
    }

    /**
     * Create a image representing the dragged widget.
     *
     * @param widget The {@link Widget} being dragged.
     * @param image The widget's type image. Can be {@code null}.
     * @return An {@link Image} instance.
     */
    private static Image createDragImage ( final Widget widget, final Image image, final int width, final int height ) {

        WritableImage dImage = new WritableImage(width, height);

        if ( image != null ) {

//            int w = (int) image.getWidth();
//            int h = (int) image.getHeight();
//            int xo = (int) ( ( width - w ) / 2.0 );
//            int yo = (int) ( ( height - h ) / 2.0 );
//            PixelReader pixelReader = image.getPixelReader();
            PixelWriter pixelWriter = dImage.getPixelWriter();

//  TODO: CR: It seems there is a bug pixelReader.getArgb(...) when on Mac when
//  screen is scaled to have more resolution (no retina scaling is used).
//            for ( int x = 0; x < w; x++ ) {
//                for ( int y = 0; y < h; y++ ) {
//
//                    int wx = xo + x;
//                    int wy = yo + y;
//
//                    if ( wx > 0 && wx < width && wy > 0 && wy < height ) {
//
//                        int argb = pixelReader.getArgb(x, y);
//
//                        pixelWriter.setArgb(wx, wy, pixelReader.getArgb(x, y));
//
//                    }
//
//                }
//            }

            //  CR: Solution #1 - draw only the widget outline.
            for ( int x = 0; x < width; x++ ) {
                pixelWriter.setColor(x, 0, GRAY);
                pixelWriter.setColor(x, height - 1, GRAY);
            }
            for ( int y = 0; y < height; y++ ) {
                pixelWriter.setColor(0, y, GRAY);
                pixelWriter.setColor(width - 1, y, GRAY);
            }

        }

        return dImage;

//  CR: Solution #2 - draw only the widget type image.
//        return image;

    }

    /**
     * @return Widgets that have a PV.
     * */
    private static List<WidgetDescriptor> getPVWidgetDescriptors ( ) {

        if ( pvWidgetDescriptors == null ) {
            pvWidgetDescriptors = WidgetFactory.getInstance()
                                               .getWidgetDescriptions()
                                               .stream()
                                               .filter(d -> d.createWidget() instanceof PVWidget)
                                               .collect(Collectors.toList());
        }

        return pvWidgetDescriptors;

    }

    /**
     * @param display_file Display file for which to create an
     *            {@link EmbeddedDisplayWidget}.
     * @param selection_tracker Used to get the grid steps from its model to be
     *            used
     *            in offsetting multiple widgets.
     * @param widgets The container of the created widgets.
     * @param updates Updates to perform on widgets
     */
    private static void installEmbeddedDisplayWidgetFromFile (
        final String display_file,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets,
        final List<Runnable> updates
    ) {

        logger.log(Level.FINE, "Creating EmbeddedDisplayWidget for {0}", display_file);

        final DisplayModel model = selection_tracker.getModel();
        final EmbeddedDisplayWidget widget = (EmbeddedDisplayWidget) EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.createWidget();

        widget.propFile().setValue(display_file);

        // Offset multiple widgets by grid size
        final int index = widgets.size();

        widget.propX().setValue(model.propGridStepX().getValue() * index);
        widget.propY().setValue(model.propGridStepY().getValue() * index);

        widgets.add(widget);
        updates.add( ( ) -> updateEmbeddedDisplayWidget(widget));

    }

    /**
     * @param image_file The image file used to create and preset a {@link PictureWidget}.
     * @param selection_tracker Used to get the grid steps from its model to be
     *            used in offsetting multiple widgets.
     * @param widgets The container of the created widgets.
     * @param updates Updates to perform on widgets
     */
    private static void installPictureWidgetFromFile (
        final String image_file,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets,
        final List<Runnable> updates
    ) {

        logger.log(Level.FINE, "Creating PictureWidget for dropped image {0}", image_file);

        final DisplayModel model = selection_tracker.getModel();
        final PictureWidget widget = (PictureWidget) PictureWidget.WIDGET_DESCRIPTOR.createWidget();

        widget.propFile().setValue(image_file);

        final int index = widgets.size();

        widget.propX().setValue(model.propGridStepX().getValue() * index);
        widget.propY().setValue(model.propGridStepY().getValue() * index);

        widgets.add(widget);
        updates.add(() -> updatePictureWidgetSize(widget));

    }

    /**
     * @param db The {@link Dragboard} containing the dragged data.
     * @param selection_tracker Used to get the display model.
     * @param widgets The container of the created widgets.
     */
    private static void installPictureWidgetFromImage (
        final Dragboard db,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets
    ) {

        logger.log(Level.FINE, "Dropped image: creating PictureWidget");

        final DisplayModel model = selection_tracker.getModel();
        final ToolkitRepresentation<?, ?> toolkit = ToolkitRepresentation.getToolkit(selection_tracker.getModel());
        final String filename = toolkit.showSaveAsDialog(model, null);

        if ( filename == null ) {
            return;
        }

        final Image image = db.getImage();

        if ( image == null ) {
            return;
        }

        final PictureWidget widget = (PictureWidget) PictureWidget.WIDGET_DESCRIPTOR.createWidget();

        widget.propWidth().setValue((int) image.getWidth());
        widget.propHeight().setValue((int) image.getHeight());
        widgets.add(widget);

        // File access should not be on UI thread, but we need to return the widget right away.
        // -> Return the widget now, create the image file later, and then update the widget's file property
        EditorUtil.getExecutor().execute( ( ) -> {
            try {

                BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bImage, "png", new File(filename));

                widget.propFile().setValue(ModelResourceUtil.getRelativePath(model.getUserData(DisplayModel.USER_DATA_INPUT_FILE), filename));

            } catch ( Exception ex ) {
                logger.log(Level.WARNING, "Cannot save image as " + filename, ex);
            }
        });

    }

    /**
     * @param fileNames The pathnames of the image files used by the symbol widget.
     * @param selection_tracker Used to get the grid steps from its model to be
     *            used in offsetting multiple widgets.
     * @param widgets The container of the created widgets.
     * @param updates Updates to perform on widgets
     */
    private static void installSymbolWidgetFromImageFiles (
        final List<String> fileNames,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets,
        final List<Runnable> updates
    ) {

        logger.log(Level.FINE, "Creating SymbolWidget for {0,number,#########0} dropped images", fileNames.size());

        final DisplayModel model = selection_tracker.getModel();
        final SymbolWidget widget = (SymbolWidget) SymbolWidget.WIDGET_DESCRIPTOR.createWidget();

        for ( int i = 0; i < fileNames.size(); i++ ) {
            widget.addOrReplaceSymbol(i, fileNames.get(i));
        }

        final int index = widgets.size();

        widget.propX().setValue(model.propGridStepX().getValue() * index);
        widget.propY().setValue(model.propGridStepY().getValue() * index);

        widgets.add(widget);
        updates.add(() -> updateSymbolWidgetSize(widget));

    }

    /**
     * @param db The {@link Dragboard} containing the dragged data.
     * @param selection_tracker Used to get the grid steps from its model to be
     *            used in offsetting multiple widgets.
     * @param widgets The container of the created widgets.
     * @param updates Updates to perform on widgets
     */
    private static void installWidgetsFromFiles (
        final Dragboard db,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets,
        final List<Runnable> updates
    ) {

        final List<File> files = db.getFiles();

        if ( files.size() > 1 && files.stream().allMatch(f -> IMAGE_FILE_EXTENSIONS.contains(getExtension(f.toString()).toUpperCase())) ) {

            final List<String> fileNames = new ArrayList<>(files.size());

            files.stream().forEach(f -> fileNames.add(resolveFile(f, selection_tracker.getModel())));
            installSymbolWidgetFromImageFiles(fileNames, selection_tracker, widgets, updates);

        } else {
            for ( int i = 0; i < files.size(); i++ ) {

                final String fileName = resolveFile(files.get(i), selection_tracker.getModel());
                final String extension = getExtension(fileName).toUpperCase();

                if ( IMAGE_FILE_EXTENSIONS.contains(extension) ) {
                    installPictureWidgetFromFile(fileName, selection_tracker, widgets, updates);
                } else if ( EMBEDDED_FILE_EXTENSIONS.contains(extension) ) {
                    installEmbeddedDisplayWidgetFromFile(fileName, selection_tracker, widgets, updates);
                }

            }
        }

    }

    /**
     * @param event The {@link DragEvent} containing the dragged data.
     * @param selection_tracker Used to get the grid steps from its model to be
     *            used in offsetting multiple widgets.
     * @param widgets The container of the created widgets.
     */
    private static void installWidgetsFromString (
        final DragEvent event,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets
    ) {

        final Dragboard db = event.getDragboard();
        final String xmlOrText = db.getString();

        try {
            widgets.addAll(ModelReader.parseXML(xmlOrText).getChildren());
        } catch ( Exception ex ) {
            installWidgetsFromString(event, xmlOrText, selection_tracker, widgets);
        }

    }

    private static void installWidgetsFromString (
        final DragEvent event,
        final String text,
        final SelectedWidgetUITracker selection_tracker,
        final List<Widget> widgets
    ) {
        //  Consider each word a separate PV
        final String[] words = text.split("[ \n]+");
        final boolean multiple = words.length > 1;
        final List<WidgetDescriptor> descriptors = getPVWidgetDescriptors();
        final List<String> choices = new ArrayList<>(descriptors.size() + ( multiple ? 1 : 0 ));
        final String format = multiple ? Messages.WT_FromString_multipleFMT : Messages.WT_FromString_singleFMT;

        //  Always offer a single LabelWidget for the complete text
        final String defaultChoice = MessageFormat.format(Messages.WT_FromString_singleFMT, LabelWidget.WIDGET_DESCRIPTOR.getName());

        choices.add(defaultChoice);

        // When multiple words were dropped, offer multiple label widgets
        if ( multiple ) {
            choices.add(MessageFormat.format(Messages.WT_FromString_multipleFMT, LabelWidget.WIDGET_DESCRIPTOR.getName()));
        }

        choices.addAll(descriptors.stream().map(d -> MessageFormat.format(format, d.getName())).collect(Collectors.toList()));
        Collections.sort(choices);

        final ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices);

        dialog.setX(event.getScreenX() - 100);
        dialog.setY(event.getScreenY() - 200);
        dialog.setTitle(Messages.WT_FromString_dialog_title);
        dialog.setHeaderText(MessageFormat.format(Messages.WT_FromString_dialog_headerFMT, reduceStrings(words)));
        dialog.setContentText(Messages.WT_FromString_dialog_content);

        final Optional<String> result = dialog.showAndWait();

        if ( !result.isPresent() ) {
            return;
        }

        final String choice = result.get();

        if ( defaultChoice.equals(choice) ) {

            logger.log(Level.FINE, "Dropped text: created LabelWidget [{0}].", text);

            // Not a valid XML. Instantiate a Label object.
            final LabelWidget widget = (LabelWidget) LabelWidget.WIDGET_DESCRIPTOR.createWidget();

            widget.propText().setValue(text);
            widgets.add(widget);

        } else {   // Parse choice back into widget descriptor

            final MessageFormat msgf = new MessageFormat(format);
            final String descriptorName;

            try {
                descriptorName = msgf.parse(choice)[0].toString();
            } catch ( Exception ex ) {
                logger.log(Level.SEVERE, "Cannot parse selected widget type " + choice, ex);
                return;
            }

            WidgetDescriptor descriptor = null;

            if ( LabelWidget.WIDGET_DESCRIPTOR.getName().equals(descriptorName) ) {
                descriptor = LabelWidget.WIDGET_DESCRIPTOR;
            } else {
                descriptor = descriptors.stream().filter(d -> descriptorName.equals(d.getName())).findFirst().orElse(null);
            }

            if ( descriptor == null ) {
                logger.log(Level.SEVERE, "Cannot obtain widget for " + descriptorName);
                return;
            }

            for ( String word : words ) {

                final Widget widget = descriptor.createWidget();

                logger.log(Level.FINE, "Dropped text: created {0} [{1}].", new Object[] { widget.getClass().getSimpleName(), word });

                if ( widget instanceof PVWidget ) {
                    ( (PVWidget) widget ).propPVName().setValue(word);
                } else if ( widget instanceof LabelWidget ) {
                    ( (LabelWidget) widget ).propText().setValue(word);
                } else {
                    logger.log(Level.WARNING, "Unexpected widget type [{0}].", widget.getClass().getSimpleName());
                }

                final int index = widgets.size();

                if ( index > 0 ) {   // Place widgets below each other

                    final Widget previous = widgets.get(index - 1);
                    int x = previous.propX().getValue();
                    int y = previous.propY().getValue() + previous.propHeight().getValue();

                    // Align (if enabled)
                    final Point2D pos = selection_tracker.gridConstrain(x, y);

                    widget.propX().setValue((int) pos.getX());
                    widget.propY().setValue((int) pos.getY());

                }

                widgets.add(widget);

            }

        }

    }

    private static void installWidgetsFromURL (
        final DragEvent event,
        final List<Widget> widgets,
        final List<Runnable> updates
    ) {

        final String choice;
        final Dragboard db = event.getDragboard();
        String url = db.getUrl();

        // Fix URL, which on linux can contain the file name twice:
        // "http://some/path/to/file.xyz\nfile.xyz"
        int sep = url.indexOf('\n');

        if ( sep > 0 ) {
            url = url.substring(0, sep);
        }

        // Check URL's extension
        sep = url.lastIndexOf('.');

        final String ext = sep > 0 ? url.substring(1 + sep).toUpperCase() : null;

        if ( EMBEDDED_FILE_EXTENSIONS.contains(ext) ) {
            choice = EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.getName();
        } else if ( IMAGE_FILE_EXTENSIONS.contains(ext) ) {
            choice = PictureWidget.WIDGET_DESCRIPTOR.getName();
        } else {   // Prompt user

            final List<String> choices = Arrays.asList(
                LabelWidget.WIDGET_DESCRIPTOR.getName(),
                EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.getName(),
                PictureWidget.WIDGET_DESCRIPTOR.getName(),
                WebBrowserWidget.WIDGET_DESCRIPTOR.getName()
            );
            final ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(3), choices);

            // Position dialog
            dialog.setX(event.getScreenX());
            dialog.setY(event.getScreenY());

            dialog.setTitle(Messages.WT_FromURL_dialog_title);
            dialog.setHeaderText(MessageFormat.format(Messages.WT_FromURL_dialog_headerFMT, reduceString(url)));
            dialog.setContentText(Messages.WT_FromURL_dialog_content);

            final Optional<String> result = dialog.showAndWait();

            if ( result.isPresent() ) {
                choice = result.get();
            } else {
                return;
            }

        }

        if ( LabelWidget.WIDGET_DESCRIPTOR.getName().equals(choice) ) {

            logger.log(Level.FINE, "Creating LabelWidget for {0}", url);

            final LabelWidget widget = (LabelWidget) LabelWidget.WIDGET_DESCRIPTOR.createWidget();

            widget.propText().setValue(url);
            widgets.add(widget);

        } else if ( WebBrowserWidget.WIDGET_DESCRIPTOR.getName().equals(choice) ) {

            logger.log(Level.FINE, "Creating WebBrowserWidget for {0}", url);

            final WebBrowserWidget widget = (WebBrowserWidget) WebBrowserWidget.WIDGET_DESCRIPTOR.createWidget();

            widget.propWidgetURL().setValue(url);
            widgets.add(widget);

        } else if ( PictureWidget.WIDGET_DESCRIPTOR.getName().equals(choice) ) {

            logger.log(Level.FINE, "Creating PictureWidget for {0}", url);

            final PictureWidget widget = (PictureWidget) PictureWidget.WIDGET_DESCRIPTOR.createWidget();

            widget.propFile().setValue(url);
            widgets.add(widget);
            updates.add( ( ) -> updatePictureWidgetSize(widget));

        } else if ( EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.getName().equals(choice) ) {

            logger.log(Level.FINE, "Creating EmbeddedDisplayWidget for {0}", url);

            EmbeddedDisplayWidget widget = (EmbeddedDisplayWidget) EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.createWidget();

            widget.propFile().setValue(url);
            widgets.add(widget);
            updates.add( ( ) -> updateEmbeddedDisplayWidget(widget));

        }

    }

    private static String reduceString ( String text ) {
        if ( text.length() <= 64 ) {
            return text;
        } else {
            return text.substring(0, 32) + "..." + text.substring(text.length() - 32);
        }
    }

    private static String reduceStrings ( String[] lines ) {

        final int ALLOWED_LINES = 16;   // Should be a even number.
        List<String> validLines = new ArrayList<>(1 + ALLOWED_LINES);

        if ( lines.length <= ALLOWED_LINES ) {
            Arrays.asList(lines).stream().forEach(l -> validLines.add(reduceString(l)));
        } else {

            for ( int i = 0; i < ALLOWED_LINES / 2; i++ ) {
                validLines.add(reduceString(lines[i]));
            }

            validLines.add("...");

            for ( int i = lines.length - ALLOWED_LINES / 2; i < lines.length; i++ ) {
                validLines.add(reduceString(lines[i]));
            }

        }

        StringBuilder builder = new StringBuilder();

        validLines.stream().forEach(l -> builder.append(l).append("\n"));
        builder.deleteCharAt(builder.length() - 1);

        return builder.toString();

    }

    /**
     * @param file The {@link File} to be resolved.
     * @param model Used to get user's data.
     * @return The resolved file's pathname.
     */
    private static String resolveFile ( File file, final DisplayModel model ) {
        String fileName = file.toString();
        return ModelResourceUtil.getRelativePath(model.getUserData(DisplayModel.USER_DATA_INPUT_FILE), fileName);
    }

    /**
     * Update an embedded widget's name and size from its input
     *
     * @param widget {@link EmbeddedDisplayWidget}
     */
    private static void updateEmbeddedDisplayWidget ( final EmbeddedDisplayWidget widget ) {

        final String display_file = widget.propFile().getValue();
        final String resolved;

        try {
            resolved = ModelResourceUtil.resolveResource(widget.getTopDisplayModel(), display_file);
        } catch ( Exception ex ) {
            logger.log(Level.WARNING, "Cannot resolve resource " + display_file, ex);
            return;
        }

        try ( final InputStream bis = ModelResourceUtil.openResourceStream(resolved); ) {

            final ModelReader reader = new ModelReader(bis);
            final DisplayModel embedded_model = reader.readModel();
            final String name = embedded_model.getName();

            if ( !name.isEmpty() ) {
                widget.propName().setValue(name);
            }

            widget.propWidth().setValue(embedded_model.propWidth().getValue());
            widget.propHeight().setValue(embedded_model.propHeight().getValue());

        } catch ( Exception ex ) {
            logger.log(Level.WARNING, "Error updating embedded widget", ex);
        }

    }

    /**
     * Update a picture widget's size from image file.
     *
     * @param widget {@link PictureWidget}
     */
    private static void updatePictureWidgetSize ( final PictureWidget widget ) {

        final String imageFile = widget.propFile().getValue();

        try {

            final String filename = ModelResourceUtil.resolveResource(widget.getTopDisplayModel(), imageFile);
            if(filename.toLowerCase().endsWith("svg")) {
                // When loaded SVG resource is set to the size of containing widget
                return;
            }

            final Image image  = new Image(ModelResourceUtil.openResourceStream(filename));

            widget.propWidth().setValue((int) Math.round(image.getWidth()));
            widget.propHeight().setValue((int) Math.round(image.getHeight()));

        } catch ( Exception ex ) {
            logger.log(Level.WARNING, "Cannot obtain image size for " + imageFile, ex);
        }

    }

    /**
     * Update a symbol widget's size from image files, using the maximum
     * width and height from the widget's symbols.
     *
     * @param widget {@link PictureWidget}
     */
    private static void updateSymbolWidgetSize ( final SymbolWidget widget ) {

        Dimension2D maxSize = SymbolRepresentation.computeMaximumSize(widget);

        widget.propWidth().setValue((int) Math.round(maxSize.getWidth()));
        widget.propHeight().setValue((int) Math.round(maxSize.getHeight()));
        widget.propAutoSize().setValue(true);

    }

}
