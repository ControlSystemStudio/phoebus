package org.csstudio.utility.adlparser.fileParser;

import java.util.List;

import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class ColorMap {
    private WidgetColor[] colors = new WidgetColor[0];
    private int numColors;

    public ColorMap(ADLWidget colorMap) throws WrongADLFormatException {
        // find out the number of colors

        for (FileLine fileLine : colorMap.getBody()){
            String bodyPart = fileLine.getLine();
            String[] row = bodyPart.trim().split("=");
            if (row.length < 2){
                throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
            }
            if (row[0].trim().equals("ncolors")){
                numColors = Integer.parseInt(row[1].replaceAll("\"", "").trim());
            }
            colors = new WidgetColor[numColors];
        }


        ADLWidget colorsWidget = colorMap.getObjects().get(0);
        if (colorsWidget.getType().equals("colors")){
            int colorCounter = 0;
            for (FileLine fileLine  : colorsWidget.getBody()){
                String bodyPart = fileLine.getLine();
                String[] row = bodyPart.split(",");
                if (row.length == 1){
                    String color = row[0].trim();
                    try {
                    colors[colorCounter] = ColorMap.getRGBColor(color);
                    }
                    catch (NumberFormatException ex){
                        ex.printStackTrace();
                        throw new WrongADLFormatException("One of the colors in the color map cannot be decoded "
                                + color);
                    }
                    colorCounter++;
                }
            }

        }
        else if ( colorsWidget.getType().equals("dl_color")){
            List<ADLWidget> colorList = colorMap.getObjects();
            if (colorList.size() == numColors) {
                for (int ii=0; ii<numColors; ii++){
                    int red=0, green=0, blue =0;
                    ADLWidget dl_color = colorList.get(ii);
                    for ( FileLine fileLine : dl_color.getBody()){
                        String bodyPart = fileLine.getLine();
                        String[] row = bodyPart.trim().split("=");
                        if (row.length < 2){
                            throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
                        }
                        try {
                            if (row[0].trim().equals("r")) {
                                red = Integer.parseInt(row[1].replaceAll("\"",
                                        "").trim());
                            }
                            if (row[0].trim().equals("g")) {
                                green = Integer.parseInt(row[1].replaceAll(
                                        "\"", "").trim());
                            }
                            if (row[0].trim().equals("b")) {
                                blue = Integer.parseInt(row[1].replaceAll("\"",
                                        "").trim());
                            }
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                            throw new WrongADLFormatException("One of the RGB values contains a" +
                                    "value that cannot be transformed into an integer: " + bodyPart);
                        }
                        try {
                            colors[ii] = new WidgetColor(red,green,blue);
                        }
                        catch (IllegalArgumentException ex){
                            throw new WrongADLFormatException("Bad Arguments for creating RGB " +
                                    red + ", " + green + ", " + blue);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return Object[] colors
     */
    public WidgetColor[] getColors() {
        return colors;
    }

    public int getNumColors() {
        return numColors;
    }

    /**
     *
     * throws IlleagalArgumentException if the string is not 6 characters long
     * throws NumberFormatException if the each pair cannot be transformed as a
     *        hexdecimal to integer
     * @param inColor
     * @return
     */
    public static WidgetColor getRGBColor(String inColor) throws NumberFormatException, IllegalArgumentException {
        if ( inColor.length() < 6 || inColor.length() >6 ) {
            throw new IllegalArgumentException("RGB string is Hex representation of the color triad.  It should "
                    + "have 6 characters");
        }
        String redStr = "#".concat(inColor.substring(0, 2));
        String greenStr = "#".concat(inColor.substring(2, 4));
        String blueStr = "#".concat(inColor.substring(4, 6));
        int red = Integer.decode(redStr);
        int green = Integer.decode(greenStr);
        int blue = Integer.decode(blueStr);
        return new WidgetColor(red, green, blue);
    }
}
