/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Specific class containing a list of EdmColor instances, parsed from
 * EdmColorsList file.
 *
 * @author Matevz
 *
 */
public class EdmColorsList extends EdmEntity {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.model.EdmColorsList");

    private Map<Integer, EdmColor> colorsMap;
    private Map<String, EdmColor> colorsNameMap;
    private Map<Integer, EdmColor> menuColorsMap;

    /**
     * Constructor which parses EdmColors from general EdmEntity interface.
     * @param genericEntity EdmEntity containing EdmColorsList data.
     * @throws EdmException if an error occurs when parsing EdmColor data.
     */
    public EdmColorsList(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
        populateColorsMaps(genericEntity);
        populateMenuColorsMap(genericEntity);
    }

    /**
     * Method iterates through EdmEntity attributes and specializes the data into
     * EdmColor instances.
     * @param genericColors EdmEntity containing EdmColorsList data.
     * @throws EdmException if any of EdmAttributes contain invalid EdmColor data.
     */
    private void populateColorsMaps(EdmEntity genericColors) throws EdmException {
        colorsMap = new HashMap<>();
        //always has 0 as the undefined color
        colorsMap.put(0, new EdmColor(null, 0,0,0));
        colorsNameMap = new HashMap<>();

        // parse subentities of generic ColorsList EdmEntity;
        // each subentity should be generic EdmColor
        for (String id : genericColors.getAttributeIdSet()) {
            int index = Integer.parseInt(id);
            EdmColor color = new EdmColor(genericColors.getAttribute(id), true);
            colorsMap.put(index, color);
            colorsNameMap.put(color.getName(), color);
        }
    }

    /**
     * Method iterates through EdmEntity sub-entities and populates the menu map.
     */
    private void populateMenuColorsMap(EdmEntity colorsData) throws EdmException {

        menuColorsMap = new HashMap<>();

        // Create a set of all defined colors to check if they are all in the menumap.
        Set<EdmColor> nonMenuColors = new HashSet<>(colorsMap.values());

        int menuInd = 0;
        for (int entityInd = 0; entityInd < colorsData.getSubEntityCount(); entityInd++) {
            EdmEntity entity = colorsData.getSubEntity(entityInd);
            String colorName = entity.getType();
            EdmColor color = colorsNameMap.get(colorName);

            if (color != null) {
                menuColorsMap.put(menuInd, color);
                nonMenuColors.remove(color);
                menuInd++;
            } else  {
                log.warning("Menumap contains an undefined color: " + colorName);
            }
        }

        if (!nonMenuColors.isEmpty()) {
            log.fine("Color definitions exist that are not in menumap. Adding them at the end.");
            Iterator<EdmColor> iterator = nonMenuColors.iterator();
            while (iterator.hasNext()) {
                menuColorsMap.put(menuInd, iterator.next());
                menuInd++;
            }
        }
    }

    public int getCount() {
        return colorsMap.size();
    }

    /**
     * Returns static color with specified index.
     * @param index Index of static color.
     * @return Static color with specified index.
     */
    public EdmColor getColor(int index) {
        return colorsMap.get(index);
    }

    /**
     * Returns the number of colors in the menumap.
     */
    public int getMenuColorCount() {
        return menuColorsMap.size();
    }

    /**
     * Returns the color at the given index in menumap.
     */
    public EdmColor getMenuColor(int index) {
        return menuColorsMap.get(index);
    }

    /**
     * Returns the color with the given name.
     */
    public EdmColor getColor(String name) {
        return colorsNameMap.get(name);
    }

    /**
     * Maps static color with specified index. This method is only for test purposes.
     * @param index Index of static color.
     * @param c Static EdmColor.
     */
    public void addColor(int index, EdmColor c) {
        colorsMap.put(index, c);
    }
}
