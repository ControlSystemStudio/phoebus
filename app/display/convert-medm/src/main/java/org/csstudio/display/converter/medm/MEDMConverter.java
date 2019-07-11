package org.csstudio.display.converter.medm;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.opibuilder.adl2boy.translator.Display2Model;
import org.csstudio.opibuilder.adl2boy.translator.Text2Model;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.ColorMap;
import org.csstudio.utility.adlparser.fileParser.ParserADL;

@SuppressWarnings("nls")
public class MEDMConverter
{
    public static final Logger logger = Logger.getLogger(MEDMConverter.class.getPackageName());

    private DisplayModel display = new DisplayModel();
    private final WidgetColor[] colorMap;

    public MEDMConverter(final File input, final File output) throws Exception
    {
        logger.log(Level.INFO, "Convert " + input + " -> " + output);

        // Parse ADL
        final ADLWidget root = ParserADL.getNextElement(input);

        // Get color map
        colorMap = getColorMap(root);
        logger.log(Level.INFO, "Color map: " + Arrays.toString(colorMap));

        // Get overall display info
        initializeDisplayModel(input.getName(), root);
        logger.log(Level.FINE, "Display '" + display.getName() + "' size " + display.propWidth().getValue() + " x " + display.propHeight().getValue());

        // Convert all widgets
        convertChildren(root.getObjects(), display, colorMap);

        // Write to output
        final ModelWriter writer = new ModelWriter(new FileOutputStream(output));
        writer.writeModel(display);
        writer.close();
    }

    /** @param root
     *  @return {@link WidgetColor}s
     *  @throws Exception on error
     */
    private WidgetColor[] getColorMap(final ADLWidget root) throws Exception
    {
        WidgetColor[] colorMap = new WidgetColor[0];
        for (ADLWidget adlWidget : root.getObjects())
        {
            String widgetType = adlWidget.getType();
            if (widgetType.equals("color map"))
            {
                ColorMap tempColorMap = new ColorMap(adlWidget);
                colorMap = tempColorMap.getColors();
            }
        }
        return colorMap;
    }

    private void initializeDisplayModel(String filename, final ADLWidget root) throws Exception
    {
        for (ADLWidget adlWidget : root.getObjects())
        {
            final String widgetType = adlWidget.getType();
            if (widgetType.equals("display"))
            {
                display = (new Display2Model(adlWidget, colorMap, null)).getWidgetModel();
                if (filename.endsWith(".adl"))
                    filename = filename.substring(0, filename.length()-4);
                display.propName().setValue(filename);
                return;
            }
        }
    }

    public static void convertChildren(final List<ADLWidget> childWidgets, final Widget parentModel, final WidgetColor[] colorMap)
    {
        for (ADLWidget adlWidget : childWidgets)
        {
            try
            {
                String widgetType = adlWidget.getType();
                logger.log(Level.FINE, "Handling #" + adlWidget.getObjectNr() + " " + adlWidget.getType());

                if (widgetType.equals("text"))
                    new Text2Model(adlWidget, colorMap, parentModel);
                // TODO Add all the widgets
                else
                    logger.log(Level.FINE, "Ignoring convert #" + adlWidget.getObjectNr() + " " + adlWidget.getType());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot convert #" + adlWidget.getObjectNr() + " " + adlWidget.getType(), ex);
            }
        }

    }
}
