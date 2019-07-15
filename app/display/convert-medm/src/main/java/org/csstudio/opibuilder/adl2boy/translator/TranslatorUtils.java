/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/
package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;

/**
 * Utilities to aid in translating ADL files to OPI files.
 *
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public class TranslatorUtils
{
    static ADLBasicAttribute defaultBasicAttribute = new ADLBasicAttribute();
    static ADLDynamicAttribute defaultDynamicAttribute = new ADLDynamicAttribute();

    public static void setDefaultBasicAttribute(ADLWidget child) throws WrongADLFormatException
    {
        if (child.getType().equals("attr"))
        {
            child.setType("basic attribute");
            defaultBasicAttribute = new ADLBasicAttribute(child);
        }
    }

    public static void setDefaultDynamicAttribute(ADLWidget child) throws WrongADLFormatException
    {
        if (child.getType().equals("attr"))
        {
            child.setType("dynamic attribute");
            defaultDynamicAttribute = new ADLDynamicAttribute(child);
        }
    }
}
