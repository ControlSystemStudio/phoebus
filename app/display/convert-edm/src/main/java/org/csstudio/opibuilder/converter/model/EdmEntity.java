/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic data container for Edm widget or group.
 * Base class for all specific Edm widget classes.
 *
 * @author Matevz
 *
 */
public class EdmEntity extends Object {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.model.EdmEntity");

    private Map<String, EdmAttribute>    attributeMap;
    private Vector<EdmEntity>            subEntities;
    private String                        type;

    /**
     * Constructs an empty EdmEntity of type specified.
     * @param type EdmEntity type.
     */
    public EdmEntity(String type) {
        attributeMap = new HashMap<>();
        subEntities = new Vector<>();
        this.type = type;
    }

    /**
     * Constructs an instance of EdmEntity from data of another EdmEntity instance.
     * If given EdmEntity is an extended Edm class, its annotated properties will
     * be parsed and replaced with specific extensions of EdmAttribute and EdmWidget
     * instances.
     *
     * @param genericEntity EdmEntity to copy.
     * @throws EdmException if there is a parsing error.
     */
    public EdmEntity(EdmEntity genericEntity) throws EdmException {

        // Multiple specializations test.
        if (!genericEntity.getClass().equals(EdmEntity.class)) {
            throw new EdmException(EdmException.SPECIFIC_PARSING_ERROR,
            "Trying to initialize from an already specialized entity.", null);
        }

        attributeMap = new HashMap<>();
        subEntities = new Vector<>();
        this.type = genericEntity.type;

        for (String key : genericEntity.getAttributeIdSet())
            attributeMap.put(key, genericEntity.getAttribute(key));

        for (int i = 0; i < genericEntity.getSubEntityCount(); i++)
            subEntities.add(new EdmEntity(genericEntity.getSubEntity(i)));

        String className = this.getClass().getName();
        Logger log = Logger.getLogger(className);
        log.config("Parsing specific " + className + ".");

        try {
            Class<?> entitySubClass = this.getClass();

            while (entitySubClass != null) {

                for (Field f : entitySubClass.getDeclaredFields()) {
                    if (f.isAnnotationPresent(EdmAttributeAn.class)) {

                        // new, specific attribute id(name) & instance
                        String name = f.getName();
                        EdmAttribute a = null;
                        boolean isEdmAttribute = true;    // if attribute has been initialized

                        boolean required = true;
                        if (f.isAnnotationPresent(EdmOptionalAn.class)) {
                            log.config("Parsing optional property: " + name);
                            required = false;
                        }
                        else {
                            log.config("Parsing required property: " + name);
                        }

                        f.setAccessible(true);

                        // Initialize primitive fields using corresponding EdmAtrtibute classes.
                        if (f.getType().equals(int.class)) {
                            EdmInt i = new EdmInt(getAttribute(name), required);
                            setAttribute(name, i);
                            f.set(this, i.get());
                            isEdmAttribute = false;
                        }

                        else if (f.getType().equals(boolean.class)) {
                            EdmBoolean b = new EdmBoolean(getAttribute(name), required);
                            setAttribute(name, b);
                            f.set(this, b.is());
                            isEdmAttribute = false;
                        }

                        else if (f.getType().equals(double.class)) {
                            EdmDouble d = new EdmDouble(getAttribute(name), required);
                            setAttribute(name, d);
                            f.set(this, d.get());
                            isEdmAttribute = false;
                        }

                        else if (f.getType().equals(String.class)) {
                            EdmString s = new EdmString(getAttribute(name), required);
                            setAttribute(name, s);
                            f.set(this, s.get());
                            isEdmAttribute = false;
                        }



                        // Specialize sub-entities recursively.
                        else if (f.getType().equals(Vector.class)) {
                            f.set(this, parseWidgets());
                            isEdmAttribute = false;
                        }

                        // Initialize non-primitive fields - EdmAttribute subclasses.
                        else if (EdmAttribute.class.isAssignableFrom(f.getType())){

                            Object attribute = f.getType().getConstructor(EdmAttribute.class, boolean.class)
                            .newInstance(getAttribute(name), required);

                            if (attribute instanceof EdmAttribute) {
                                a = (EdmAttribute)attribute;
                            } else {
                                isEdmAttribute = false;
                            }

                        } else {
                            isEdmAttribute = false;
                            log.warning("Property type not mapped!");
                        }


                        if (isEdmAttribute) {
                            setAttribute(name, a);
                            f.set(this, a);
                        }

                        f.setAccessible(false);
                    }
                }

                entitySubClass = entitySubClass.getSuperclass();
            }
        } catch (Exception e) {
            if (e instanceof EdmException)
                throw (EdmException)e;
            else {
                throw new EdmException(EdmException.SPECIFIC_PARSING_ERROR,
                "Error when parsing annotated field.",
                e instanceof InvocationTargetException? e.getCause():e);
            }
        }
    }

    /**
     * Returns the number of attributes in entity.
     * @return    The number of attributes in entity.
     */
    public int getAttributeCount() {
        return attributeMap.size();
    }

    /**
     * Adds the attribute with specified id to entity.
     * Id must be unique.
     * @param id    Unique id of attribute.
     * @param a        Attribute to add.
     * @throws EdmException
     */
    public void addAttribute(String id, EdmAttribute a) throws EdmException {
        // Warn, then replace with new value
        if (attributeMap.containsKey(id))
            log.log(Level.WARNING, "EDM trribute set more than once",
                    new EdmException(EdmException.ATTRIBUTE_ALREADY_EXISTS,
                            "Attribute " + id + " already exists.", null));
        attributeMap.put(id, a);
    }

    /**
     * Returns the attribute with specified id.
     * @param id    The id of attribute that we want to get.
     * @return        Attribute with desired id.
     */
    public EdmAttribute getAttribute(String id) {
        return attributeMap.get(id);
    }

    /**
     * Returns all names of EdmAttributes in current EdmEntity.
     * @return Names of all EdmAttributes (keys in HashMap).
     */
    public Set<String> getAttributeIdSet() {
        return attributeMap.keySet();
    }

    /**
     * Returns the number of subentities in an entity.
     * @return    The number of subentities.
     */
    public int getSubEntityCount() {
        return subEntities.size();
    }

    /**
     * Adds subentity to an entity.
     * @param subE    Subentity to add.
     */
    public void addSubEntity(EdmEntity subE) {
        subEntities.add(subE);
    }

    /**
     * Returns the subentity at specified index.
     * @param i    Index of desired subentity.
     * @return    Subentity.
     */
    public EdmEntity getSubEntity(int i) {
        return subEntities.get(i);
    }

    /**
     * Changes the attribute with specified id.
     * @param id        Id of the attribute that we want to change.
     * @param newAtt    New attribute.
     */
    protected void setAttribute(String id, EdmAttribute newAtt) {
        attributeMap.put(id, newAtt);
    }

    /**
     * Changes the subentity at desired index.
     * @param i        Index of the subentity to change.
     * @param subE    New subentity.
     */
    public void setSubEntity(int i, EdmEntity subE) {
        subEntities.set(i, subE);
    }

    /**
     * Returns EdmEntity type.
     * @return EdmEntity type.
     */
    public String getType() {
        return type;
    }

    /**
     * Parses specific widget data from its parent EdmEntity.
     */
    private Vector<EdmEntity> parseWidgets()  {

//        boolean robust = Boolean.parseBoolean(System.getProperty("edm2xml.robustParsing"));

        Vector<EdmEntity> w = new Vector<>();

        log.config("Parsing specific widgets.");

        String packageName = EdmWidget.class.getPackage().getName();

        for (int i = 0; i < subEntities.size(); i++) {
            EdmEntity subE = subEntities.get(i);
            String wType = "Edm_" + subE.getType();

            log.config("Parsing specific widget: " + wType);
            wType = wType.replace(":", "_");
            Object o;
            try {
                o = Class.forName(packageName + "." + wType).
                getConstructor(EdmEntity.class).newInstance(subE);
                if (o instanceof EdmEntity) {
                    subEntities.set(i, (EdmEntity)o);
                    w.add((EdmEntity)o);
                } else {
                    log.warning("Class not declared: " + wType);
                }
            } catch (Exception e) {
                if(e instanceof ClassNotFoundException){
                    log.warning(wType + " is not convertible.");
                } else {
                    log.log(Level.WARNING, "Parse widget error. ",
                            e instanceof InvocationTargetException ? e.getCause() : e);
                }
            }
        }
        return w;
    }
}
