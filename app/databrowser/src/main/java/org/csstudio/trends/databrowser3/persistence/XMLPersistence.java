/*******************************************************************************
 * Copyright (c) 2014-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.persistence;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.trends.databrowser3.model.AnnotationInfo;
import org.csstudio.trends.databrowser3.model.ArchiveRescale;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.framework.persistence.IndentingXMLStreamWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/** Load and save {@link Model} as XML file
 *
 *  <p>Attempts to load files going back to very early versions
 *  of the Data Browser
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLPersistence
{
    public static final String DEFAULT_FONT_FAMILY = "Liberation Sans";
    public static final double DEFAULT_FONT_SIZE = 10;

    // XML file tags
    final public static String TAG_DATABROWSER = "databrowser";

    final public static String TAG_TITLE = "title";
    final public static String TAG_SAVE_CHANGES = "save_changes";
    final public static String TAG_GRID = "grid";
    final public static String TAG_SCROLL = "scroll";
    final public static String TAG_UPDATE_PERIOD = "update_period";
    final public static String TAG_SCROLL_STEP = "scroll_step";
    final public static String TAG_START = "start";
    final public static String TAG_END = "end";
    final public static String TAG_ARCHIVE_RESCALE = "archive_rescale";
    final public static String TAG_FOREGROUND = "foreground";
    final public static String TAG_BACKGROUND = "background";
    final public static String TAG_TITLE_FONT = "title_font";
    final public static String TAG_LABEL_FONT = "label_font";
    final public static String TAG_SCALE_FONT = "scale_font";
    final public static String TAG_LEGEND_FONT = "legend_font";
    final public static String TAG_AXES = "axes";
    final public static String TAG_ANNOTATIONS = "annotations";
    final public static String TAG_PVLIST = "pvlist";

    final public static String TAG_SHOW_TOOLBAR = "show_toolbar";
    final public static String TAG_SHOW_LEGEND = "show_legend";

    final public static String TAG_COLOR = "color";
    final public static String TAG_RED = "red";
    final public static String TAG_GREEN = "green";
    final public static String TAG_BLUE = "blue";

    final public static String TAG_AXIS = "axis";
    final public static String TAG_VISIBLE = "visible";
    final public static String TAG_NAME = "name";
    final public static String TAG_USE_AXIS_NAME = "use_axis_name";
    final public static String TAG_USE_TRACE_NAMES = "use_trace_names";
    final public static String TAG_RIGHT = "right";
    final public static String TAG_MAX = "max";
    final public static String TAG_MIN = "min";
    final public static String TAG_AUTO_SCALE = "autoscale";
    final public static String TAG_LOG_SCALE = "log_scale";

    final public static String TAG_ANNOTATION = "annotation";
    final public static String TAG_PV = "pv";
    final public static String TAG_TIME = "time";
    final public static String TAG_VALUE = "value";
    final public static String TAG_OFFSET = "offset";
    final public static String TAG_TEXT = "text";

    final public static String TAG_X = "x";
    final public static String TAG_Y = "y";

    final public static String TAG_DISPLAYNAME = "display_name";
    final public static String TAG_TRACE_TYPE = "trace_type";
    final public static String TAG_LINE_STYLE = "line_style";
    final public static String TAG_LINEWIDTH = "linewidth";
    final public static String TAG_POINT_TYPE = "point_type";
    final public static String TAG_POINT_SIZE = "point_size";
    final public static String TAG_WAVEFORM_INDEX = "waveform_index";
    final public static String TAG_SCAN_PERIOD = "period";
    final public static String TAG_LIVE_SAMPLE_BUFFER_SIZE = "ring_size";
    final public static String TAG_REQUEST = "request";
    final public static String TAG_ARCHIVE = "archive";

    final public static String TAG_URL = "url";

    final public static String TAG_FORMULA = "formula";
    final public static String TAG_INPUT = "input";

    final private static String TAG_OLD_XYGRAPH_SETTINGS = "xyGraphSettings";

    final public static String TAG_KEY = "key";

    /** @param model Model to load
     *  @param stream XML stream
     *  @throws Exception on error
     */
    public static void load(final Model model, final InputStream stream) throws Exception
    {
        final DocumentBuilder docBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(stream);
        load(model, doc);
    }

    private static void load(final Model model, final Document doc) throws Exception
    {
        if (model.getItems().size() > 0)
            throw new RuntimeException("Model was already in use");

        // Check if it's a <databrowser/>.
        doc.getDocumentElement().normalize();
        final Element root_node = doc.getDocumentElement();
        if (!root_node.getNodeName().equals(TAG_DATABROWSER))
            throw new Exception("Expected " + TAG_DATABROWSER + " but got " + root_node.getNodeName());

        // Global settings
        XMLUtil.getChildString(root_node, TAG_TITLE).ifPresent(model::setTitle);
        XMLUtil.getChildBoolean(root_node, TAG_SAVE_CHANGES).ifPresent(model::setSaveChanges);
        XMLUtil.getChildBoolean(root_node, TAG_GRID).ifPresent(model::setGridVisible);

        XMLUtil.getChildDouble(root_node, TAG_UPDATE_PERIOD).ifPresent(model::setUpdatePeriod);

        try
        {
            model.setScrollStep( Duration.ofSeconds(
                    XMLUtil.getChildInteger(root_node, TAG_SCROLL_STEP).orElse((int) Preferences.scroll_step.getSeconds())));
        }
        catch (Throwable ex)
        {
            // Ignore
        }

        final String start = model.resolveMacros(XMLUtil.getChildString(root_node, TAG_START).orElse(""));
        final String end = model.resolveMacros(XMLUtil.getChildString(root_node, TAG_END).orElse(""));
        if (start.length() > 0  &&  end.length() > 0)
        {
            final boolean scroll = XMLUtil.getChildBoolean(root_node, TAG_SCROLL).orElse(true);
            final TimeRelativeInterval interval;
            if (scroll)
            {   // Relative start time .. now
                final TemporalAmount span = TimeWarp.parseLegacy(start);
                if (Duration.ZERO.equals(span))
                    interval = TimeRelativeInterval.of(Preferences.time_span, Duration.ZERO);
                else
                    interval = TimeRelativeInterval.startsAt(span);
            }
            else
            {   // Absolute start ... end
                interval = TimeRelativeInterval.of(TimestampFormats.parse(patchLegacyAbsTime(start)), TimestampFormats.parse(patchLegacyAbsTime(end)));
            }
            model.setTimerange(interval);
        }

        final String rescale = XMLUtil.getChildString(root_node, TAG_ARCHIVE_RESCALE).orElse(ArchiveRescale.STAGGER.name());
        try
        {
            model.setArchiveRescale(ArchiveRescale.valueOf(rescale));
        }
        catch (Throwable ex)
        {
            // Ignore
        }

        XMLUtil.getChildBoolean(root_node, TAG_SHOW_TOOLBAR).ifPresent(model::setToolbarVisible);
        XMLUtil.getChildBoolean(root_node, TAG_SHOW_LEGEND).ifPresent(model::setLegendVisible);

        // Value Axes
        final Element axes = XMLUtil.getChildElement(root_node, TAG_AXES);
        if (axes != null)
        {
            for (Element item : XMLUtil.getChildElements(axes, TAG_AXIS))
                model.addAxis(AxisConfig.fromDocument(item));
        }
        else
        {   // Check for legacy <xyGraphSettings> <axisSettingsList>
            final Element list = XMLUtil.getChildElement(root_node, TAG_OLD_XYGRAPH_SETTINGS);
            if (list != null)
            {
                loadColorFromDocument(list, "plotAreaBackColor").ifPresent(model::setPlotBackground);

                boolean first_axis = true;
                for (Element item : XMLUtil.getChildElements(list, "axisSettingsList"))
                {
                    if (first_axis)
                    {   // First axis is 'X'
                        XMLUtil.getChildBoolean(item, "showMajorGrid").ifPresent(model::setGridVisible);
                        first_axis = false;
                    }
                    else
                    {   // Read 'Y' axes
                        final String name = XMLUtil.getChildString(item, "title").orElse(null);
                        final AxisConfig axis = new AxisConfig(name);
                        loadColorFromDocument(item, "foregroundColor").ifPresent(axis::setColor);

                        XMLUtil.getChildBoolean(item, "showMajorGrid").ifPresent(axis::setGridVisible);
                        XMLUtil.getChildBoolean(item, "logScale").ifPresent(axis::setLogScale);
                        XMLUtil.getChildBoolean(item, "autoScale").ifPresent(axis::setAutoScale);

                        final Element range = XMLUtil.getChildElement(item, "range");
                        if (range != null)
                        {
                            final double min = XMLUtil.getChildDouble(range, "lower").orElse(axis.getMin());
                            final double max = XMLUtil.getChildDouble(range, "upper").orElse(axis.getMax());
                            axis.setRange(min, max);
                        }
                        model.addAxis(axis);

                        // Using legacy settings from _last_ axis for fonts
                        loadFontFromDocument(item, "scaleFont").ifPresent(model::setScaleFont);
                        loadFontFromDocument(item, "titleFont").ifPresent(model::setLabelFont);
                    }
                }
            }
        }

        // New settings, possibly replacing settings from legacy <xyGraphSettings> <axisSettingsList>
        loadColorFromDocument(root_node, TAG_FOREGROUND).ifPresent(model::setPlotForeground);
        loadColorFromDocument(root_node, TAG_BACKGROUND).ifPresent(model::setPlotBackground);
        loadFontFromDocument(root_node, TAG_TITLE_FONT).ifPresent(model::setTitleFont);
        loadFontFromDocument(root_node, TAG_LABEL_FONT).ifPresent(model::setLabelFont);
        loadFontFromDocument(root_node, TAG_SCALE_FONT).ifPresent(model::setScaleFont);
        loadFontFromDocument(root_node, TAG_LEGEND_FONT).ifPresent(model::setLegendFont);

        // Load Annotations
        Element list = XMLUtil.getChildElement(root_node, TAG_ANNOTATIONS);
        if (list != null)
        {
            final List<AnnotationInfo> annotations = new ArrayList<>();
            for (Element item : XMLUtil.getChildElements(list, TAG_ANNOTATION))
            {
                try
                {
                    annotations.add(AnnotationInfo.fromDocument(item));
                }
                catch (Throwable ex)
                {
                    logger.log(Level.INFO, "XML error in Annotation", ex);
                }
            }
            model.setAnnotations(annotations);
        }

        // Load PVs/Formulas
        list = XMLUtil.getChildElement(root_node, TAG_PVLIST);
        if (list != null)
        {
            // Iterate over all elements, then check for PV or FORMULA to preserve order.
            // Iterating over all PVs first, then FORMULAs would change their order.
            for (Element item : XMLUtil.getChildElements(list))
            {
                if (item.getNodeName().equals(TAG_PV))
                {
                    // Load PV item
                    final PVItem model_item = PVItem.fromDocument(model, item);

                    if (model_item.getName().isBlank())
                    {
                        // Items need a PV name.
                        // Patch missing name, don't remove item in case following formulas
                        // use "x5" with this PV's index
                        model_item.setName("loc://empty(0)");
                        model_item.setDisplayName(model_item.getName());
                        logger.log(Level.WARNING, "Patching <pv> entry without <name> into " + model_item.getName());
                    }

                    // Adding item creates the axis for it if not already there
                    model.addItem(model_item);
                    // Ancient data browser stored axis configuration with each item: Update axis from that.
                    final AxisConfig axis = model_item.getAxis();

                    XMLUtil.getChildBoolean(item, TAG_AUTO_SCALE).ifPresent(
                        auto ->
                        {
                            if (auto)
                                axis.setAutoScale(true);
                        });

                    XMLUtil.getChildBoolean(item, TAG_LOG_SCALE).ifPresent(
                        log ->
                        {
                            if (log)
                                axis.setLogScale(true);
                        });

                    final Optional<Double> min = XMLUtil.getChildDouble(item, TAG_MIN);
                    final Optional<Double> max = XMLUtil.getChildDouble(item, TAG_MAX);
                    if (min.isPresent()  &&  max.isPresent())
                        axis.setRange(min.get(), max.get());
                }
                else if (item.getNodeName().equals(TAG_FORMULA))
                {
                    // Load Formulas
                    model.addItem(FormulaItem.fromDocument(model, item));
                }
            }
        }

        // Update items from legacy <xyGraphSettings>
        list = XMLUtil.getChildElement(root_node, TAG_OLD_XYGRAPH_SETTINGS);
        if (list != null)
        {
            XMLUtil.getChildString(list, TAG_TITLE).ifPresent(model::setTitle);
            final Iterator<ModelItem> model_items = model.getItems().iterator();
            for (Element item : XMLUtil.getChildElements(list, "traceSettingsList"))
            {
                if (! model_items.hasNext())
                    break;
                final ModelItem pv = model_items.next();
                loadColorFromDocument(item, "traceColor").ifPresent(value -> pv.setColor(value));
                XMLUtil.getChildInteger(item, "lineWidth").ifPresent(value -> pv.setLineWidth(value));
                XMLUtil.getChildString(item, "name").ifPresent(value -> pv.setDisplayName(value));
            }
        }
    }

    private static String patchLegacyAbsTime(final String spec)
    {
        // Older absolute time spec used "yyyy/mm/dd ...",
        // which now must be "yyyy-mm-dd ...",
        if (spec.length() > 10  && spec.charAt(4)=='/'  &&  spec.charAt(7) =='/')
            return spec.replace('/', '-');
        return spec;
    }

    /** Load RGB color from XML document
     *  @param node Parent node of the color
     *  @return {@link Color}
     *  @throws Exception on error
     */
    public static Optional<Color> loadColorFromDocument(final Element node) throws Exception
    {
        return loadColorFromDocument(node, TAG_COLOR);
    }

    /** Load RGB color from XML document
     *  @param node Parent node of the color
     *  @param color_tag Name of tag that contains the color
     *  @return {@link Color}
     *  @throws Exception on error
     */
    public static Optional<Color> loadColorFromDocument(final Element node, final String color_tag) throws Exception
    {
        if (node == null)
            return Optional.of(Color.BLACK);
        final Element color = XMLUtil.getChildElement(node, color_tag);
        if (color == null)
            return Optional.empty();
        final int red = XMLUtil.getChildInteger(color, TAG_RED).orElse(0);
        final int green = XMLUtil.getChildInteger(color, TAG_GREEN).orElse(0);
        final int blue = XMLUtil.getChildInteger(color, TAG_BLUE).orElse(0);
        return Optional.of(Color.rgb(red, green, blue));
    }

    /** Load font from XML document
     *  @param node Parent node of the color
     *  @param font_tag Name of tag that contains the font
     *  @return {@link Font}
     */
    public static Optional<Font> loadFontFromDocument(final Element node, final String font_tag)
    {
        final String desc = XMLUtil.getChildString(node, font_tag).orElse("");
        if (desc.isEmpty())
            return Optional.empty();

        String family = DEFAULT_FONT_FAMILY;
        FontPosture posture = FontPosture.REGULAR;
        FontWeight weight = FontWeight.NORMAL;
        double size = DEFAULT_FONT_SIZE;

        // Legacy format was "Liberation Sans|20|1"
        final String[] items = desc.split("\\|");
        if (items.length == 3)
        {
            family = items[0];
            size = Double.parseDouble(items[1]);
            switch (items[2])
            {
            case "1": // SWT.BOLD
                weight = FontWeight.BOLD;
                break;
            case "2": // SWT.ITALIC
                posture = FontPosture.ITALIC;
                break;
            case "3": // SWT.BOLD | SWT.ITALIC
                weight = FontWeight.BOLD;
                posture = FontPosture.ITALIC;
                break;
            }
        }
        return Optional.of(Font.font(family, weight, posture, size ));
    }

    private static void writeFont(XMLStreamWriter writer, final String tag_name, final Font font) throws Exception
    {
        writer.writeStartElement(tag_name);
        final StringBuilder buf = new StringBuilder();
        buf.append(font.getFamily())
           .append('|')
           .append((int)font.getSize())
           .append('|');
        // Cannot get the style out of the font as FontWeight, FontPosture??
        final String style = font.getStyle().toLowerCase();
        int code = 0;
        if (style.contains("bold"))
            code |= 1;
        if (style.contains("italic"))
            code |= 2;
        buf.append(code);
        writer.writeCharacters(buf.toString());
        writer.writeEndElement();
    }

    /** Write XML formatted Model content.
     *  @param model Model to write
     *  @param out {@link OutputStream}
     *  @throws Exception on error
     */
    public static void write(final Model model, final OutputStream out) throws Exception
    {
        final XMLStreamWriter base =
            XMLOutputFactory.newInstance().createXMLStreamWriter(out, XMLUtil.ENCODING);
        final XMLStreamWriter writer = new IndentingXMLStreamWriter(base);
        writer.writeStartDocument(XMLUtil.ENCODING, "1.0");
        writer.writeStartElement(TAG_DATABROWSER);
        {
            writer.writeStartElement(TAG_TITLE);
            writer.writeCharacters(model.getTitle().orElse(""));
            writer.writeEndElement();

            if (!model.shouldSaveChanges())
            {
                writer.writeStartElement(TAG_SAVE_CHANGES);
                writer.writeCharacters(Boolean.FALSE.toString());
                writer.writeEndElement();
            }

            // Visibility of toolbar and legend
            if (model.isLegendVisible())
            {
                writer.writeStartElement(TAG_SHOW_LEGEND);
                writer.writeCharacters(Boolean.TRUE.toString());
                writer.writeEndElement();
            }

            if (model.isToolbarVisible())
            {
                writer.writeStartElement(TAG_SHOW_TOOLBAR);
                writer.writeCharacters(Boolean.TRUE.toString());
                writer.writeEndElement();
            }

            // Time axis
            if (model.isGridVisible())
            {
                writer.writeStartElement(TAG_GRID);
                writer.writeCharacters(Boolean.TRUE.toString());
                writer.writeEndElement();
            }

            writer.writeStartElement(TAG_UPDATE_PERIOD);
            writer.writeCharacters(Double.toString(model.getUpdatePeriod()));
            writer.writeEndElement();
            writer.writeStartElement(TAG_SCROLL_STEP);
            writer.writeCharacters(Long.toString(model.getScrollStep().getSeconds()));
            writer.writeEndElement();


            final TimeRelativeInterval span = model.getTimerange();
            writer.writeStartElement(TAG_SCROLL);
            writer.writeCharacters(Boolean.toString(! span.isEndAbsolute()));
            writer.writeEndElement();

            final TimeInterval interval = span.toAbsoluteInterval();
            if (span.isEndAbsolute())
            {
                writer.writeStartElement(TAG_START);
                writer.writeCharacters(TimestampFormats.MILLI_FORMAT.format(interval.getStart()));
                writer.writeEndElement();
                writer.writeStartElement(TAG_END);
                writer.writeCharacters(TimestampFormats.MILLI_FORMAT.format(interval.getEnd()));
                writer.writeEndElement();
            }
            else
            {
                writer.writeStartElement(TAG_START);
                writer.writeCharacters(TimeWarp.formatAsLegacy(span.getRelativeStart().get()));
                writer.writeEndElement();
                writer.writeStartElement(TAG_END);
                writer.writeCharacters(TimeParser.NOW);
                writer.writeEndElement();
            }

            writer.writeStartElement(TAG_ARCHIVE_RESCALE);
            writer.writeCharacters(model.getArchiveRescale().name());
            writer.writeEndElement();

            writeColor(writer, TAG_FOREGROUND, model.getPlotForeground());
            writeColor(writer, TAG_BACKGROUND, model.getPlotBackground());
            writeFont(writer, TAG_TITLE_FONT, model.getTitleFont());
            writeFont(writer, TAG_LABEL_FONT, model.getLabelFont());
            writeFont(writer, TAG_SCALE_FONT, model.getScaleFont());
            writeFont(writer, TAG_LEGEND_FONT, model.getLegendFont());

            // Value axes
            writer.writeStartElement(TAG_AXES);
            for (AxisConfig axis : model.getAxes())
                axis.write(writer);
            writer.writeEndElement();

            // Annotations
            writer.writeStartElement(TAG_ANNOTATIONS);
            for (AnnotationInfo annotation : model.getAnnotations())
                annotation.write(writer);
            writer.writeEndElement();

            // PVs (Formulas)
            writer.writeStartElement(TAG_PVLIST);
            for (ModelItem item : model.getItems())
                item.write(writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    /** Write RGB color to XML document
     *  @param writer
     *  @param tag_name
     *  @param color
     *  @throws Exception
     */
    public static void writeColor(final XMLStreamWriter writer,
                                  final String tag_name, final Color color) throws Exception
    {
        writer.writeStartElement(tag_name);
        writer.writeStartElement(TAG_RED);
        writer.writeCharacters(Integer.toString((int) (color.getRed()*255)));
        writer.writeEndElement();
        writer.writeStartElement(TAG_GREEN);
        writer.writeCharacters(Integer.toString((int) (color.getGreen()*255)));
        writer.writeEndElement();
        writer.writeStartElement(TAG_BLUE);
        writer.writeCharacters(Integer.toString((int) (color.getBlue()*255)));
        writer.writeEndElement();
        writer.writeEndElement();
    }
}
