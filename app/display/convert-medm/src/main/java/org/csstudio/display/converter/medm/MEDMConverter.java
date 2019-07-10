package org.csstudio.display.converter.medm;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.ColorMap;
import org.csstudio.utility.adlparser.fileParser.ParserADL;

@SuppressWarnings("nls")
public class MEDMConverter
{
    public static final Logger logger = Logger.getLogger(MEDMConverter.class.getPackageName());

    private final DisplayModel display = new DisplayModel();
    private final WidgetColor[] colorMap;

    public MEDMConverter(final File input, final File output) throws Exception
    {
        logger.log(Level.INFO, "Convert " + input + " -> " + output);

        final ADLWidget root = ParserADL.getNextElement(input);

        colorMap = getColorMap(root);
        logger.log(Level.INFO, "Color map: " + Arrays.toString(colorMap));

        initializeDisplayModel(root);

        // TODO TranslatorUtils.convertAdlToModel
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

    private void initializeDisplayModel(final ADLWidget root)
    {
        // TODO Auto-generated method stub
        for (ADLWidget adlWidget : root.getObjects()){
            String widgetType = adlWidget.getType();
            if (widgetType.equals("display")){
                // displayModel = (DisplayModel)(new Display2Model(adlWidget, colorMap, null)).getWidgetModel();
            }
        }


    }
}
