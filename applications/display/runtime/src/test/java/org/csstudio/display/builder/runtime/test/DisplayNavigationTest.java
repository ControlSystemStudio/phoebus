/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayNavigation;
import org.csstudio.display.builder.runtime.app.DisplayNavigation.Listener;
import org.junit.Test;
import org.phoebus.framework.macros.Macros;

/** JUnit test for {@link DisplayNavigation}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayNavigationTest
{
    private final AtomicInteger changes = new AtomicInteger(0);

    @Test
    public void testNavigation()
    {
        final DisplayNavigation navigation = new DisplayNavigation();
        final Listener listener = nav -> changes.incrementAndGet();
        navigation.addListener(listener);

        // No history
        assertThat(navigation.getBackwardDisplays().size(), equalTo(0));
        assertThat(navigation.getForwardDisplays().size(), equalTo(0));
        assertThat(changes.get(), equalTo(0));

        // Current display, still no history
        DisplayInfo display = new DisplayInfo("/path/a.opi", "A", new Macros(), false);
        navigation.setCurrentDisplay(display);
        System.out.println(navigation);
        assertThat(navigation.getBackwardDisplays().size(), equalTo(0));
        assertThat(navigation.getForwardDisplays().size(), equalTo(0));
        assertThat(changes.get(), equalTo(0));

        // Open new displays: Now 5 items available to go back
        for (int i=1; i<=5; ++i)
            navigation.setCurrentDisplay(new DisplayInfo("/path/N" + i + ".opi", "N" + i, new Macros(), false));
        System.out.println(navigation);
        assertThat(navigation.getBackwardDisplays().size(), equalTo(5));
        assertThat(navigation.getForwardDisplays().size(), equalTo(0));
        assertThat(changes.get(), equalTo(5));

        display = navigation.goBackward(2);
        System.out.println(navigation);
        assertThat(display.getName(), equalTo("N3"));
        assertThat(navigation.getBackwardDisplays().size(), equalTo(3));
        assertThat(navigation.getForwardDisplays().size(), equalTo(2));
        assertThat(changes.get(), equalTo(6));

        // Informing navigation about the display that's already current
        // (equal, not necessarily identical) has no impact on history
        navigation.setCurrentDisplay(new DisplayInfo(display.getPath(), display.getName(), new Macros(), false));
        assertThat(navigation.getBackwardDisplays().size(), equalTo(3));
        assertThat(navigation.getForwardDisplays().size(), equalTo(2));
        assertThat(changes.get(), equalTo(6));

        // Navigate back to the first display
        display = navigation.goBackward(3);
        System.out.println(navigation);
        assertThat(display.getName(), equalTo("A"));
        assertThat(navigation.getBackwardDisplays().size(), equalTo(0));
        assertThat(navigation.getForwardDisplays().size(), equalTo(5));
        assertThat(changes.get(), equalTo(7));

        // Navigate forward to "N1"
        display = navigation.goForward(1);
        System.out.println(navigation);
        assertThat(display.getName(), equalTo("N1"));
        assertThat(navigation.getBackwardDisplays().size(), equalTo(1));
        assertThat(navigation.getForwardDisplays().size(), equalTo(4));
        assertThat(changes.get(), equalTo(8));

        // Clear forward chain by opening different display
        display = new DisplayInfo("/path/x.opi", "X", new Macros(), false);
        navigation.setCurrentDisplay(display);
        System.out.println(navigation);
        assertThat(navigation.getBackwardDisplays().size(), equalTo(2));
        assertThat(navigation.getForwardDisplays().size(), equalTo(0));
        assertThat(changes.get(), equalTo(9));

        navigation.removeListener(listener);
    }
}
