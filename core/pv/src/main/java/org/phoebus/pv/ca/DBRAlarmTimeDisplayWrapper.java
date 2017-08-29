/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import java.text.NumberFormat;

import org.diirt.util.text.NumberFormats;
import org.diirt.vtype.Display;

import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.TIME;
/** Wrap DBR as VType
 *
 *  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DBRAlarmTimeDisplayWrapper<T_DBR extends TIME> extends DBRAlarmTimeWrapper<T_DBR> implements Display
{
    final private GR metadata;

    public DBRAlarmTimeDisplayWrapper(final GR metadata, final T_DBR dbr)
    {
        super(dbr);
        this.metadata = metadata;
    }

    @Override
    public Double getLowerDisplayLimit()
    {
        if (metadata == null)
            return Double.NaN;
        return metadata.getLowerDispLimit().doubleValue();
    }

    @Override
    public Double getLowerCtrlLimit()
    {
        if (metadata instanceof CTRL)
            return ((CTRL)metadata).getLowerCtrlLimit().doubleValue();
        return getLowerDisplayLimit();
    }

    @Override
    public Double getLowerAlarmLimit()
    {
        if (metadata == null)
            return Double.NaN;
        return metadata.getLowerAlarmLimit().doubleValue();
    }

    @Override
    public Double getLowerWarningLimit()
    {
        if (metadata == null)
            return Double.NaN;
        return metadata.getLowerWarningLimit().doubleValue();
    }

    @Override
    public String getUnits()
    {
        if (metadata == null)
            return "?";
        return metadata.getUnits();
    }

    @Override
    public synchronized NumberFormat getFormat()
    {
        if (metadata instanceof PRECISION)
        {
            final int precision = ((PRECISION) metadata).getPrecision();
            if (precision >= 0)
                return NumberFormats.format(precision);
            return NumberFormats.toStringFormat();
        }
        return NumberFormats.format(0);
    }

    @Override
    public Double getUpperWarningLimit()
    {
        if (metadata == null)
            return Double.NaN;
        return metadata.getUpperWarningLimit().doubleValue();
    }

    @Override
    public Double getUpperAlarmLimit()
    {
        if (metadata == null)
            return Double.NaN;
        return metadata.getUpperAlarmLimit().doubleValue();
    }

    @Override
    public Double getUpperCtrlLimit()
    {
        if (metadata instanceof CTRL)
            return ((CTRL)metadata).getUpperCtrlLimit().doubleValue();
        return getUpperDisplayLimit();
    }

    @Override
    public Double getUpperDisplayLimit()
    {
        if (metadata == null)
            return Double.NaN;
        return metadata.getUpperDispLimit().doubleValue();
    }
}
