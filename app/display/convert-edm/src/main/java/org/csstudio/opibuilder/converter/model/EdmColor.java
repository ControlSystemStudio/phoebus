/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.logging.Logger;
import org.csstudio.opibuilder.converter.StringSplitter;

/**
 * Specific class representing EdmColor property.
 *
 * @author Matevz
 *
 */
public class EdmColor extends EdmAttribute {

    public static final String DYNAMIC = "d";
    public static final String STATIC = "s";


    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmColor");

    private String name = "unknown";

    private int red = 0;
    private int green =0;
    private int blue = 0;

    private boolean blinking;

    private boolean dynamic;

    private int blinkRed;
    private int blinkGreen;
    private int blinkBlue;

    private LinkedHashMap<String, String> ruleMap;

    /**
     * @param name color name
     * @param red red element (0-255)
     * @param green
     * @param blue
     */
    public EdmColor(String name, int red, int green, int blue){
        this.name= name;
        this.red = red<<8;
        this.green = green<<8;
        this.blue =  blue<<8;
    }

    /**
     * Constructor which parses EdmColor from general EdmAttribute value.
     *
     * @param genericEntity EdmAttribute containing general EdmColor data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException if EdmAttribute contains invalid data.
     */
    public EdmColor(EdmAttribute genericEntity, boolean required) throws EdmException {
        super(genericEntity);

        setRequired(required);

        if (genericEntity == null || getValueCount() == 0) {
            if (isRequired()) {
                log.warning("Missing required property.");
            } else {
                log.fine("Missing optional property.");
                return;
            }
        }

        String firstVal = getValue(0);

        if (firstVal.startsWith("rgb ")) {
            parseRGBColor();
        } else if (firstVal.startsWith("index ")) {
            parseStaticColor(Integer.parseInt(firstVal.replace("index ", "")));
        } else if (firstVal.equals(STATIC)){
            parseStaticColorListDefinition();
        } else if (firstVal.equals(DYNAMIC)){
            parseDynamicColorListDefinition();
        }

        setInitialized(true);
    }

    public EdmColor(int i) throws EdmException {
        parseStaticColor(i);
    }

    private void parseStaticColorListDefinition() throws EdmException {
        //input
        name = getValue(1);
        //new EdmString(new EdmAttribute(getValue(0)), true);
        if (name == null || name.length() == 0) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "Color name is empty", null);
        }

        try {
            String[] color = getValue(2).split(" ");

            red = Integer.valueOf(color[0]).intValue();
            green = Integer.valueOf(color[1]).intValue();
            blue = Integer.valueOf(color[2]).intValue();

            if (getValueCount() == 4) {
                blinking = true;
                color = getValue(3).split(" ");
                blinkRed = Integer.valueOf(color[0]).intValue();
                blinkGreen = Integer.valueOf(color[1]).intValue();
                blinkBlue = Integer.valueOf(color[2]).intValue();
            }
            else {
                blinking = false;
            }

        } catch (Exception exception) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "", exception);
        }

        log.config("Parsed colorsList definition.");
    }

    private void parseDynamicColorListDefinition() throws EdmException {
        //input
        name = getValue(1);
        //new EdmString(new EdmAttribute(getValue(0)), true);
        if (name == null || name.length() == 0) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "Color name is empty", null);
        }
        dynamic = true;
        ruleMap = new LinkedHashMap<String, String>();
        Scanner scanner = null;
        try {
            String rule = getValue(2);
            scanner = new Scanner(rule);
            while(scanner.hasNextLine()){
                String[] pieces = StringSplitter.splitIgnoreInQuotes(scanner.nextLine(), ':', true);
                if(pieces.length ==2)
                    ruleMap.put(pieces[0],pieces[1]);
            }

        } catch (Exception exception) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "", exception);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        log.config("Parsed colorsList definition.");
    }

    private void parseRGBColor() throws EdmException {

        Pattern p = Pattern.compile("(\\w*)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
        Matcher m = p.matcher(getValue(0));

        if (!m.find()) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "Invalid RGB color format.", null);
        }

        try {
            name = null;
            red = Integer.valueOf(m.group(2)).intValue();
            green = Integer.valueOf(m.group(3)).intValue();
            blue = Integer.valueOf(m.group(4)).intValue();
            blinking = false;

            log.config("Parsed RGB color.");
        }
        catch (Exception e) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "Invalid RGB color format.",e);
        }
    }

    private void parseStaticColor(int i) throws EdmException {

        EdmColor color = EdmModel.getColorsList().getColor(i);
        if (color == null) {
            log.warning("Color index " + i + " is not in colors list file. Use black color instead.");
            color = new EdmColor(0);
        }

        name = color.getName();
        red = color.getRed();
        green = color.getGreen();
        blue = color.getBlue();
        blinking = color.isBlinking();
        blinkRed = color.getBlinkRed();
        blinkGreen = color.getBlinkGreen();
        blinkBlue = color.getBlinkBlue();
        dynamic = color.isDynamic();
        ruleMap = color.getRuleMap();

        log.config("Parsed static color.");
    }


    public String getName() {
        return name;
    }

    public int getRed() {
        if(isDynamic() && !ruleMap.isEmpty()){
            EdmColor color = EdmModel.getColorsList().getColor(ruleMap.values().toArray()[0].toString());
            if(color!=null)
                return color.getRed();
        }
        return red;
    }

    /**
     * @return the rule map. Key is the rule expression, value is the color name.
     */
    public LinkedHashMap<String, String> getRuleMap() {
        return ruleMap;
    }

    public int getGreen() {
        if(isDynamic() && !ruleMap.isEmpty()){
            EdmColor color = EdmModel.getColorsList().getColor(ruleMap.values().toArray()[0].toString());
            if(color!=null)
                return color.getGreen();
        }
        return green;
    }

    public int getBlue() {
        if(isDynamic() && !ruleMap.isEmpty()){
            EdmColor color = EdmModel.getColorsList().getColor(ruleMap.values().toArray()[0].toString());
            if(color!=null)
                return color.getBlue();
        }
        return blue;
    }

    public int getBlinkRed() {
        return blinkRed;
    }

    public int getBlinkGreen() {
        return blinkGreen;
    }

    public int getBlinkBlue() {
        return blinkBlue;
    }

    public boolean isBlinking() {
        return blinking;
    }

    public boolean isDynamic() {
        return dynamic;
    }
}
