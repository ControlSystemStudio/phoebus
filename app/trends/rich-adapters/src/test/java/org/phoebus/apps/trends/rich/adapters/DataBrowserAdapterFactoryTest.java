/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.apps.trends.rich.adapters;

import javafx.scene.image.Image;
import org.csstudio.trends.databrowser3.ui.selection.DatabrowserSelection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.logbook.LogEntry;
import org.phoebus.util.time.TimeRelativeInterval;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class DataBrowserAdapterFactoryTest {

    @Test
    public void testGetAdaptableObject(){
        assertEquals(DatabrowserSelection.class, new DatabrowserAdapterFactory().getAdaptableObject());
    }

    @Test
    public void testGetAdapterList(){
        List<? extends  Class> list = new DatabrowserAdapterFactory().getAdapterList();
        assertEquals(2, list.size());
        assertTrue(list.contains(LogEntry.class));
        assertTrue(list.contains(EmailEntry.class));
    }

    @Test
    public void testAdaptLogEntry(){
        DatabrowserAdapterFactory factory = new DatabrowserAdapterFactory();
        DatabrowserSelection databrowserSelection = Mockito.mock(DatabrowserSelection.class);

        when(databrowserSelection.getPlotTitle()).thenReturn(Optional.of("Plot Title"));
        when(databrowserSelection.getPlotPVs()).thenReturn(Arrays.asList("PV1", "PV2"));
        when(databrowserSelection.getPlotTime()).thenReturn(TimeRelativeInterval.of(Instant.EPOCH, Duration.ofDays(10)));
        Optional<LogEntry> logEntry = factory.adapt(databrowserSelection, LogEntry.class);
        assertEquals(1, logEntry.get().getAttachments().size());
    }

    @Test
    public void testAdaptEmailEntry(){
        DatabrowserAdapterFactory factory = new DatabrowserAdapterFactory();
        DatabrowserSelection databrowserSelection = Mockito.mock(DatabrowserSelection.class);

        when(databrowserSelection.getPlotTime()).thenReturn(TimeRelativeInterval.of(Instant.EPOCH, Duration.ofDays(10)));
        when(databrowserSelection.getPlot()).thenReturn(Mockito.mock(Image.class));
        Optional<EmailEntry> emailEntry = factory.adapt(databrowserSelection, EmailEntry.class);
        assertEquals(1, emailEntry.get().getImages().size());
    }
}
