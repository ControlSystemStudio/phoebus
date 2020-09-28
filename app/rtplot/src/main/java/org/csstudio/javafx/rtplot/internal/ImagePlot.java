/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.ColorMappingFunction;
import org.csstudio.javafx.rtplot.Interpolation;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTImagePlotListener;
import org.csstudio.javafx.rtplot.RegionOfInterest;
import org.csstudio.javafx.rtplot.data.ValueRange;
import org.csstudio.javafx.rtplot.internal.undo.ChangeImageZoom;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.LinearScreenTransform;
import org.csstudio.javafx.rtplot.internal.util.Log10;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.IteratorNumber;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VImageType;
import org.phoebus.ui.javafx.BufferUtil;
import org.phoebus.ui.javafx.ChildCare;
import org.phoebus.ui.javafx.DoubleBuffer;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.javafx.Tracker;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

/** Plot for an image
 *
 *  <p>Displays the intensity of values from a {@link ListNumber}
 *  as an image.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImagePlot extends PlotCanvasBase
{
    /** Background color */
    private volatile Color background = Color.WHITE;

    /** Axis range for 'full' image */
    private volatile double min_x = 0.0, max_x = 100.0, min_y = 0.0, max_y = 100.0;

    /** X Axis */
    private final AxisPart<Double> x_axis;

    /** Y Axis */
    private final YAxisImpl<Double> y_axis;

    /** Area used by the image */
    private volatile Rectangle image_area = new Rectangle(0, 0, 0, 0);

    /** Color bar Axis */
    private final YAxisImpl<Double> colorbar_axis;

    /** Area used by the color bar. <code>null</code> if not visible */
    private volatile Rectangle colorbar_area = null;

    /** Image data size */
    private volatile int data_width = 0, data_height = 0;

    /** Interpolation from image to screen pixels */
    private volatile Interpolation interpolation = Interpolation.AUTOMATIC;

    /** Auto-scale the data range? */
    private volatile boolean autoscale = true;

    /** Image data range */
    private volatile double min=0.0, max=1.0;

    /** Show color bar? */
    private volatile boolean show_colormap = true;

    /** Color bar size */
    private volatile int colorbar_size = 40;

    /** Mapping of value 0..1 to color */
    private volatile ColorMappingFunction color_mapping = ColorMappingFunction.GRAYSCALE;

    /** Image data */
    private volatile ListNumber image_data = null;

    /** Is 'image_data' meant to be treated as 'unsigned'? */
    private volatile boolean unsigned_data = false;

    /** Color map: use ColorMap or RGB pixels? */
    private volatile VImageType vimage_type = VImageType.TYPE_MONO;

    /** Regions of interest */
    private final List<RegionOfInterest> rois = new CopyOnWriteArrayList<>();

    /** Show crosshair marker, positioned on click?
     *  Otherwise update cursor listener with each mouse movement.
     */
    private volatile boolean crosshair = false;

    /** Crosshair position (when using crosshair) */
    private volatile Point2D crosshair_position = null;

    private volatile RTImagePlotListener plot_listener = null;

    private Tracker roi_tracker;

    /** Initial axis ranges for panning */
    private AxisRange<Double> mouse_start_x_range, mouse_start_y_range;

    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     */
    public ImagePlot(final boolean active)
    {
        super(active);
        x_axis = new HorizontalNumericAxis("X", plot_part_listener);
        y_axis = new YAxisImpl<>("Y", plot_part_listener);
        colorbar_axis =  new YAxisImpl<>("", plot_part_listener);

        x_axis.setValueRange(min_x, max_x);
        y_axis.setValueRange(min_y, max_y);

        if (active)
        {
            setOnMousePressed(this::mouseDown);
            setOnMouseMoved(this::mouseMove);
            setOnMouseDragged(this::mouseMove);
            setOnMouseReleased(this::mouseUp);
            setOnMouseExited(this::mouseExit);
        }
    }

    /** @param plot_listener Plot listener */
    public void setListener(final RTImagePlotListener plot_listener)
    {
        if (this.plot_listener != null)
            throw new IllegalStateException("Listener already set");
        this.plot_listener = plot_listener;
    }

    /** @param color Background color */
    public void setBackground(final Color color)
    {
        background  = color;
    }

    /** @param interpolation How to interpolate from image to screen pixels */
    public void setInterpolation(final Interpolation interpolation)
    {
        this.interpolation = interpolation;
        requestUpdate();
    }

    /** @return Auto-scale the color mapping? */
    public boolean isAutoscale()
    {
        return autoscale;
    }

    /** @param autoscale  Auto-scale the color mapping? */
    public void setAutoscale(final boolean autoscale)
    {
        this.autoscale = autoscale;
        requestUpdate();
    }

    /** @return Use log scale for color mapping? */
    public boolean isLogscale()
    {
        return colorbar_axis.isLogarithmic();
    }

    /** @param logscale Use log scale for color mapping? */
    public void setLogscale(final boolean logscale)
    {
        colorbar_axis.setLogarithmic(logscale);
        requestUpdate();
    }

    /** Get color mapping value range
      * @return {@link ValueRange}
     */
    public ValueRange getValueRange()
    {
        return new ValueRange(min, max);
    }

    /** Set color mapping value range
     *  @param min
     *  @param max
     */
    public void setValueRange(final double min, final double max)
    {
        this.min = min;
        this.max = max;
        requestUpdate();
    }

    /** @param color_mapping {@link ColorMappingFunction} */
    public void setColorMapping(final ColorMappingFunction color_mapping)
    {
        this.color_mapping = color_mapping;
        requestUpdate();
    }

    /** @return {@link ColorMappingFunction} */
    public ColorMappingFunction getColorMapping()
    {
        return color_mapping;
    }

    /** Set axis range for 'full' image
     *  @param min_x
     *  @param max_x
     *  @param min_y
     *  @param max_y
     */
    public void setAxisRange(final double min_x, final double max_x,
                             final double min_y, final double max_y)
    {
        this.min_x = min_x;
        this.max_x = max_x;
        this.min_y = min_y;
        this.max_y = max_y;
        x_axis.setValueRange(min_x, max_x);
        y_axis.setValueRange(min_y, max_y);
    }

    /** <b>Note: May offer too much access
     *  @return X Axis
     */
    public Axis<Double> getXAxis()
    {
        return x_axis;
    }

    /** <b>Note: May offer too much access
     *  @return Y Axis
     */
    public Axis<Double> getYAxis()
    {
        return y_axis;
    }

    /** @return Show color map? */
    public boolean isShowingColorMap()
    {
        return show_colormap;
    }

    /** @param show Show color map? */
    public void showColorMap(final boolean show)
    {
        show_colormap = show;
        requestLayout();
    }

    /** @param size Color bar size in pixels */
    public void setColorMapSize(final int size)
    {
        colorbar_size = size;
        requestLayout();
    }

    /** @param size Color bar size in pixels */
    public void setColorMapFont(final Font font)
    {
        colorbar_axis.setScaleFont(font);
        requestLayout();
    }

    /** @param foreground Color bar text color. */
    public void setColorMapForeground(final javafx.scene.paint.Color foreground)
    {
        colorbar_axis.setColor(foreground);
        requestRedraw();
    }

    /** @param show Show crosshair, moved on click?
     *              Or update cursor listener with each mouse move,
     *              not showing a persistent crosshair?
     */
    public void showCrosshair(final boolean show)
    {
        if (show == crosshair)
            return;
        crosshair = show;
        requestRedraw();
    }

    /** @return Is crosshair enabled? */
    public boolean isCrosshairVisible()
    {
        return crosshair;
    }

    /** Add region of interest
     *  @param name
     *  @param color
     *  @param visible
     *  @param interactive
     *  @return {@link RegionOfInterest}
     */
    public RegionOfInterest addROI(final String name, final javafx.scene.paint.Color color,
                                   final boolean visible, final boolean interactive)
    {   // Return a ROI that triggers a redraw as it's changed
        final RegionOfInterest roi = new RegionOfInterest(name, color, visible, interactive, 0, 0, 10, 10)
        {
            @Override
            public void setImage(final Image image)
            {
                super.setImage(image);
                requestUpdate();
            }

            @Override
            public void setVisible(boolean visible)
            {
                super.setVisible(visible);
                requestUpdate();
            }

            @Override
            public void setRegion(Rectangle2D region)
            {
                super.setRegion(region);
                requestUpdate();
            }
        };
        rois.add(roi);
        return roi;
    }

    /** @return Regions of interest */
    public List<RegionOfInterest> getROIs()
    {
        return rois;
    }

    /** @param index Index of R.O.I. to remove
     *  @throws IndexOutOfBoundsException
     */
    public void removeROI(final int index)
    {
        rois.remove(index);
        requestUpdate();
    }

    /** Set the data to display
     *  @param width Number of elements in one 'row' of data
     *  @param height Number of data rows
     *  @param data Image elements, starting in 'top left' corner,
     *              proceeding along the row, then to next rows
     *  @param unsigned Is the data meant to be treated as 'unsigned'
     */
    public void setValue(final int width, final int height, final ListNumber data, final boolean unsigned)
    {
    	setValue(width, height, data, unsigned, VImageType.TYPE_MONO);
    }
    public void setValue(final int width, final int height, final ListNumber data, final boolean unsigned, VImageType type)
    {
        data_width = width;
        data_height = height;
        image_data = data;
        vimage_type = type;
        unsigned_data = unsigned;
        requestUpdate();
    }

    /** Compute layout of plot components */
    private void computeLayout(final Graphics2D gc, final Rectangle bounds,
                               final double min, final double max)
    {
        logger.log(Level.FINE, "computeLayout");

        // X Axis as high as desired. Width will depend on Y axis.
        final int x_axis_height = x_axis.getDesiredPixelSize(bounds, gc);
        final int y_axis_height = bounds.height - x_axis_height;
        final int y_axis_width  = y_axis.getDesiredPixelSize(new Rectangle(0, 0, bounds.width, y_axis_height), gc);

        image_area = new Rectangle(y_axis_width, 0, bounds.width - y_axis_width, bounds.height - x_axis_height);

        // Color bar requested and there's room?
        if (show_colormap)
        {
            colorbar_area = new Rectangle(bounds.width - colorbar_size, colorbar_size, colorbar_size, image_area.height-2*colorbar_size);

            final int cb_axis_width = colorbar_axis.getDesiredPixelSize(colorbar_area, gc);
            colorbar_axis.setBounds(colorbar_area.x, colorbar_area.y, cb_axis_width, colorbar_area.height);
            colorbar_area.x += cb_axis_width;
            colorbar_area.width -= cb_axis_width;

            if (image_area.width > cb_axis_width + colorbar_area.width)
                image_area.width -= cb_axis_width + colorbar_area.width;
            else
                colorbar_area = null;
        }
        else
            colorbar_area = null;

        y_axis.setBounds(0, 0, y_axis_width, image_area.height);
        x_axis.setBounds(image_area.x, image_area.height, image_area.width, x_axis_height);
    }

    // Functionals for reading the next Number as an unsigned value
    private static double getUnsignedByte(final IteratorNumber iter)
    {
        return Byte.toUnsignedInt(iter.nextByte());
    }

    private static double getUnsignedShort(final IteratorNumber iter)
    {
        return Short.toUnsignedInt(iter.nextShort());
    }

    private static double getUnsignedInt(final IteratorNumber iter)
    {
        return Integer.toUnsignedLong(iter.nextInt());
    }

    // Functionals for RGB
    private static int getUByteForRGB(final IteratorNumber iter)
    {
        return Byte.toUnsignedInt(iter.nextByte());
    }

    private static int getUShortForRGB(final IteratorNumber iter)
    {
        return Short.toUnsignedInt(iter.nextShort());
    }

    private static int getUIntForRGB(final IteratorNumber iter)
    {
        return iter.nextInt();
    }

    private static int getByteForRGB(final IteratorNumber iter)
    {
        return Byte.toUnsignedInt((byte)(iter.nextByte()+Byte.MIN_VALUE));
    }

    private static int getShortForRGB(final IteratorNumber iter)
    {
        return Short.toUnsignedInt((short)(iter.nextShort()+Short.MIN_VALUE));
    }

    private static int getIntForRGB(final IteratorNumber iter)
    {
        return iter.nextInt()+Integer.MIN_VALUE;
    }

    /** Buffers used to create the next image buffer */
    private final DoubleBuffer buffers = new DoubleBuffer();

    /** Draw all components into image buffer */
    @Override
    protected BufferedImage updateImageBuffer()
    {
        // Would like to use JFX WritableImage,
        // but rendering problem on Linux (sandbox.ImageScaling),
        // and no way to disable the color interpolation that 'smears'
        // the scaled image.
        // (http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8091877).
        // So image is prepared in AWT and then converted to JFX
        logger.log(Level.FINE, "updateImageBuffer");

        final Rectangle area_copy = area;
        if (area_copy.width <= 0  ||  area_copy.height <= 0)
            return null;

        final BufferUtil buffer = buffers.getBufferedImage(area_copy.width, area_copy.height);
        if (buffer == null)
            return null;
        final BufferedImage image = buffer.getImage();
        final Graphics2D gc = buffer.getGraphics();

        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        // Get safe copy of the data
        // (not synchronized, i.e. width vs. data may be inconsistent,
        //  but at least data won't change within this method)
        final int data_width = this.data_width, data_height = this.data_height;
        final ListNumber numbers = this.image_data;
        final boolean unsigned = this.unsigned_data;
        double min = this.min, max = this.max;
        final VImageType type = this.vimage_type;
        final ColorMappingFunction color_mapping = this.color_mapping;

        ToDoubleFunction<IteratorNumber> next_sample_func = IteratorNumber::nextDouble;
    	boolean isRGB = type == VImageType.TYPE_RGB1 || type == VImageType.TYPE_RGB2 || type == VImageType.TYPE_RGB3;
    	@SuppressWarnings("unchecked")
		final ToIntFunction<IteratorNumber> next_rgb [] = new ToIntFunction [3];
        if (numbers != null)
        {
            if (isRGB)
            {
        		if (numbers instanceof ArrayShort)
        		{
        			if (unsigned)
        			{
            			next_rgb[0] = (iter) -> getUShortForRGB(iter) << 8 & 0xFF0000;
            			next_rgb[1] = (iter) -> getUShortForRGB(iter) & 0xFF00;
            			next_rgb[2] = (iter) -> getUShortForRGB(iter) >>> 8;
        			}
    				else
    				{
            			next_rgb[0] = (iter) -> getShortForRGB(iter) << 8 & 0xFF0000;
            			next_rgb[1] = (iter) -> getShortForRGB(iter) & 0xFF00;
            			next_rgb[2] = (iter) -> getShortForRGB(iter) >>> 8;
    				}
        		}
        		if (numbers instanceof ArrayInteger)
        		{
        			if (unsigned)
        			{
            			next_rgb[0] = (iter) -> getUIntForRGB(iter) >>> 8 & 0xFF0000;
            			next_rgb[1] = (iter) -> getUIntForRGB(iter) >>> 16 & 0xFF00;
            			next_rgb[2] = (iter) -> getUIntForRGB(iter) >>> 24;
        			}
    				else
    				{
            			next_rgb[0] = (iter) -> getIntForRGB(iter) >>> 8 & 0xFF0000;
            			next_rgb[1] = (iter) -> getIntForRGB(iter) >>> 16 & 0xFF00;
            			next_rgb[2] = (iter) -> getIntForRGB(iter) >>> 24;
    				}
        		}
        		else
        		{
            		if (!(numbers instanceof ArrayByte))
            			logger.log(Level.WARNING, "Cannot handle rgb1 image data of type " + numbers.getClass().getName());
        			if (unsigned)
        			{
            			next_rgb[0] = (iter) -> getUByteForRGB(iter) << 16;
            			next_rgb[1] = (iter) -> getUByteForRGB(iter) << 8;
            			next_rgb[2] = (iter) -> getUByteForRGB(iter);
        			}
    				else
    				{
            			next_rgb[0] = (iter) -> getByteForRGB(iter) << 16;
            			next_rgb[1] = (iter) -> getByteForRGB(iter) << 8;
            			next_rgb[2] = (iter) -> getByteForRGB(iter);
    				}
        		}
            }
            else //is not RGB
            {
	            if (unsigned)
	            {
	                if (numbers instanceof ArrayShort)
	                    next_sample_func = ImagePlot::getUnsignedShort;
	                else if (numbers instanceof ArrayByte)
	                    next_sample_func = ImagePlot::getUnsignedByte;
	                else if (numbers instanceof ArrayInteger)
	                    next_sample_func = ImagePlot::getUnsignedInt;
	                else
	                    logger.log(Level.WARNING, "Cannot handle unsigned data of type " + numbers.getClass().getName());
	            }

	            if (autoscale)
	            {   // Compute min..max before layout of color bar
	                final IteratorNumber iter = numbers.iterator();
	                min = Double.MAX_VALUE;
	                max = Double.NEGATIVE_INFINITY;
	                while (iter.hasNext())
	                {
	                    final double sample = next_sample_func.applyAsDouble(iter);
	                    if (sample > max)
	                        max = sample;
	                    if (sample < min)
	                        min = sample;
	                }
	                logger.log(Level.FINE, "Autoscale range {0} .. {1}", new Object[] { min, max });
	            }
            }
        }

        // If log, min needs to be > 0
        if (colorbar_axis.isLogarithmic()  &&  min <= 0.0)
            min = 0.001;  // arbitrary minimum
        colorbar_axis.setValueRange(min, max);
        if (need_layout.getAndSet(false))
            computeLayout(gc, area_copy, min, max);

        // Fill with a 'background' color
        gc.setColor(background);
        gc.fillRect(0, 0, area_copy.width, area_copy.height);

        // Debug: Show exact outer rim
//        gc.setColor(Color.RED);
//        gc.drawLine(0, 0, image_area.width-1, 0);
//        gc.drawLine(image_area.width-1, 0, image_area.width-1, image_area.height-1);
//        gc.drawLine(image_area.width-1, image_area.height-1, 0, image_area.height-1);
//        gc.drawLine(0, image_area.height-1, 0, 0);

        if (numbers != null)
        {
            // Paint the image
            gc.setClip(image_area.x, image_area.y, image_area.width, image_area.height);
            final Object image_or_error =  !isRGB ?
            		drawData(data_width, data_height, numbers, next_sample_func, min, max, color_mapping) :
        			drawDataRGB(data_width, data_height, numbers, next_rgb, type);
            if (image_or_error instanceof BufferedImage)
            {
                final BufferedImage unscaled = (BufferedImage) image_or_error;
                // Transform from full axis range into data range,
                // using the current 'zoom' state of each axis
                final LinearScreenTransform t = new LinearScreenTransform();
                AxisRange<Double> zoomed = x_axis.getValueRange();
                t.config(min_x, max_x, 0, data_width);
                // Round down .. up to always cover the image_area
                final int src_x1 = Math.max(0,          (int)t.transform(zoomed.getLow()));
                final int src_x2 = Math.min(data_width, (int)(t.transform(zoomed.getHigh()) + 1));

                // Pixels of the image need to be aligned to their axis location,
                // especially when zoomed way in and the pixels are huge.
                // Turn pixel back into axis value, and then determine its destination on screen.
                final int dst_x1 = x_axis.getScreenCoord(t.inverse(src_x1));
                final int dst_x2 = x_axis.getScreenCoord(t.inverse(src_x2));

                // For Y axis, min_y == bottom == data_height
                zoomed = y_axis.getValueRange();
                t.config(min_y, max_y, data_height, 0);
                final int src_y1 = Math.max(0,           (int) t.transform(zoomed.getHigh()));
                final int src_y2 = Math.min(data_height, (int) (t.transform(zoomed.getLow() ) + 1));
                final int dst_y1 = y_axis.getScreenCoord(t.inverse(src_y1));
                final int dst_y2 = y_axis.getScreenCoord(t.inverse(src_y2));

                switch (interpolation)
                {
                case NONE:
                    gc.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    break;
                case INTERPOLATE:
                    gc.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    break;
                default:
                    // If image is smaller than screen area, show the actual pixels
                    if ((src_x2-src_x1) < image_area.width  &&   (src_y2-src_y1) < image_area.height)
                        gc.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    else
                        // If image is larger than screen area, use best possible interpolation
                        // to avoid artifacts from statistically picking some specific nearest neighbor
                        gc.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                }
                gc.drawImage(unscaled,
                             dst_x1, dst_y1, dst_x2, dst_y2,
                             src_x1,  src_y1,  src_x2,  src_y2,
                             /* ImageObserver */ null);
            }
            else
            {
                gc.setColor(Color.RED);
                gc.setFont(x_axis.label_font);
                gc.drawString(Objects.toString(image_or_error), image_area.x+10, image_area.y + 20);
            }
            gc.setClip(0, 0, area_copy.width, area_copy.height);
        }

        // Axes
        y_axis.paint(gc, image_area);
        x_axis.paint(gc, image_area);

        // Color bar
        if (colorbar_area != null)
        {
            final BufferedImage bar = drawColorBar(min, max, color_mapping);
            gc.drawImage(bar, colorbar_area.x, colorbar_area.y, colorbar_area.width, colorbar_area.height, null);
            colorbar_axis.paint(gc, colorbar_area);
        }

        // ROI uses X axis font
        gc.setFont(x_axis.label_font);
        gc.setClip(image_area.x, image_area.y, image_area.width, image_area.height);
        for (RegionOfInterest roi : rois)
            drawROI(gc, roi);
        gc.setClip(0, 0, area_copy.width, area_copy.height);

        return image;
    }

    /** @param roi RegionOfInterest
     *  @return Screen coordinates
     */
    private Rectangle roiToScreen(final RegionOfInterest roi)
    {
        final int x0 = x_axis.getScreenCoord(roi.getRegion().getMinX()),
                  y0 = y_axis.getScreenCoord(roi.getRegion().getMinY()),
                  x1 = x_axis.getScreenCoord(roi.getRegion().getMaxX()),
                  y1 = y_axis.getScreenCoord(roi.getRegion().getMaxY());
        final int x = Math.min(x0, x1);
        final int y = Math.min(y0, y1);
        return new Rectangle(x, y, Math.abs(x1-x0), Math.abs(y1-y0));
    }

    /** @param roi RegionOfInterest to move to new location
     *  @param screen_pos Screen position, will be converted into axes' values
     */
    private void updateRoiFromScreen(final int index, final Rectangle2D screen_pos)
    {
        final RegionOfInterest roi = rois.get(index);
        // Convert screen position to axis values
        final double x0 = x_axis.getValue((int)screen_pos.getMinX()),
                     y0 = y_axis.getValue((int)screen_pos.getMinY()),
                     x1 = x_axis.getValue((int)screen_pos.getMaxX()),
                     y1 = y_axis.getValue((int)screen_pos.getMaxY());
        final double x = Math.min(x0, x1);
        final double y = Math.min(y0, y1);
        final Rectangle2D region = new Rectangle2D(x, y, Math.abs(x1-x0), Math.abs(y1-y0));
        roi.setRegion(region);
        requestUpdate();

        // Notify listener of ROI change
        final RTImagePlotListener listener = plot_listener;
        if (listener != null)
            listener.changedROI(index, roi.getName(), roi.getRegion());
    }

    /** If there is a ROI tracker, remove it */
    public void removeROITracker()
    {
        if (roi_tracker != null)
        {
            ChildCare.removeChild(getParent(), roi_tracker);
            roi_tracker = null;
        }
    }

    /** @param gc GC for off-screen image
     *  @param roi RegionOfInterest to draw
     */
    private void drawROI(final Graphics2D gc, final RegionOfInterest roi)
    {
        if (! roi.isVisible())
            return;

        final Color color = GraphicsUtils.convert(roi.getColor());
        final java.awt.geom.Rectangle2D rect = roiToScreen(roi);
        final Image image = roi.getImage();
        if (image == null)
        {
            gc.setColor(color);
            gc.drawRect((int)rect.getMinX(), (int)rect.getMinY(), (int)rect.getWidth()-1, (int)rect.getHeight()-1);
            if (rect.getWidth() > 3  &&  rect.getHeight() > 3)
            {
                // Determine contrasting color for outline
                gc.setColor(getBrightness(color) > BRIGHT_THRESHOLD
                            ? color.darker()
                            : color.brighter());
                // Inside, outside
                gc.drawRect((int)rect.getMinX()+1, (int)rect.getMinY()+1, (int)rect.getWidth()-3, (int)rect.getHeight()-3);
                gc.drawRect((int)rect.getMinX()-1, (int)rect.getMinY()-1, (int)rect.getWidth()+1, (int)rect.getHeight()+1);
            }
        }
        else
        {
            final BufferedImage awt_image = SwingFXUtils.fromFXImage(image, null);
            gc.drawImage(awt_image, (int)rect.getMinX(), (int)rect.getMinY(), (int)rect.getWidth(), (int)rect.getHeight(), null);
        }

        gc.setColor(color);
        gc.drawString(roi.getName(), (int)rect.getMinX(), (int)rect.getMinY()-2);
    }

    // Brightness weightings from BOY
    // https://github.com/ControlSystemStudio/cs-studio/blob/master/applications/opibuilder/opibuilder-plugins/org.csstudio.swt.widgets/src/org/csstudio/swt/widgets/figures/LEDFigure.java
    /** Threshold for considering a color 'bright', suggesting a darker outline */
    public static final double BRIGHT_THRESHOLD = 105000;

    /** @param color Color
     *  @return Weighed brightness
     */
    public static double getBrightness(final Color color)
    {
        return color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114;
    }

    /** @param min Value ..
     *  @param max .. range
     *  @param color_mapping Color mapping info
     *  @return Off-screen image
     */
    private BufferedImage drawColorBar(final double min, final double max, final ColorMappingFunction color_mapping)
    {
        final BufferedImage image = new BufferedImage(1, 256, BufferedImage.TYPE_INT_ARGB);
        final int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int value=0; value<256; ++value)
            data[value] = color_mapping.getRGB((255-value)/255.0);
        return image;
    }

    // private static long runs = 0, avg_nano = 0;

    /** Buffers used for the data (to be merged/scaled into the complete image) */
    private final DoubleBuffer data_buffers = new DoubleBuffer();

    /** @param data_width
     *  @param data_height
     *  @param numbers
     *  @param next_sample_func
     *  @param min
     *  @param max
     *  @param color_mapping
     *  @return {@link BufferedImage}, sized to match data or String with error message
     */
    private Object drawData(final int data_width, final int data_height, final ListNumber numbers,
                                   final ToDoubleFunction<IteratorNumber> next_sample_func,
                                   double min, double max, final ColorMappingFunction color_mapping)
    {
        // final long start = System.nanoTime();

        if (data_width <= 0  ||  data_height <= 0)
        {
            // With invalid size, cannot create a BufferedImage, not even for the error message
            return "Cannot draw image sized " + data_width + " x " + data_height;
        }

        final BufferUtil buffer = data_buffers.getBufferedImage(data_width, data_height);
        if (buffer == null)
            return "Cannot get buffer";
        final BufferedImage image = buffer.getImage();
        if (numbers.size() < data_width * data_height)
            return "Image sized " + data_width + " x " + data_height +
                   " received only " + numbers.size() + " data samples";

        if (!  (min < max))  // Implies min and max being finite, not-NaN
        {
            logger.log(Level.FINE, "Invalid value range {0} .. {1}", new Object[] { min, max });
            min = 0.0;
            max = 1.0;
        }

        // Direct access to 'int' pixels in data buffer is about twice as fast as access
        // via image.setRGB(x, y, color.getRGB()),
        // which in turn is about 3x faster than drawLine or fillRect.
        // Creating a byte[] with one byte per pixel and ColorModel based on color map is fastest,
        // but only 8 bits per pixel instead of 8 bits each for R, G and B isn't enough resolution.
        // Rounding of values into 8 bits creates artifacts.
        final int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        final IteratorNumber iter = numbers.iterator();
        int idx = 0;

        if (colorbar_axis.isLogarithmic())
        {
            final double lmin = Log10.log10(min),
                         lmax = Log10.log10(max),
                         span = lmax - lmin;
            for (int y=0; y<data_height; ++y)
                for (int x=0; x<data_width; ++x)
                {
                    final double sample = Log10.log10(next_sample_func.applyAsDouble(iter));
                    double scaled = (sample - lmin) / span;
                    if (scaled < 0.0)
                        scaled = 0;
                    else if (scaled > 1.0)
                        scaled = 1.0;
                    data[idx++] = color_mapping.getRGB(scaled);
                }
        }
        else
        {
            final double span = max - min;
            for (int y=0; y<data_height; ++y)
                for (int x=0; x<data_width; ++x)
                {
                    final double sample = next_sample_func.applyAsDouble(iter);
                    double scaled = (sample - min) / span;
                    if (scaled < 0.0)
                        scaled = 0;
                    else if (scaled > 1.0)
                        scaled = 1.0;
                    data[idx++] = color_mapping.getRGB(scaled);
                }
        }
        // final long nano = System.nanoTime() - start;
        // avg_nano = (avg_nano*3 + nano)/4;
        // if (++runs > 100)
        // {
        //     runs = 0;
        //    System.out.println(avg_nano/1e6 + " ms");
        // }

        return image;
    }

    /** @param data_width
     *  @param data_height
     *  @param numbers
     *  @param next_rgbs
     *  @param type RGB type (RGB1, RGB2, or RGB3)
     *  @return {@link BufferedImage}, sized to match data
     */
    private Object drawDataRGB(final int data_width, final int data_height, final ListNumber numbers,
                               final ToIntFunction<IteratorNumber> next_rgbs [], final VImageType type)
    {
        if (data_width <= 0  ||  data_height <= 0)
        {
            // With invalid size, cannot create a BufferedImage, not even for the error message
            return "Cannot draw image sized " + data_width + " x " + data_height;
        }

        final BufferUtil buffer = data_buffers.getBufferedImage(data_width, data_height);
        if (buffer == null)
            return "Cannot get buffer";
        final BufferedImage image = buffer.getImage();
        if (numbers.size() < data_width * data_height * 3)
            return "RGB image sized " + data_width + " x " + data_height +
                   " received only " + numbers.size() + " data samples";

        // Using direct access to 'int' pixels in data buffer for speed. See other drawData() for details.
        final int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        final IteratorNumber iter = numbers.iterator();

        switch(type)
        {
	        case TYPE_RGB2:
	        	for (int y_times_width = 0; y_times_width < data_height*data_width; y_times_width += data_width)
	        	{
	        		//red
        			for (int x = 0; x < data_width; ++x)
        				data[y_times_width + x] = 0xFF000000 | next_rgbs[0].applyAsInt(iter);
        			//green
        			for (int x = 0; x < data_width; ++x)
        				data[y_times_width + x] |= next_rgbs[1].applyAsInt(iter);
        			//blue
        			for (int x = 0; x < data_width; ++x)
        				data[y_times_width + x] |= next_rgbs[2].applyAsInt(iter);
	        	}
	        	break;
	        case TYPE_RGB3:
	        	//red
        		for (int i = 0; i < data_height*data_width; ++i)
        			data[i] = 0xFF000000 | next_rgbs[0].applyAsInt(iter);
        		//green
        		for (int i = 0; i < data_height*data_width; ++i)
        			data[i] |= next_rgbs[1].applyAsInt(iter);
    			//blue
        		for (int i = 0; i < data_height*data_width; ++i)
        			data[i] |= next_rgbs[2].applyAsInt(iter);
	        	break;
        	default:
        		throw new IllegalArgumentException("Image type must be an RGB type");
        		//no "break;"
	        case TYPE_RGB1:
	        	for (int i = 0; i < data_height*data_width; ++i)
	            	data[i] = 0xFF000000 | next_rgbs[0].applyAsInt(iter) | next_rgbs[1].applyAsInt(iter) | next_rgbs[2].applyAsInt(iter);
        }

        return image;
    }

    /** Draw visual feedback (rubber band rectangle etc.)
     *  for current mouse mode
     *  @param gc GC
     */
    @Override
    protected void drawMouseModeFeedback(final Graphics2D gc)
    {   // Safe copy, then check null (== isPresent())
        final Point2D current = mouse_current.orElse(null);
        final Rectangle plot_bounds = image_area;
        if (current != null)
        {
            final Point2D start = mouse_start.orElse(null);
            if (mouse_mode == MouseMode.ZOOM_IN_X  &&  start != null)
                drawZoomXMouseFeedback(gc, plot_bounds, start, current);
            else if (mouse_mode == MouseMode.ZOOM_IN_Y  &&  start != null)
                drawZoomYMouseFeedback(gc, plot_bounds, start, current);
            else if (mouse_mode == MouseMode.ZOOM_IN_PLOT  &&  start != null)
                drawZoomMouseFeedback(gc, plot_bounds, start, current);
        }
        final Point2D cross = crosshair ? crosshair_position : null;
        if (cross != null)
        {
            final int x = x_axis.getScreenCoord(cross.getX());
            final int y = y_axis.getScreenCoord(cross.getY());
            if (plot_bounds.contains(x, y))
            {
                for (int i=0; i<2; ++i)
                {
                    if (i==0)
                    {
                        gc.setColor(java.awt.Color.BLACK );
                        gc.setStroke(MOUSE_FEEDBACK_BACK);
                    }
                    else
                    {
                        gc.setColor(java.awt.Color.RED);
                        gc.setStroke(MOUSE_FEEDBACK_FRONT);
                    }
                    gc.drawLine(plot_bounds.x, y, x-2, y);
                    gc.drawLine(x+2, y, plot_bounds.x + plot_bounds.width, y);
                    gc.drawLine(x, plot_bounds.y, x, y-2);
                    gc.drawLine(x, y+2, x, plot_bounds.y + plot_bounds.height);
                }
            }
        }

        // Performed either a complete image and cursor redraw,
        // or just a cursor update over existing image.
        // In any case, the cursor listener needs to receive new location and/or image pixel
        notifyCursorListener();
    }

    private void notifyCursorListener()
    {
        // Is there a cursor listener?
        if (plot_listener == null)
            return;

        final Point2D last_pos = crosshair_position;
        if (last_pos == null)
        {
            plot_listener.changedCursorInfo(Double.NaN, Double.NaN, -1, -1, Double.NaN);
            return;
        }

        final double x_val = last_pos.getX(), y_val = last_pos.getY();

        // Location as coordinate in image
        // No "+0.5" rounding! Truncate to get full pixel offsets,
        // don't jump to next pixel when mouse moves beyond 'half' of the current pixel.
        // Use -1 to mark location outside of data width resp. height.
        int image_x = (int) (data_width * (x_val - min_x) / (max_x - min_x));
        if (image_x < 0)
            image_x = -1;
        else if (image_x >= data_width)
            image_x = -1;

        // Mouse and image coords for Y go 'down'
        int image_y = (int) (data_height * (max_y - y_val) / (max_y - min_y));
        if (image_y < 0)
            image_y = -1;
        else if (image_y >= data_height)
            image_y = -1;

        final ListNumber data = image_data;
        double pixel = Double.NaN;
        if (data != null  &&  image_x >= 0  &&  image_y >= 0)
        {
            final int offset = image_x + image_y * data_width;
            try
            {
                if (unsigned_data)
                {
                    if (data instanceof ArrayByte)
                        pixel = Byte.toUnsignedInt(data.getByte(offset));
                    else if (data instanceof ArrayShort)
                        pixel = Short.toUnsignedInt(data.getShort(offset));
                    else if (data instanceof ArrayInteger)
                        pixel = Integer.toUnsignedLong(data.getInt(offset));
                    else
                        pixel = data.getDouble(offset);
                }
                else
                    pixel = data.getDouble(offset);
            }
            catch (Throwable ex)
            {   // Catch ArrayIndexOutOfBoundsException or other internal errors of ListNumber
                logger.log(Level.WARNING, "Error accessing pixel " + image_x + ", " + image_y + " of data with size " + data.size());
                // leave pixel == Double.NaN;
            }
        }
        plot_listener.changedCursorInfo(x_val, y_val, image_x, image_y, pixel);
    }

    /** onMousePressed */
    private void mouseDown(final MouseEvent e)
    {
        // Don't start mouse actions when user invokes context menu
        if (! e.isPrimaryButtonDown()  ||  (PlatformInfo.is_mac_os_x && e.isControlDown()))
            return;

        // Received a click while a tacker is active
        // -> User clicked outside of tracker. Remove it.
        if (roi_tracker != null)
        {
            removeROITracker();
            // Don't cause accidental 'zoom out' etc.
            // User needs to click again for that.
            return;
        }

        // Select any tracker
        final Point2D current = new Point2D(e.getX(), e.getY());
        for (RegionOfInterest roi : rois)
            if (roi.isVisible()  &&  roi.isInteractive())
            {
                final Rectangle rect = roiToScreen(roi);
                if (rect.contains(current.getX(), current.getY()))
                {   // Check if complete ROI is visible,
                    // because otherwise tracker would extend beyond the
                    // current image viewport
                    final Rectangle2D image_rect = GraphicsUtils.convert(image_area);
                    if (image_rect.contains(rect.x, rect.y, rect.width, rect.height))
                    {
                        roi_tracker = new Tracker(image_rect);
                        roi_tracker.setPosition(rect.x, rect.y, rect.width, rect.height);
                        ChildCare.addChild(getParent(), roi_tracker);
                        final int index = rois.indexOf(roi);
                        roi_tracker.setListener((old_pos, new_pos) -> updateRoiFromScreen(index, new_pos));
                        return;
                    }
                }
            }

        mouse_start = mouse_current = Optional.of(current);

        final int clicks = e.getClickCount();

        if (mouse_mode == MouseMode.NONE)
        {
            if (crosshair)
            {
                updateLocationInfo(e.getX(), e.getY());
                requestRedraw();
            }
        }
        else if (mouse_mode == MouseMode.PAN)
        {   // Determine start of 'pan'
            mouse_start_x_range = x_axis.getValueRange();
            mouse_start_y_range = y_axis.getValueRange();
            mouse_mode = MouseMode.PAN_PLOT;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN  &&  clicks == 1)
        {   // Determine start of 'rubberband' zoom.
            // Reset cursor from SIZE* to CROSS.
            if (y_axis.getBounds().contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.ZOOM_IN_Y;
                PlotCursors.setCursor(this, mouse_mode);
            }
            else if (image_area.contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.ZOOM_IN_PLOT;
                PlotCursors.setCursor(this, mouse_mode);
            }
            else if (x_axis.getBounds().contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.ZOOM_IN_X;
                PlotCursors.setCursor(this, mouse_mode);
            }
        }
        else if ((mouse_mode == MouseMode.ZOOM_IN && clicks == 2)  ||  mouse_mode == MouseMode.ZOOM_OUT)
            zoomInOut(current.getX(), current.getY(), ZOOM_FACTOR);
    }

    /** setOnMouseMoved */
    private void mouseMove(final MouseEvent e)
    {
        final Point2D current = new Point2D(e.getX(), e.getY());
        mouse_current = Optional.of(current);

        // While zooming, when mouse is quickly dragged outside the widget
        // and then released, the 'mouseUp' event is sometimes missing.
        // --> When seeing an active mouse move w/o button press,
        //     treat that just like a release.
        if (mouse_mode.ordinal() >= MouseMode.ZOOM_IN_X.ordinal()  &&  !e.isPrimaryButtonDown())
        {
            mouseUp(e);
            return;
        }

        PlotCursors.setCursor(this, mouse_mode);

        final Point2D start = mouse_start.orElse(null);
        switch (mouse_mode)
        {
        case PAN_PLOT:
            if (start != null)
            {
                x_axis.pan(mouse_start_x_range, x_axis.getValue((int)start.getX()), x_axis.getValue((int)current.getX()));
                y_axis.pan(mouse_start_y_range, y_axis.getValue((int)start.getY()), y_axis.getValue((int)current.getY()));
            }
            break;
        case ZOOM_IN_X:
        case ZOOM_IN_Y:
        case ZOOM_IN_PLOT:
            // Show mouse feedback for ongoing zoom
            requestRedraw();
            break;
        default:
        }
        if (! crosshair)
            updateLocationInfo(e.getX(), e.getY());
    }

    /**
     * Check if the mouse double-clicked on the end of an axis, and if mouse_mode is PAN or NONE.
     * If true, return information about the clicked axis; if not, return null.
     * @author Amanda Carpenter
     * @param event MouseEvent to get info for
     * @return An Object [3] containing:
     * <ol>
     * <li>{@link AxisPart}&lt;Double&gt; axis - clicked axis<\li>
     * <li>boolean isHighEnd - true if click was on high-value end of axis; else, false<\li>
     * <li>{@link Rectangle} area - dimensions and location of click region<\li>
     * </ol>
     */
    public Object [] axisClickInfo(MouseEvent event)
    {
    	//For event.getX(), etc. to work as desired, 'this' must be the source of the MouseEvent
    	if (!this.equals(event.getSource()))
    		event = event.copyFor(this, event.getTarget());
        if ((mouse_mode == MouseMode.NONE || mouse_mode == MouseMode.PAN) && event.getClickCount() == 2)
        {
        	double click_x = event.getX();
        	double click_y = event.getY();
        	//Do the upper or lower end regions of y_axis contain the click?
    		int x = (int) y_axis.getBounds().getX();
    		int w = (int) y_axis.getBounds().getWidth();
    		int h = (int) Math.min(y_axis.getBounds().getHeight()/2, w);
    		Rectangle upper = new Rectangle(x, (int) y_axis.getBounds().getY(), w, h);
    		Rectangle lower = new Rectangle(x, (int) y_axis.getBounds().getMaxY()-h, w, h);
    		if (upper.contains(click_x, click_y))
    		{
    			Object [] ret = {y_axis, true, upper};
    			return ret;
    		}
    		else if (lower.contains(click_x, click_y))
    		{
    			Object [] ret = {y_axis, false, lower};
    			return ret;
    		}
        	//Do the left-side (lesser) or right-side (greater) end regions of the x-axis contain it?
        	int y = (int) x_axis.getBounds().getY();
        	h = (int) x_axis.getBounds().getHeight();
        	w = (int) Math.min(x_axis.getBounds().getWidth()/2, h);
        	Rectangle lesser = new Rectangle((int) x_axis.getBounds().getX(), y, w, h);
        	Rectangle greater = new Rectangle((int) x_axis.getBounds().getMaxX()-w, y, w, h);
    		if (lesser.contains(click_x, click_y))
    		{
    			Object [] ret = {x_axis, false, lesser};
    			return ret;
    		}
    		else if (greater.contains(click_x, click_y))
    		{
    			Object [] ret = {x_axis, true, greater};
    			return ret;
        	}
        }
    	return null;
    }

    /** Update information about the image location under the mouse pointer
     *  @param mouse_x
     *  @param mouse_y
     */
    private void updateLocationInfo(final double mouse_x, final double mouse_y)
    {
        if (image_area.contains(mouse_x, mouse_y))
        {
            // Pass 'double' mouse_x/y?
            // In reality, the values seem to be full numbers anyway,
            // so rounding to nearest integer doesn't loose any information.
            final int screen_x = (int) (mouse_x + 0.5);
            final int screen_y = (int) (mouse_y + 0.5);

            // Location on axes, i.e. what user configured as horizontal and vertical values
            final double x_val = x_axis.getValue(screen_x);
            final double y_val = y_axis.getValue(screen_y);
            setCrosshairLocation(x_val, y_val, true);
        }
        else
            setCrosshairLocation(Double.NaN, Double.NaN, true);
    }

    /** Set location of crosshair
     *  @param x_val Mouse coordinate
     *  @param y_val ..
     *  @param notify_listener Notify cursor listener?
     */
    public void setCrosshairLocation(final double x_val, final double y_val, final boolean notify_listener)
    {
        if (Double.isNaN(x_val)  ||  Double.isNaN(y_val))
        {
            if (crosshair_position == null)
                return;
            crosshair_position = null;
            if (notify_listener  &&  plot_listener != null)
                plot_listener.changedCursorInfo(Double.NaN, Double.NaN, -1, -1, Double.NaN);
            requestRedraw();
            return;
        }

        final Point2D pos = new Point2D(x_val, y_val);
        if (pos.equals(crosshair_position))
            return;
        crosshair_position = pos;
        if (notify_listener  &&  plot_listener != null)
            plot_listener.changedCrosshair(x_val, y_val);

        // New cursor location needs redrawn crosshair and update of cursor listener
        // (redraw with current image data, not full update of image itself)
        requestRedraw();
    }

    /** setOnMouseReleased */
    private void mouseUp(final MouseEvent e)
    {
        final Point2D start = mouse_start.orElse(null);
        final Point2D current = mouse_current.orElse(null);
        if (start == null  ||  current == null)
            return;

        if (mouse_mode == MouseMode.PAN_PLOT)
        {
            mouseMove(e);
            undo.add(new ChangeImageZoom(Messages.Pan, x_axis, mouse_start_x_range, x_axis.getValueRange(),
                                         y_axis, mouse_start_y_range, y_axis.getValueRange()));
            mouse_mode = MouseMode.PAN;
            mouse_start_x_range = null;
            mouse_start_y_range = null;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_X)
        {   // X axis increases going _right_ just like mouse 'x' coordinate
            if (Math.abs(start.getX() - current.getX()) > ZOOM_PIXEL_THRESHOLD)
            {
                final int low = (int) Math.min(start.getX(), current.getX());
                final int high = (int) Math.max(start.getX(), current.getX());
                final AxisRange<Double> original_x_range = x_axis.getValueRange();
                final AxisRange<Double> new_x_range = getRestrictedRange(x_axis.getValue(low), x_axis.getValue(high), min_x, max_x);
                undo.execute(new ChangeImageZoom(x_axis, original_x_range, new_x_range, null, null, null));
            }
            mouse_mode = MouseMode.ZOOM_IN;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_Y)
        {   // Mouse 'y' increases going _down_ the screen
            if (Math.abs(start.getY() - current.getY()) > ZOOM_PIXEL_THRESHOLD)
            {
                final int high = (int) Math.min(start.getY(), current.getY());
                final int low = (int) Math.max(start.getY(), current.getY());
                final AxisRange<Double> original_y_range = y_axis.getValueRange();
                final AxisRange<Double> new_y_range = getRestrictedRange(y_axis.getValue(low), y_axis.getValue(high), min_y, max_y);
                undo.execute(new ChangeImageZoom(null, null, null, y_axis, original_y_range, new_y_range));
            }
            mouse_mode = MouseMode.ZOOM_IN;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_PLOT)
        {
            if (Math.abs(start.getX() - current.getX()) > ZOOM_PIXEL_THRESHOLD  ||
                Math.abs(start.getY() - current.getY()) > ZOOM_PIXEL_THRESHOLD)
            {   // X axis increases going _right_ just like mouse 'x' coordinate
                int low = (int) Math.min(start.getX(), current.getX());
                int high = (int) Math.max(start.getX(), current.getX());
                final AxisRange<Double> original_x_range = x_axis.getValueRange();
                final AxisRange<Double> new_x_range = getRestrictedRange(x_axis.getValue(low), x_axis.getValue(high), min_x, max_x);
                // Mouse 'y' increases going _down_ the screen
                high = (int) Math.min(start.getY(), current.getY());
                low = (int) Math.max(start.getY(), current.getY());
                final AxisRange<Double> original_y_range = y_axis.getValueRange();
                final AxisRange<Double> new_y_range = getRestrictedRange(y_axis.getValue(low), y_axis.getValue(high), min_y, max_y);
                undo.execute(new ChangeImageZoom(x_axis, original_x_range, new_x_range,
                                                 y_axis, original_y_range, new_y_range));
            }
            mouse_mode = MouseMode.ZOOM_IN;
        }
    }

    /** setOnMouseExited */
    private void mouseExit(final MouseEvent e)
    {
        // Reset cursor
        // Clear mouse position so drawMouseModeFeedback() won't restore cursor
        mouse_current = Optional.empty();
        PlotCursors.setCursor(this, Cursor.DEFAULT);
        if (! crosshair)
            updateLocationInfo(-1, -1);
    }

    /** Zoom 'in' or 'out' from where the mouse was clicked
     *  @param x Mouse coordinate
     *  @param y Mouse coordinate
     *  @param factor Zoom factor, positive to zoom 'out'
     */
    @Override
    protected void zoomInOut(final double x, final double y, final double factor)
    {
        // In case ROI is visible, hide because zoom invalidates the tracker position on the screen
        removeROITracker();

        final boolean zoom_x = x_axis.getBounds().contains(x, y);
        final boolean zoom_y = y_axis.getBounds().contains(x, y);
        final boolean zoom_both = image_area.getBounds().contains(x, y);
        AxisRange<Double> orig_x = null, orig_y = null;
        if (zoom_x || zoom_both)
            orig_x = zoomAxis(x_axis, (int)x, factor, min_x, max_x);
        if (zoom_y || zoom_both)
            orig_y = zoomAxis(y_axis, (int)y, factor, min_y, max_y);
        if (zoom_both)
            undo.add(new ChangeImageZoom(x_axis, orig_x, x_axis.getValueRange(),
                                         y_axis, orig_y, y_axis.getValueRange()));
        else if (zoom_x)
            undo.add(new ChangeImageZoom(x_axis, orig_x, x_axis.getValueRange(), null, null, null));
        else if (zoom_y)
            undo.add(new ChangeImageZoom(null, null, null, y_axis, orig_y, y_axis.getValueRange()));
    }

    /** Zoom 'in' or 'out' from center
     *  @param zoom_in Zoom 'in' or 'out'?
     */
    public void zoomInOut(final boolean zoom_in)
    {
        final AxisRange<Integer> xrange = x_axis.getScreenRange();
        final AxisRange<Integer> yrange = y_axis.getScreenRange();
        zoomInOut((xrange.getLow() + xrange.getHigh())/2,
                  (yrange.getLow() + yrange.getHigh())/2,
                  zoom_in ? -ZOOM_FACTOR : ZOOM_FACTOR);
    }

    /** Zoom one axis 'in' or 'out' around a position on the axis
     *  @param axis Axis to zoom
     *  @param pos Screen coordinate on the axis
     *  @param factor Zoom factor
     *  @param min Minimum and ..
     *  @param max .. maximum value for axis range
     *  @return
     */
    private AxisRange<Double> zoomAxis(final AxisPart<Double> axis, final int pos, final double factor,
                                       final double min, final double max)
    {
        final AxisRange<Double> orig = axis.getValueRange();
        final double fixed = axis.getValue(pos);
        final double new_low  = fixed - (fixed - orig.getLow()) * factor;
        final double new_high = fixed + (orig.getHigh() - fixed) * factor;
        final AxisRange<Double> new_range = getRestrictedRange(new_low, new_high, min, max);
        axis.setValueRange(new_range.getLow(), new_range.getHigh());
        return orig;
    }

    /** Restrict value range
     *
     *  <p>Do not allow zooming 'out' beyond min..max,
     *  accounting for both normal and inverted axis ranges
     *
     *  @param low, high: Desired range
     *  @param min, max: Limits
     */
    private static AxisRange<Double> getRestrictedRange(final double low, final double high, final double min, final double max)
    {
        if (min <= max)
            return new AxisRange<>(Math.max(min, low), Math.min(max, high));
        else
            return new AxisRange<>(Math.min(min, low), Math.max(max, high));
    }

    void fireChangedAxisRange(final Axis<Double> axis)
    {
        final RTImagePlotListener listener = plot_listener;
        if (listener == null)
            return;
        final AxisRange<Double> range = axis.getValueRange();
        if (axis == x_axis)
            listener.changedXAxis(range.getLow(), range.getHigh());
        else
            listener.changedYAxis(range.getLow(), range.getHigh());
    }

    void fireChangedAutoScale()
    {
        final RTImagePlotListener listener = plot_listener;
        if (listener != null)
            listener.changedAutoScale(autoscale);
    }

    void fireChangedLogarithmic()
    {
        final RTImagePlotListener listener = plot_listener;
        if (listener != null)
            listener.changedLogarithmic(isLogscale());
    }

    void fireChangedValueRange()
    {
        final RTImagePlotListener listener = plot_listener;
        if (listener != null)
            listener.changedValueRange(min, max);
    }

    /** Should be invoked when plot no longer used to release resources */
    @Override
    public void dispose()
    {
        super.dispose();

        // Release memory ASAP
        removeROITracker();
        image_data = null;
        rois.clear();
        plot_listener = null;
    }
}
