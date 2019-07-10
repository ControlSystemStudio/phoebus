/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/
package org.csstudio.opibuilder.adl2boy.translator;

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
    static final ADLBasicAttribute defaultBasicAttribute = new ADLBasicAttribute();
    static final  ADLDynamicAttribute defaultDynamicAttribute = new ADLDynamicAttribute();
}
