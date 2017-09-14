/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import static org.phoebus.applications.pvtree.PVTreeApplication.logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.prefs.Preferences;
/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    private static final String UPDATE_PERIOD = "update_period";
    private static final String FIELDS = "fields";
    private static final String READ_LONG_FIELDS = "read_long_fields";


    /** Read links as "long strings"?
     *
     *  <p>The channel access DBR_STRING has a length limit of 40 chars.
     *  Since EPICS base R3.14.11, reading fields with an added '$' returns
     *  their value as a char[] without length limitation.
     *  For older IOCs, this will however fail, so set this option
     *  only if all IOCs are at least version R3.14.11
     */
    public static boolean readLongFields()
    {
        final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        boolean value = prefs.getBoolean(READ_LONG_FIELDS, true);
        prefs.putBoolean(READ_LONG_FIELDS, value);
        return value;
    }

    /** For each record type, list the fields to read and trace as 'links'.
     *  Format: record_type (field1, field2) ; record_type (...)
     *
     *  Fields can simply be listed as 'INP', 'DOL'.
     *  The syntax INPA-L is a shortcut for INPA, INPB, INPC, ..., INPL
     *  The syntax INP001-128 is a shortcut for INP001, INP002, ..., INP128
     *  The general syntax is "FIELDxxx-yyy",
     *  where "xxx" and "yyy" are the initial and final value.
     *  "xxx" and "yyy" need to be of the same length, i.e. "1-9" or "01-42", NOT "1-42".
     *  For characters, only single-char "A-Z" is supported, NOT "AA-ZZ",
     *  where it's also unclear if that should turn into AA, AB, AC, .., AZ, BA, BB, BC, .., ZZ
     *  or AA, BB, .., ZZ
     *
     *  bigASub is a CSIRO/ASCAP record type, doesn't hurt to add that to the shared configuration
     *
     *  scalcout is a bit unfortunate since there is no shortcut for INAA-INLL.
     */
    public static Map<String, List<String>> getFieldInfo()
    {
        final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        String spec = "aai(INP);ai(INP);bi(INP);compress(INP);longin(INP);mbbi(INP);mbbiDirect(INP);mbboDirect(INP);stringin(INP);subArray(INP);waveform(INP);aao(DOL);ao(DOL);bo(DOL);fanout(DOL);longout(DOL);mbbo(DOL);stringout(DOL);sub(INPA-L);genSub(INPA-L);calc(INPA-L);calcout(INPA-L);aSub(INPA-U);seq(SELN);bigASub(INP001-128);scalcout(INPA-L,INAA,INBB,INCC,INDD,INEE,INFF,INGG,INHH,INII,INJJ,INKK,INLL)";
        spec = prefs.get(FIELDS, spec);
        prefs.put(FIELDS, spec);
        try
        {
            return FieldParser.parse(spec);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot parse fields from '" + spec + "'", ex);
        }
        return Collections.emptyMap();
    }

    /** @return Max update period in seconds */
    public static double getUpdatePeriod()
    {
        final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        double period = prefs.getDouble(UPDATE_PERIOD, 0.2);
        prefs.putDouble(UPDATE_PERIOD, period);
        return period;
    }
}
