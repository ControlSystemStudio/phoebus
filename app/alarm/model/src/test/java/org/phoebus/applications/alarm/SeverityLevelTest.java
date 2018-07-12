/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** JUnit Plug-in test of alarm severities
 *  @author Kay Kasemir
 */
public class SeverityLevelTest
{
    @Test
    public void testOrdering()
    {
        final SeverityLevel minor = SeverityLevel.MINOR;
        final SeverityLevel major = SeverityLevel.MAJOR;
        final SeverityLevel major_ack = SeverityLevel.MAJOR_ACK;

        // major more severe than minor:
        assertTrue(major.ordinal() > minor.ordinal());

        // major more severe than ack'ed major:
        assertTrue(major.ordinal() > major_ack.ordinal());

        // .. but when updating severities, an ack'ed major
        // means further major and minor alarms of the same PV don't matter
        assertTrue(major_ack.getAlarmUpdatePriority() > major.getAlarmUpdatePriority());
        assertTrue(major_ack.getAlarmUpdatePriority() > minor.getAlarmUpdatePriority());

        // Only an invalid alarm would be higher
        assertTrue(SeverityLevel.INVALID.getAlarmUpdatePriority() > major_ack.getAlarmUpdatePriority());
        assertTrue(SeverityLevel.INVALID.getAlarmUpdatePriority() > major.getAlarmUpdatePriority());
    }
}
