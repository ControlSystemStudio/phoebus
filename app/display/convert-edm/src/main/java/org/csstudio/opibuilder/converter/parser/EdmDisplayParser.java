/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.parser;

import java.io.InputStream;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.opibuilder.converter.model.EdmAttribute;
import org.csstudio.opibuilder.converter.model.EdmEntity;
import org.csstudio.opibuilder.converter.model.EdmException;


/**
 * Parser class which parses EdmDisplay data.
 *
 * @author Matevz
 *
 */
public class EdmDisplayParser extends EdmParser{

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmDisplayParser");

    private static final String[] reservedWords = {"beginObjectProperties", "beginGroup",
        "beginScreenProperties", "endScreenProperties", "endObjectProperties", "endGroup"};

    /**
     * Parses
     * @param m Matcher, which iterates through each line of data.
     * @param entity Entity which will contain parsed attributes.
     * @throws EdmException if there is nesting error, or if attribute already exists.
     */
    private void parseProperty(Matcher m, EdmEntity entity) throws EdmException {
        EdmAttribute a = new EdmAttribute();
        String line = m.group();

        if (line.length() > 0)
            log.config("Parse attribute from string: \"" + line + "\"");
        else
            return;

        boolean isCompound = false;
        int bracePos = line.indexOf("{");     // position of "{"
        if (bracePos > -1) {
            // checks whether "{" is a part of a string attribute
            if ( ! line.substring(0, bracePos).contains("\""))
                isCompound = true;
        }

        if (isCompound) {
            String attName = line.substring(0, line.indexOf("{") - 1);
            entity.addAttribute(attName, a);
            log.config("Added attribute: " + attName);

            parseCompoundProperty(m, a);
        }
        else {
            Pattern p1 = Pattern.compile("\\S+"); // each word
            Matcher m1 = p1.matcher(line);

            int i = 0;
            StringBuilder value = new StringBuilder();

            // first word = attribute name. Other words = attribute value
            while (m1.find()) {
                if (i == 0) {
                    String attName = m1.group();
                    entity.addAttribute(attName, a);
                    log.config("Added attribute: " + attName);
                }
                else
                    value.append(m1.group() + " ");
                i = i + 1;
            }

            String finalValue;
            if (i > 1)
                // remove last space if there were more words
                finalValue = value.substring(0, value.length() - 1);
            else //if (i > 1)
                finalValue = value.toString();

            log.config("Added value: \"" + a.appendValue(finalValue) + "\"");
        }
    }

    /**
     * Parses values of a nested attribute. Attribute must already be instantiated.
     *
     * @param m    Matcher that iterates through each line of data.
     * @param a    Already instantiated attribute.
     * @throws EdmException    if attribute's values are not properly nested.
     */
    private void parseCompoundProperty(Matcher m, EdmAttribute a) throws EdmException {
        /* PARSES LINE - whole line except for "{" is attr name */

        String line = "";

        // parses another lines - until line contains "}" char
        while (!line.contains("}")) {
            m.find();
            line = m.group();
            if (!line.contains("}") & line.length() > 0) {
                String val = line.trim();
                log.config("Added value: \"" + a.appendValue(val) + "\"");
            }

            boolean nestError = false;
            for (int c = 0; c < reservedWords.length; c ++) {
                if (line.contains(reservedWords[c])) {
                    nestError = true;
                    break;
                }
            }
            if (line.contains("{"))
                nestError = true;
            if (nestError)
                throw new EdmException(EdmException.NESTING_ERROR,
                        "Nesting error at attribute: " + line, null);
        }
    }

    /**
     * Parses the beginScreenProperties tag and returns data without this tag.
     *
     * @param data Data with screen properties to parse.
     * @return Data without screen properties.
     * @throws EdmException if parsing error occurs.
     */
    private StringBuilder parseDisplayProperties(StringBuilder data) throws EdmException {

        Pattern pattern = Pattern.compile("(beginScreenProperties(.*?)endScreenProperties)",
                Pattern.DOTALL);    // everything between beginScr... and endScr...
        Matcher matcher = pattern.matcher(data.toString());

        int i = 0;
        while (matcher.find()) {

            String matchData = matcher.group(2);
            int start = matcher.start();
            int end = matcher.end();

            Pattern p = Pattern.compile(".*");    // whole line
            Matcher m = p.matcher(matchData);

            while (m.find()) {
                parseProperty(m, getRoot());
            }
            if (i > 0)
                log.warning("More than one display file headers found.");
            i++;

            data.delete(start, end);
        }

        return data;
    }

    /**
     * Iterates through data and determines which data is relevant
     * to current group.
     *
     * @param data    Data to parse.
     * @return        Returns the array of integers that describe three positions:
     *                     [0]    Data start position.
     *                     [1]    Data end position.
     *                     [2]    Position of data closing (end) expression.
     * @throws EdmException    Throws exception where there is nesting problem.
     */
    private int[] getGroupData(StringBuilder data) throws EdmException {

        /*example:
            [0]object activeGroupClass
            beginObjectProperties
            :::::data:::::
            beginGroup
            :::::objects:::::
            [1]endGroup
            :::::data:::::
            endObjectProperties[2]
        */

        Vector<Integer> begins = new Vector<>();
        Vector<Integer> ends = new Vector<>();
        Vector<Integer> finalEnds = new Vector<>();

        // searching for positions of group nesting start expressions
        Pattern pattern = Pattern.compile("object\\s+?activeGroupClass",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(data.toString());
        while (matcher.find())
            begins.add(matcher.start());
        // searching for positions of group nesting end expressions
        pattern = Pattern.compile(
                "endGroup.+?endObjectProperties",
                Pattern.DOTALL);
        matcher = pattern.matcher(data.toString());
        while (matcher.find()) {
            ends.add(matcher.start());
            finalEnds.add(matcher.end());
        }
        // if opening and closing expressions count does not match, something is wrong
        if (begins.size() != ends.size())
            throw new EdmException(EdmException.NESTING_ERROR,
                    "Open and close expressions count do not match: open = " + begins.size() +
                    " close = " + ends.size() + ".", null);

        // returns the position of closing expression that is relevant to current group
        int[] endPos = getClosingPosition(begins, ends);
        int end = endPos[0];        // end --> the position where data in interest finishes
        int finalEnd = endPos[1];    // finalEnd --> index of closing expression in array

        if (begins.size() == 0)
            return null;

        int[] result = { begins.get(0), end, finalEnds.get(finalEnd-1) };

        return result;
    }

    /**
     * Returns the closing position relevant to the first data start expression.
     *
     * @param begins    Array of positions of begin expressions.
     * @param ends        Array of positions of closing (end) expressions.
     * @return            Array of two integers:
     *                         [0]    Last (closing) position of data of interest.
     *                         [1]    Index of closing expression in array that closed current data nest.
     * @throws EdmException    Throws exception if data is not properly nested.
     */
    private int[] getClosingPosition(Vector<Integer> begins,
            Vector<Integer> ends) throws EdmException {

        Vector<Integer> all = new Vector<>();
        all.addAll(begins);
        all.addAll(ends);
        Collections.sort(all);

        int level = 0;
        int end = 0;
        int endIndex = 0;
        for (int x = 0; x < all.size(); x++) {
            if (begins.contains(all.get(x)))
                level = level + 1;
            if (ends.contains(all.get(x))) {
                level = level - 1;
                endIndex = endIndex + 1;
            }

            if (level == 0) {
                end = all.get(x);
                break;
            }
        }

        if (level < 0)
            throw new EdmException(EdmException.NESTING_ERROR, "Tree depth has fallen below zero.", null);

        int[] result = { end, endIndex};
        return result;
    }

    /**
     * Parses group header data.
     *
     * @param group EdmEntity representing group.
     * @param groupData Group data to parse.
     * @param endGroupPosition Position where "endGroup" statement is. Used for parsing data between "endGroup" and "endObjectProperties"
     * @return Data without parsed section.
     * @throws EdmException if an error occurs.
     */
    private StringBuilder parseGroupHeader(EdmEntity group, StringBuilder groupData, int endGroupPosition) throws EdmException {

        // group data between "endGroup" and "endObjectProperties" declaration
        int afterDataStart = endGroupPosition + 8; // 8 = "endGroup".length
        int afterDataEnd = groupData.length() - 19; // 19 = "endObjectProperties".length
        String afterData = groupData.substring(afterDataStart, afterDataEnd);
        // removes afterData from groupData
        groupData.delete(endGroupPosition, groupData.length());


        // group data between "beginObjectProperties" and "beginGroup" declaration
        Pattern p = Pattern.compile(
                "(object\\s+activeGroupClass.*?beginObjectProperties(.*?)beginGroup)",
                Pattern.DOTALL);
        Matcher m = p.matcher(groupData);
        m.find();
        int start = m.start();
        int end = m.end();
        String matchData = m.group(2);

        // append afterData
        matchData = matchData + afterData;

        Pattern p1 = Pattern.compile(".*");
        Matcher m1 = p1.matcher(matchData);

        while (m1.find())
            parseProperty(m1, group);

        groupData.delete(start, end);
        return groupData;
    }

    /**
     * Parses the group data enclosed with regexp:
     * "(object activeGroupClass(.*?)endGroup.*?endObjectProperties)",
     *
     * Inside this, objects as well as possible other groups are parsed
     * and added into hierarchical data tree.
     *
     * @param parent Parent EdmEntity, which will contain all objects.
     * @param data Group data to parse.
     * @return Remaining data without already parsed data.
     * @throws EdmException if error occurs. Parsing is continued only when
     *         objects contain invalid data. In this case, they are ignored.
     */
    private StringBuilder parseGroup(EdmEntity parent, StringBuilder data) throws EdmException {

        // get boundaries of group data with nesting algorithm
        int[] boundaries = getGroupData(data);
        if (boundaries == null)
            return data;

        StringBuilder groupData = new StringBuilder();
        groupData.append(data.substring(boundaries[0], boundaries[2]));
        data.delete(boundaries[0], boundaries[2]);

        log.config("******** Parsing group.");

        EdmEntity groupEntity = new EdmEntity("activeGroupClass");
        parent.addSubEntity(groupEntity);

        groupData = parseGroupHeader(groupEntity, groupData, boundaries[1] - boundaries[0]);

        boolean isMatch = true;
        while (isMatch) {
            Pattern p = Pattern.compile(
                    "(object(.*?)beginObjectProperties(.*?)endObjectProperties)",
                    Pattern.DOTALL);
            Matcher m = p.matcher(groupData);

            isMatch = m.find();

            if (isMatch) {
                String objType = m.group(2).trim();
                String objData = m.group(3);

                if (objType.equals("activeGroupClass")) {
                    groupData = parseGroup(groupEntity, groupData);
                }
                else {
                    int start = m.start();
                    int end = m.end();
                    parseObject(groupEntity, objType, objData);

                    groupData.delete(start, end);
                }
            }
        }

        return data;
    }

    /**
     * Parses object data. If edm2xml.robustParsing system property is set to "true",
     * parsing is not interrupted in case of erroneous object data. In such case, object is
     * skipped at robust parsing.
     *
     * @param parent Parent EdmEntity, which will contain parsed object.
     * @param objType Object type string.
     * @param objData Object data string, containing all properties.
     * @return True if object was correctly parsed.
     * @throws EdmException in case of error. When parsing is robust, it skips the object,
     *         otherwise parsing is terminated.
     */
    private boolean parseObject(EdmEntity parent, String objType, String objData) throws EdmException {

        boolean error = false;

        log.config("");
        log.config("******** Parsing object: " + objType);

        EdmEntity object = new EdmEntity(objType);

        Pattern p1 = Pattern.compile(".*");
        Matcher m1 = p1.matcher(objData);

        if (robust) {
            while (m1.find()) {
                try {
                    parseProperty(m1, object);
                }
                catch (Exception e) {
                    log.severe("Object parsing skipped due to parsing error.");
                    error = true;
                    break;
                }
            }

            if (!error)
                parent.addSubEntity(object);
        }
        else {
            while (m1.find()) {
                parseProperty(m1, object);
            }

            parent.addSubEntity(object);
        }

        return !error;
    }

    /**
     * Constructor. Reads the file and parses its data.
     * First, display file header is parsed. Then all group hierarchy is parsed,
     * together with objects.
     *
     * @param fileName EDL file to parse.
     * @param stream Stream for that file
     * @throws EdmException if a parsing error occurs.
     */
    public EdmDisplayParser(final String fileName, final InputStream stream) throws Exception {
        super(fileName, stream);

        edmData = parseDisplayProperties(edmData);

        boolean isMatch = true;
        while (isMatch) {
            Pattern p = Pattern.compile(
                    "(object(.*?)beginObjectProperties(.*?)endObjectProperties)",
                    Pattern.DOTALL);
            Matcher m = p.matcher(edmData);

            isMatch = m.find();

            if (isMatch) {
                String objType = m.group(2).trim();
                String objData = m.group(3);

                if (objType.equals("activeGroupClass")) {
                    edmData = parseGroup(getRoot(), edmData);
                }
                else {
                    int start = m.start();
                    int end = m.end();
                    if(objType.contains(":")){
                        objType=objType.replace(":", "_");
                    }
                    parseObject(getRoot(), objType, objData);

                    edmData.delete(start, end);
                }
            }
        }

        // EDM also adds a version number.  If we have extra data other than the
        // version number, log a warning.
        String remainingEdmData = edmData.toString().trim();
        if (remainingEdmData.length() > 0) {
            String versionRegex = "\\d \\d \\d";
            Pattern versionPattern = Pattern.compile(versionRegex);
            Matcher versionMatcher = versionPattern.matcher(remainingEdmData);
            if (!versionMatcher.matches()) {
                log.warning("Remaining data not parsed: " + remainingEdmData);
            } else {
                log.fine("Parsed EDM version number: " + remainingEdmData);
            }
        }
    }
}
