/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;

import org.epics.vtype.AlarmSeverity;
import org.phoebus.archive.vtype.VTypeHelper;

/** Samples of a {@link FormulaItem}.

 *  <p>If the last sample is valid, it's
 *  extended to 'now' assuming no new data means
 *  that the last value is still valid.
 *
 *  @author Kay Kasemir
 */
public class FormulaSamples extends PlotSampleArray
{
    /** @return Sample count, includes the last sample extended to 'now' */
    @Override
    public int size()
    {
        final int raw = super.size();
        if (raw <= 0)
            return raw;
        final PlotSample last = get(raw-1);
        if (org.phoebus.core.vtypes.VTypeHelper.getSeverity(last.getVType()) == AlarmSeverity.UNDEFINED)
            return raw;
        // Last sample is valid, so it should still apply 'now'
        return raw+1;
    }

    /** @param index 0... size()-1
     *  @return Sample from historic or live sample subsection
     */
    @Override
    public PlotSample get(final int index)
    {
        final int raw_count = super.size();
        if (index < raw_count)
            return super.get(index);
        // Last sample is valid, so it should still apply 'now'
        final PlotSample sample = super.get(raw_count-1);
        if (Instant.now().compareTo(sample.getPosition()) < 0)
            return sample;
        else
            return new PlotSample(sample.getSource(), VTypeHelper.transformTimestampToNow(sample.getVType()));
    }
}
