/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.PredefinedColorMaps;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.javafx.rtplot.ColorMappingFunction;
import org.csstudio.javafx.rtplot.NamedColorMapping;
import org.csstudio.javafx.rtplot.NamedColorMappings;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.Window;
import javafx.util.Duration;

/** Represent model items in JavaFX toolkit
 *
 *  <p>The parent of each widget is either a {@link Group} or
 *  a {@link Pane}.
 *  Common ancestor of both is {@link Parent}, but note that other
 *  parent types (Region, ..) are not permitted.
 *
 *  <p>General scene layout:
 *  <pre>
 *  model_root    (ScrollPane)
 *   |
 *  scroll_body   (Group)
 *   |
 *  widget_parent (Pane)
 *  </pre>
 *
 *  <p>widget_parent:
 *  This is where the widgets of the model get represented.
 *  Its scaling factors are used to zoom.
 *  Also used to set the overall background color.
 *
 *  <p>scroll_body:
 *  Needed for scroll pane to use visual bounds, i.e. be aware of zoom.
 *  Otherwise scroll bars would enable/disable based on layout bounds,
 *  regardless of zoom.
 *  ( https://pixelduke.wordpress.com/2012/09/16/zooming-inside-a-scrollpane )
 *
 *  <p>model_root:
 *  Scroll pane for viewing a subsection of larger display.
 *  This is also the 'root' of the model-related scene.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXRepresentation extends ToolkitRepresentation<Parent, Node>
{
    /** ID of the model_root */
    public static final String MODEL_ROOT_ID = "model_root";

    /** Adjustment for scroll body size to prevent scroll bars from being displayed */
    // XXX Would be good to understand this value instead of 2-by-trial-and-error
    private static final int SCROLLBAR_ADJUST = 2;

    public static final String ACTIVE_MODEL = "_active_model";

    /** Zoom to fit display */
    public static final double ZOOM_ALL = -1.0;

    /** Zoom to fit display's width */
    public static final double ZOOM_WIDTH = -3.0;

    /** Zoom to fit display's height */
    public static final double ZOOM_HEIGHT = -2.0;

    /** Minimal Zoom level for Ctrl-Wheel */
    public static final double ZOOM_MIN = 0.1;

    /** Maximal Zoom level for Ctrl-Wheel */
    public static final double ZOOM_MAX = 10;

    /** Zoom factor for Ctrl-Wheel */
    public static final double ZOOM_FACTOR = 1.1;

    /** Width of the grid lines. */
    private static final float GRID_LINE_WIDTH = 0.222F;

    /** Update model size indicators (in edit mode) */
    private final WidgetPropertyListener<Integer> model_size_listener = ( p, o, n ) -> execute(this::updateModelSizeIndicators);

    /** Update background color, grid */
    private final UntypedWidgetPropertyListener background_listener = ( p, o, n ) -> execute(this::updateBackground);

    private Line horiz_bound, vert_bound;
    private Pane widget_parent;
    private Group scroll_body;
    private ScrollPane model_root;

    /** Called with zoom level text when zooming via Ctrl-Wheel */
    private Consumer<String> zoom_listener;

    /** For Middle Button (Wheel press) drag panning */
    private final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<>();

    /** Constructor
     *  @param edit_mode Edit mode?
     */
    public JFXRepresentation(final boolean edit_mode)
    {
        super(edit_mode);
    }

    @Override
    protected void initialize()
    {
        super.initialize();

        // Register default color mappings of model in JFX RTPlot
        for (PredefinedColorMaps.Predefined map : PredefinedColorMaps.PREDEFINED)
            NamedColorMappings.add(new NamedColorMapping(map.getName(), intensity  ->  ColorMappingFunction.getRGB(map.getColor(intensity))));
    }

    /** Create scrollpane etc. for hosting the model
     *
     *  @return ScrollPane
     *  @throws IllegalStateException if had already been called
     */
    final public ScrollPane createModelRoot()
    {
        if (model_root != null)
            throw new IllegalStateException("Already created model root");

        widget_parent = new Pane();
        scroll_body = new Group(widget_parent);

        if (isEditMode())
        {
            horiz_bound = new Line();
            horiz_bound.getStyleClass().add("display_model_bounds");
            horiz_bound.setStartX(0);

            vert_bound = new Line();
            vert_bound.getStyleClass().add("display_model_bounds");
            vert_bound.setStartY(0);

            scroll_body.getChildren().addAll(vert_bound, horiz_bound);
        }

        model_root = new ScrollPane(scroll_body);
        model_root.setId(MODEL_ROOT_ID);

        final InvalidationListener resized = prop -> handleViewportChanges();
        model_root.widthProperty().addListener(resized);
        model_root.heightProperty().addListener(resized);

        // Middle Button (Wheel press) drag panning started
        final EventHandler<MouseEvent> onMousePressedHandler = evt ->
        {
            if (evt.isMiddleButtonDown())
            {
                lastMouseCoordinates.set(new Point2D(evt.getX(), evt.getY()));
                scroll_body.setCursor(Cursor.CLOSED_HAND);
                evt.consume();
            }
        };
        if (isEditMode())
            scroll_body.addEventFilter(MouseEvent.MOUSE_PRESSED, onMousePressedHandler);
        else
            scroll_body.setOnMousePressed(onMousePressedHandler);

        // Middle Button (Wheel press) drag panning function
        final EventHandler<MouseEvent> onMouseDraggedHandler = evt ->
        {
            if (evt.isMiddleButtonDown())
            {
                double deltaX = evt.getX() - lastMouseCoordinates.get().getX();
                double extraWidth = scroll_body.getLayoutBounds().getWidth() - model_root.getViewportBounds().getWidth();
                double deltaH = deltaX * (model_root.getHmax() - model_root.getHmin()) / extraWidth;
                double desiredH = model_root.getHvalue() - deltaH;
                model_root.setHvalue(Math.max(0, Math.min(model_root.getHmax(), desiredH)));

                double deltaY = evt.getY() - lastMouseCoordinates.get().getY();
                double extraHeight = scroll_body.getLayoutBounds().getHeight() - model_root.getViewportBounds().getHeight();
                double deltaV = deltaY * (model_root.getHmax() - model_root.getHmin()) / extraHeight;
                double desiredV = model_root.getVvalue() - deltaV;
                model_root.setVvalue(Math.max(0, Math.min(model_root.getVmax(), desiredV)));
                evt.consume();
            }
        };
        if (isEditMode())
            scroll_body.addEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDraggedHandler);
        else
            scroll_body.setOnMouseDragged(onMouseDraggedHandler);

        // Middle Button (Wheel press) drag panning finished
        final EventHandler<MouseEvent> onMouseReleasedHandler = evt ->
        {
            if (scroll_body.getCursor() == Cursor.CLOSED_HAND)
            {
                scroll_body.setCursor(Cursor.DEFAULT);
                evt.consume();
            }
        };
        if (isEditMode())
            scroll_body.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleasedHandler);
        else
            scroll_body.setOnMouseReleased(onMouseReleasedHandler);

        // Ctrl-Wheel zoom gesture implementation.
        // Use EventFilter for Editor - more user friendly
        // Don't _filter_ for Runtime because some widgets (plot)
        // also handle the wheel
        if (isEditMode())
            model_root.addEventFilter(ScrollEvent.ANY, evt ->
            {
                if (evt.isShortcutDown())
                {
                    evt.consume();
                    doWheelZoom(evt.getDeltaY(), evt.getX(), evt.getY());
                }
            });
        else
            widget_parent.addEventHandler(ScrollEvent.ANY, evt ->
            {
                if (evt.isShortcutDown())
                {
                    evt.consume();
                    ScrollEvent gevt = evt.copyFor(model_root, scroll_body);
                    doWheelZoom(evt.getDeltaY(), gevt.getX(), gevt.getY());
                }
            });

        return model_root;
    }

    /** Ctrl-Wheel zoom gesture help function
     *  Zoom work function
     */
    private void doWheelZoom(final double delta, final double x, final double y)
    {
        final double zoom = getZoom();
	if (delta == 0)
	    return;
        if (zoom >= ZOOM_MAX && delta > 0)
            return;
        if (zoom <= ZOOM_MIN && delta < 0)
            return;

        final double zoomFactor = delta > 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
        double new_zoom = zoom * zoomFactor;
        new_zoom = new_zoom > ZOOM_MAX ? ZOOM_MAX : (new_zoom < ZOOM_MIN ? ZOOM_MIN : new_zoom);
        final double realFactor = new_zoom / zoom;

        Point2D scrollOffset = figureScrollOffset(scroll_body, model_root);

        // Set Zoom:
        // do not directly setValue(Double.toString(new_zoom * 100));
        // setText() only, otherwise it gets into an endless update due to getValue/setValue implementation in Editor. In Runtime was OK.
        // Drawback: return to a previous "combo driven" zoom level from any wheel level not possible directly (no value change in combo)
        setZoom(new_zoom);
        zoom_listener.accept(Integer.toString((int)(new_zoom * 100)) + " %");

        repositionScroller(scroll_body, model_root, realFactor, scrollOffset, new Point2D(x, y));
    }

    /** Ctrl-Wheel zoom gesture help function
     *  Store scroll offset of scrollContent (scroll_body Group) in a scroller (model_root ScrollPane) before zoom
     */
    private Point2D figureScrollOffset(final Node scrollContent, final ScrollPane scroller)
    {
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
        double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
        double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
        return new Point2D(scrollXOffset, scrollYOffset);
    }

    /** Ctrl-Wheel zoom gesture help function
     *  Repositioning scrollbars in scroller so that the zoom centre stays at mouse cursor
     */
    private void repositionScroller(final Node scrollContent, final ScrollPane scroller, final double scaleFactor, final Point2D scrollOffset, final Point2D mouse)
    {
        double scrollXOffset = scrollOffset.getX();
        double scrollYOffset = scrollOffset.getY();
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        if (extraWidth > 0)
        {
            double newScrollXOffset = (scaleFactor - 1) *  mouse.getX() + scaleFactor * scrollXOffset;
            scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
        }
        else
            scroller.setHvalue(scroller.getHmin());
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        if (extraHeight > 0)
        {
            double newScrollYOffset = (scaleFactor - 1) * mouse.getY() + scaleFactor * scrollYOffset;
            scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
        }
        else
            scroller.setHvalue(scroller.getHmin());
    }

    /** @param listener Will be called with zoom level text when using Ctrl-Wheel to zoom */
    public void setZoomListener(final Consumer<String> listener)
    {
        zoom_listener = listener;
    }

    /** @return Top-level scroll pane */
    final public ScrollPane getModelRoot()
    {
        return model_root;
    }

    /** @return Parent node of model widgets */
    final public Parent getModelParent()
    {
        return widget_parent;
    }

    /** @param scene Scene where style sheet for display builder is added */
    public static void setSceneStyle(final Scene scene)
    {
        // Fetch css relative to JFXRepresentation, not derived class
        final String css = JFXRepresentation.class.getResource("opibuilder.css").toExternalForm();
        Styles.set(scene, css);
    }

    /** Standard zoom levels */
    // Values and order of options similar to 'Word' on Mac
    public static final String[] ZOOM_LEVELS = new String[]
    {
        "200 %",
        "150 %",
        "125 %",
        "100 %",
        " 75 %",
        " 50 %",
        " 25 %",
        Messages.Zoom_Width,
        Messages.Zoom_Height,
        Messages.Zoom_All
    };

    /** Default zoom level */
    public static final String DEFAULT_ZOOM_LEVEL = ZOOM_LEVELS[3];

    /** @param level_spec "123 %" or Messages.Zoom_*
     *  @return Zoom spec actually used
     */
    final public String requestZoom(final String level_spec)
    {
        double zoom;
        if (level_spec.equalsIgnoreCase(Messages.Zoom_All))
            zoom = ZOOM_ALL;
        else if (level_spec.equalsIgnoreCase(Messages.Zoom_Width))
            zoom = ZOOM_WIDTH;
        else if (level_spec.equalsIgnoreCase(Messages.Zoom_Height))
            zoom = ZOOM_HEIGHT;
        else
        {   // Parse " 123 % "
            String number = level_spec.trim();
            if (number.endsWith("%"))
                number = number.substring(0, number.length()-1).trim();
            try
            {
                zoom = Double.parseDouble(number) / 100.0;
            }
            catch (NumberFormatException ex)
            {
                zoom = 1.0;
            }
        }
        zoom = setZoom(zoom);

        return getZoomLevelSpec();
    }

    /** Set zoom level
     *  @param zoom Zoom level: 1.0 for 100%, 0.5 for 50%, ZOOM_ALL, ZOOM_WIDTH, ZOOM_HEIGHT
     *  @return Zoom level actually used
     */
    private double setZoom(double zoom)
    {
        if (zoom <= 0.0)
        {   // Determine zoom to fit outline of display into available space
            final Bounds available = model_root.getLayoutBounds();
            final Bounds outline = widget_parent.getLayoutBounds();

            // 'outline' will wrap the actual widgets when the display
            // is larger than the available viewport.
            // So it can be used to zoom 'out'.
            // But when the viewport is much larger than the widget,
            // the JavaFX outline grows to fill the viewport,
            // so falling back to the self-declared model width and height
            // to zoom 'in'.
            // This requires displays to be created with
            // correct width/height properties.
            final double zoom_x, zoom_y;
            if (outline.getWidth() > available.getWidth())
                zoom_x = available.getWidth()  / outline.getWidth();
            else if (model.propWidth().getValue() > 0)
                zoom_x = available.getWidth()  / model.propWidth().getValue();
            else
                zoom_x = 1.0;

            if (outline.getHeight() > available.getHeight())
                zoom_y = available.getHeight() / outline.getHeight();
            else if (model.propHeight().getValue() > 0)
                zoom_y = available.getHeight() / model.propHeight().getValue();
            else
                zoom_y = 1.0;

            if (zoom == ZOOM_WIDTH)
                zoom = zoom_x;
            else if (zoom == ZOOM_HEIGHT)
                zoom = zoom_y;
            else // Assume ZOOM_ALL
                zoom = Math.min(zoom_x, zoom_y);
        }

        widget_parent.getTransforms().setAll(new Scale(zoom, zoom));
        // Appears similar to using this API:
        //     widget_parent.setScaleX(zoom);
        //     widget_parent.setScaleY(zoom);
        // but when resizing the window,
        // using setScaleX/Y results in sluggish updates,
        // sometimes shifting the content around
        // (top left origin of content no longer in top left corner of window).
        // Setting a Scale() transform does not exhibit that quirk,
        // maybe because both X and Y scaling are set 'at once'?

        if (isEditMode())
            updateModelSizeIndicators();

        return zoom;
    }

    /** @return Zoom factor, 1.0 for 1:1 */
    public double getZoom()
    {
        final List<Transform> transforms = widget_parent.getTransforms();
        if (transforms.isEmpty()  ||
            transforms.size() > 1 ||
            ! (transforms.get(0) instanceof Scale))
            return 1.0;
        // Have one 'scale'
        final Scale scale = (Scale) transforms.get(0);
        // Expect scaling in X == Y, but just in case return average
        return ( scale.getX() + scale.getY() ) / 2.0;
    }

    /** @return Zoom level "100%", suitable for calling {@link #requestZoom(String)} */
    public String getZoomLevelSpec()
    {
        return Math.round(getZoom()*100) + " %";
    }

    /** Obtain the 'children' of a Toolkit widget parent
     *  @param parent Parent that's either Group or Pane
     *  @return Children
     */
    public static ObservableList<Node> getChildren(final Parent parent)
    {
        if (parent instanceof Group)
            return ((Group)parent).getChildren();
        else if (parent instanceof Pane)
            return ((Pane)parent).getChildren();
        throw new IllegalArgumentException("Expecting Group or Pane, got " + parent);
    }

    @Override
    public ToolkitRepresentation<Parent, Node> openNewWindow(final DisplayModel model, Consumer<DisplayModel> close_handler) throws Exception
    {   // Use JFXStageRepresentation or RCP-based implementation
        throw new IllegalStateException("Not implemented");
    }

    /** Handle changes in on-screen size of this representation */
    private void handleViewportChanges()
    {
        final int view_width = (int) model_root.getWidth();
        final int view_height = (int) model_root.getHeight();

        final int model_width, model_height;
        final DisplayModel copy = model;
        if (copy == null)
            model_width = model_height = 0;
        else
        {
            model_width = copy.propWidth().getValue();
            model_height = copy.propHeight().getValue();
        }

        // If on-screen viewport is larger than model,
        // grow the scroll_body so that the complete area is
        // filled with the background color
        // and - in edit mode - the grid.
        // If the viewport is smaller, use the model's size
        // to get appropriate scrollbars.
        final int show_x = view_width >= model_width
                         ? view_width-SCROLLBAR_ADJUST
                         : model_width;
        final int show_y = view_height >= model_height
                         ? view_height-SCROLLBAR_ADJUST
                         : model_height;

        // Does not consider zooming.
        // If the widget_parent is zoomed 'out', e.g. 50%,
        // the widget_parent will only be half as large
        // as we specify here in pixels
        // -> Ignore. If user zooms out a lot, there'll be an
        //    area a gray area at the right and bottom of the display.
        //    But user tends to zoom out to see the complete set of widgets,
        //    so there is very little gray area.
        //        widget_parent.setMinWidth(show_x / zoom);
        //        widget_parent.setMinHeight(show_y / zoom);
        widget_parent.setMinSize(show_x, show_y);
    }

    /** Update lines that indicate model's size in edit mode */
    private void updateModelSizeIndicators()
    {
        int width = model.propWidth().getValue();
        int height = model.propHeight().getValue();

        final ObservableList<Transform> transforms = widget_parent.getTransforms();
        if (transforms.size() > 0  &&  transforms.get(0) instanceof Scale)
        {
            final Scale scale = (Scale) transforms.get(0);
            width  *= scale.getX();
            height *= scale.getY();
        }

        horiz_bound.setStartY(height);
        horiz_bound.setEndX(width);
        horiz_bound.setEndY(height);

        vert_bound.setStartX(width);
        vert_bound.setEndY(height);
        vert_bound.setEndX(width);
    }

    @Override
    public void representModel(final Parent root, final DisplayModel model) throws Exception
    {
        root.getProperties().put(ACTIVE_MODEL, model);

        // Add Nodes to the overall scene graph
        super.representModel(root, model);

        // Alternatively, could first add nodes to an off-scene parent,
        //   final Pane tmp_parent = new Pane();
        //   super.representModel(root, model);
        // and then add to the overall scene graph, to avoid parent/child
        // updates as each widget is represented.
        //   JFXRepresentation.getChildren(root).addAll(tmp_parent.getChildren());
        // No discernable performance gain, and in either case large displays
        // end with JFX spending time in Node.processCSS when first shown.

        // In edit mode, indicate overall bounds of the top-level model
        if (model.isTopDisplayModel())
        {
            // Listen to model background
            model.propBackgroundColor().addUntypedPropertyListener(background_listener);

            if (isEditMode())
            {
                // Track display size w/ initial update
                model.propWidth().addPropertyListener(model_size_listener);
                model.propHeight().addPropertyListener(model_size_listener);
                model_size_listener.propertyChanged(null, null, null);

                // Track grid changes w/ initial update
                model.propGridVisible().addUntypedPropertyListener(background_listener);
                model.propGridColor().addUntypedPropertyListener(background_listener);
                model.propGridStepX().addUntypedPropertyListener(background_listener);
                model.propGridStepY().addUntypedPropertyListener(background_listener);
            }
            background_listener.propertyChanged(null, null, null);
        }
    }

    @Override
    public Parent disposeRepresentation(final DisplayModel model)
    {
        if (model.isTopDisplayModel())
        {
            model.propBackgroundColor().removePropertyListener(background_listener);
            if (isEditMode())
            {
                model.propGridStepY().removePropertyListener(background_listener);
                model.propGridStepX().removePropertyListener(background_listener);
                model.propGridColor().removePropertyListener(background_listener);
                model.propGridVisible().removePropertyListener(background_listener);
                model.propHeight().removePropertyListener(model_size_listener);
                model.propWidth().removePropertyListener(model_size_listener);
            }
        }

        final Parent root = super.disposeRepresentation(model);
        root.getProperties().remove(ACTIVE_MODEL);
        return root;
    }

    @Override
    public void execute(final Runnable command)
    {   // If already on app thread, execute right away
        if (Platform.isFxApplicationThread())
            command.run();
        else
            Platform.runLater(command);
    }

    @Override
    public void showMessageDialog(final Widget widget, final String message)
    {
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        final CountDownLatch done = new CountDownLatch(1);
        execute( ()->
        {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            DialogHelper.positionDialog(alert, node, -100, -50);
            alert.setResizable(true);
            alert.setTitle(Messages.ShowMessageDialogTitle);
            // "header text" allows for larger content than the "content text"
            alert.setContentText(null);
            alert.setHeaderText(message);
            alert.initOwner(node.getScene().getWindow());
            alert.showAndWait();
            done.countDown();
        });
        try
        {
            done.await();
        }
        catch (InterruptedException ex)
        {
            // Ignore
        }
    }

    @Override
    public void showErrorDialog(final Widget widget, final String error)
    {
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        final CountDownLatch done = new CountDownLatch(1);
        execute( ()->
        {
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            DialogHelper.positionDialog(alert, node, -100, -50);
            alert.setResizable(true);
            alert.setTitle(Messages.ShowErrorDialogTitle);
            alert.setHeaderText(error);
            alert.initOwner(node.getScene().getWindow());
            alert.showAndWait();
            done.countDown();
        });
        try
        {
            done.await();
        }
        catch (InterruptedException ex)
        {
            // Ignore
        }
    }

    @Override
    public boolean showConfirmationDialog(final Widget widget, final String question)
    {
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        final CompletableFuture<Boolean> done = new CompletableFuture<>();
        execute( ()->
        {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            DialogHelper.positionDialog(alert, node, -100, -50);
            alert.setResizable(true);
            alert.setTitle(Messages.ShowConfirmationDialogTitle);
            alert.setHeaderText(question);
            // Setting "Yes", "No" buttons
            alert.getButtonTypes().clear();
            alert.getButtonTypes().add(ButtonType.YES);
            alert.getButtonTypes().add(ButtonType.NO);
            alert.initOwner(node.getScene().getWindow());
            final Optional<ButtonType> result = alert.showAndWait();
            // NOTE that button type OK/YES/APPLY checked in here must match!
            done.complete(result.isPresent()  &&  result.get() == ButtonType.YES);
        });
        try
        {
            return done.get();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Confirmation dialog ('" + question + "') failed", ex);
        }
        return false;
    }

    @Override
    public String showSelectionDialog(final Widget widget, final String title, final List<String> options)
    {
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        final CompletableFuture<String> done = new CompletableFuture<>();
        execute( ()->
        {
            final ChoiceDialog<String> dialog = new ChoiceDialog<>(null, options);
            DialogHelper.positionDialog(dialog, node, -100, -50);

            dialog.setHeaderText(title);
            final int lines = title.split("\n").length;
            dialog.setResizable(true);
            dialog.getDialogPane().setPrefHeight(50+25*lines);
            dialog.initOwner(node.getScene().getWindow());
            final Optional<String> result = dialog.showAndWait();
            done.complete(result.orElse(null));
        });
        try
        {
            return done.get();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Selection dialog ('" + title + ", ..') failed", ex);
        }
        return null;
    }

    @Override
    public String showPasswordDialog(final Widget widget, final String title, final String correct_password)
    {
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        final CompletableFuture<String> done = new CompletableFuture<>();
        execute( ()->
        {
            final PasswordDialog dialog = new PasswordDialog(title, correct_password);
            DialogHelper.positionDialog(dialog, node, -100, -50);
            dialog.initOwner(node.getScene().getWindow());
            final Optional<String> result = dialog.showAndWait();
            done.complete(result.orElse(null));
        });
        try
        {
            return done.get();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Password dialog ('" + title + ", ..') failed", ex);
        }
        return null;
    }

    @Override
    public String showSaveAsDialog(final Widget widget, final String initial_value)
    {
        File file = (initial_value != null) ? new File(initial_value) : null;
        final Window window = null;
        file = new SaveAsDialog().promptForFile(window, Messages.ShowSaveAsDialogTitle, file, FilenameSupport.file_extensions);
        return file == null ? null : file.toString();
    }

    /** Update background, using background color and grid information from model */
    private void updateBackground()
    {
        final WidgetColor background = model.propBackgroundColor().getValue();

        // Setting the "-fx-background:" of the root node propagates
        // to all child nodes in the scene graph.
        //
        //        if (isEditMode())
        //            model_root.setStyle("-fx-background: linear-gradient(from 0px 0px to 10px 10px, reflect, #D2A2A2 48%, #D2A2A2 2%, #D2D2A2 48% #D2D2A2 2%)");
        //        else
        //            model_root.setStyle("-fx-background: " + JFXUtil.webRGB(background));
        //
        // In edit mode, this results in error messages because the linear-gradient doesn't "work" for all nodes:
        //
        // javafx.scene.CssStyleHelper (calculateValue)
        // Caught java.lang.ClassCastException: javafx.scene.paint.LinearGradient cannot be cast to javafx.scene.paint.Color
        // while converting value for
        // '-fx-background-color' from rule '*.text-input' in stylesheet ..jfxrt.jar!/com/sun/javafx/scene/control/skin/modena/modena.bss
        // '-fx-effect' from rule '*.scroll-bar:vertical>*.increment-button>*.increment-arrow' in StyleSheet ...  jfxrt.jar!/com/sun/javafx/scene/control/skin/modena/modena.bss
        // '-fx-effect' from rule '*.scroll-bar:vertical>*.decrement-button>*.decrement-arrow' in stylesheet ... modena.bss
        // '-fx-effect' from rule '*.scroll-bar:horizontal>*.increment-button>*.increment-arrow' in stylesheet ... modena.bss
        //
        // In the runtime, the background color style is applied to for example the TextEntryRepresentation,
        // overriding its jfx_node.setBackground(..) setting.

        // Setting just the scroll body background to a plain color or grid image provides basic color control.
        // In edit mode, the horiz_bound, vert_bound lines and grid provide sufficient
        // visual indication of the display size.

        final Color backgroundColor = new Color(background.getRed(), background.getGreen(), background.getBlue());

        final boolean gridVisible = isEditMode() ? model.propGridVisible().getValue() : false;
        final int gridStepX = model.propGridStepX().getValue(),
                  gridStepY = model.propGridStepY().getValue();
        final WidgetColor grid_rgb = model.propGridColor().getValue();
        final Color gridColor = new Color(grid_rgb.getRed(), grid_rgb.getGreen(), grid_rgb.getBlue());

        final BufferedImage image = new BufferedImage(gridStepX, gridStepY, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.setBackground(backgroundColor);
        g2d.clearRect(0, 0, gridStepX, gridStepY);

        if (gridVisible)
        {
            g2d.setColor(gridColor);
            g2d.setStroke(new BasicStroke(GRID_LINE_WIDTH));
            g2d.drawLine(0, 0, gridStepX, 0);
            g2d.drawLine(0, 0, 0, gridStepY);
        }

        final WritableImage wimage = new WritableImage(gridStepX, gridStepY);
        SwingFXUtils.toFXImage(image, wimage);
        final ImagePattern pattern = new ImagePattern(wimage, 0, 0, gridStepX, gridStepY, false);
        widget_parent.setBackground(new Background(new BackgroundFill(pattern, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // Future for controlling the audio player
    private class AudioFuture implements Future<Boolean>
    {
        private volatile MediaPlayer player;

        AudioFuture(final MediaPlayer player)
        {
            this.player = player;
            // Player by default just stays in "PLAYING" state
            player.setOnEndOfMedia(() -> player.stop());
            player.play();
            logger.log(Level.INFO, "Playing " + this);
        }

        @Override
        public boolean isDone()
        {
            switch (player.getStatus())
            {
            case UNKNOWN:
            case READY:
            case PLAYING:
            case PAUSED:
            case STALLED:
                return false;
            case HALTED:
            case STOPPED:
            case DISPOSED:
            default:
                return true;
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            logger.log(Level.INFO, "Stopping " + this);
            final boolean stopped = !isDone();

            // TODO On Linux, playback of WAV doesn't work. Just stays in PLAYING state.
            // Worse: player.stop() as well as player.dispose() hang
            execute(() ->
            {
                player.stop();
            });

            return stopped;
        }

        @Override
        public boolean isCancelled()
        {
            return player.getStatus() == Status.STOPPED;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException
        {
            while (! isDone())
            {
                logger.log(Level.FINE, "Awaiting end " + this);
                Thread.sleep(100);
            }
            return !isCancelled();
        }

        @Override
        public Boolean get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException
        {
            final long end = System.currentTimeMillis() + unit.toMillis(timeout);
            while (! isDone())
            {
                logger.log(Level.FINE, "Awaiting end " + this);
                Thread.sleep(100);
                if (System.currentTimeMillis() >= end)
                    throw new TimeoutException("Timeout for " + this);
            }
            return !isCancelled();
        }

        @Override
        protected void finalize() throws Throwable
        {
            logger.log(Level.INFO, "Disposing " + this);
            player.dispose();
            player = null;
        }

        @Override
        public String toString()
        {
            final MediaPlayer copy = player;
            if (copy == null)
                return "Disposed audio player";
            return "Audio player for " + player.getMedia().getSource() + " (" + player.getStatus() + ")";
        }
    }

    @Override
    public Future<Boolean> playAudio(final String url)
    {
        final CompletableFuture<AudioFuture> result = new CompletableFuture<>();
        // Create on UI thread
        execute(() ->
        {
            try
            {
                final Media sound = new Media(url);
                final MediaPlayer player = new MediaPlayer(sound);
                player.setStartTime(Duration.ZERO);
                result.complete(new AudioFuture(player));
            }
            catch (Exception ex)
            {
                result.completeExceptionally(ex);
            }
        });

        try
        {
            return result.get();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Audio playback error for " + url, ex);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public void openFile(final String path) throws Exception
    {
        logger.log(Level.INFO, "Opening file " + path);

        execute(() ->
        {
            // First try opening with phoebus
            final File file = new File(path);
            if (file.exists()  &&
                ApplicationLauncherService.openFile(file, false, null))
                return;

            // Check for a resource
            try
            {
                final URI resource = new URI(path);
                if (ApplicationLauncherService.openResource(resource, false, null))
                    return;
            }
            catch (URISyntaxException ex)
            {
                // Ignore
            }

            // Fall back to OS, which might open in web browser
            PhoebusApplication.INSTANCE.getHostServices().showDocument(path);

            // AWT API has  Desktop.getDesktop().open(File),
            // but that results in hangup
            // https://github.com/ControlSystemStudio/phoebus/issues/433
        });
    }

    @Override
    public void openWebBrowser(final String url) throws Exception
    {
        logger.log(Level.INFO, "Opening web page " + url);
        execute(() -> PhoebusApplication.INSTANCE.getHostServices().showDocument(url));
    }

    @Override
    public void shutdown()
    {
        if (! widget_parent.getChildren().isEmpty())
            logger.log(Level.WARNING, "Display representation still contains items on shutdown: " + widget_parent.getChildren());

        widget_parent = null;
        model_root = null;
        scroll_body = null;
        zoom_listener = null;
        super.shutdown();
    }
}
