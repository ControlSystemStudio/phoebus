/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.table.AlarmTableModel;

/** JUnit test of the {@link AlarmTableModel}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableModelTest
{
    @Test
    public void testAlarmTableModel() throws Exception
    {
        final AlarmTableModel model = new AlarmTableModel();

        // Initially, no alarms
        assertThat(model.getActiveAlarms().size(), equalTo(0));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(0));

        final AlarmClientNode root = new AlarmClientNode(null, "Test");
        final AlarmClientNode area = new AlarmClientNode(root, "Section 1");
        final AlarmClientLeaf pv1 = new AlarmClientLeaf(area, "pv1");
        final AlarmClientLeaf pv2 = new AlarmClientLeaf(area, "pv2");

        // Receiving updates that don't change the alarm table
        assertThat(model.handleUpdate(root), equalTo(false));
        assertThat(model.handleUpdate(area), equalTo(false));
        assertThat(model.getActiveAlarms().size(), equalTo(0));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(0));

        assertThat(model.handleUpdate(pv1), equalTo(false));
        assertThat(model.handleUpdate(pv2), equalTo(false));
        assertThat(model.getActiveAlarms().size(), equalTo(0));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(0));

        // PV into alarm
        pv1.setState(new ClientState(SeverityLevel.MAJOR, "High", "10.0", Instant.now(), SeverityLevel.MAJOR, "High"));
        assertThat(model.handleUpdate(pv1), equalTo(true));
        // Same value again is no update
        assertThat(model.handleUpdate(pv1), equalTo(false));
        assertThat(model.getActiveAlarms().size(), equalTo(1));
        assertThat(model.getActiveAlarms(), hasItems(pv1));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(0));

        // Another PV into alarm
        pv2.setState(new ClientState(SeverityLevel.MINOR, "Low", "8.0", Instant.now(), SeverityLevel.MINOR, "Low"));
        assertThat(model.handleUpdate(pv2), equalTo(true));
        assertThat(model.getActiveAlarms().size(), equalTo(2));
        assertThat(model.getActiveAlarms(), hasItems(pv1, pv2));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(0));

        // One PV alarm acknowledged
        pv1.setState(new ClientState(SeverityLevel.MAJOR_ACK, "High", "10.0", Instant.now(), SeverityLevel.MAJOR, "High"));
        assertThat(model.handleUpdate(pv1), equalTo(true));
        assertThat(model.getActiveAlarms().size(), equalTo(1));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(1));

        // All clear
        pv1.setState(new ClientState(SeverityLevel.OK, "ok", "5.0", Instant.now(), SeverityLevel.OK, ""));
        pv2.setState(new ClientState(SeverityLevel.OK, "ok", "5.0", Instant.now(), SeverityLevel.OK, ""));
        assertThat(model.handleUpdate(pv1), equalTo(true));
        assertThat(model.handleUpdate(pv2), equalTo(true));
        assertThat(model.getActiveAlarms().size(), equalTo(0));
        assertThat(model.getAcknowledgedAlarms().size(), equalTo(0));
    }
}
